package com.example.papereyes.domain

import android.util.Xml
import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.ScholarlyIdentifiers
import com.example.papereyes.data.model.normalizePaperDoi
import com.example.papereyes.data.remote.ArxivApi
import com.example.papereyes.data.remote.ArxivClient
import com.example.papereyes.data.remote.InspireRepository
import com.example.papereyes.data.remote.OpenAlexProvider
import com.example.papereyes.data.remote.PaperRepository
import com.example.papereyes.data.remote.ScholarlyProvider
import com.example.papereyes.data.remote.ScholarlyRequestPolicy
import com.example.papereyes.data.remote.SemanticScholarApi
import com.example.papereyes.data.remote.SemanticScholarClient
import com.example.papereyes.domain.citation.JournalCitationParser
import com.example.papereyes.domain.discovery.InteriorPageDiscovery
import com.example.papereyes.domain.discovery.RetrievalEvidence
import com.example.papereyes.domain.evidence.DocumentEvidence
import com.example.papereyes.util.concurrency.SuspendQueryCache
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import retrofit2.HttpException
import java.util.Locale

enum class PaperInputType { DOI, ARXIV, JOURNAL_CITATION, TITLE_OR_OCR, INTERIOR_TEXT }
enum class ResolutionStatus { VERIFIED, CANDIDATES, UNCERTAIN, NOT_FOUND, PROVIDERS_UNAVAILABLE }
data class PaperResolveResult(
    val inputType: PaperInputType,
    val papers: List<Paper>,
    val status: ResolutionStatus = if (papers.isEmpty()) ResolutionStatus.NOT_FOUND else ResolutionStatus.CANDIDATES,
    val unavailableProviders: Set<String> = emptySet()
)
class PaperResolutionException(message: String, cause: Throwable? = null) : Exception(message, cause)
private class PaperNotFoundException(message: String) : Exception(message)

/** Exact/journal/title routes stay separate from experimental interior retrieval. */
class PaperResolver(
    private val crossrefRepository: PaperRepository = PaperRepository(),
    private val inspireRepository: InspireRepository = InspireRepository(),
    private val semanticScholarApi: SemanticScholarApi = SemanticScholarClient.api,
    private val arxivApi: ArxivApi = ArxivClient.api,
    private val interiorDiscovery: InteriorPageDiscovery = InteriorPageDiscovery(listOf(OpenAlexProvider())),
    private val requestPolicy: ScholarlyRequestPolicy = ScholarlyRequestPolicy.shared
) {
    private val cache = SuspendQueryCache<String, PaperResolveResult>(ttlMillis = {
        when {
            it.unavailableProviders.isNotEmpty() -> 0L
            it.papers.isEmpty() -> 5_000L
            else -> 30_000L
        }
    })

    suspend fun resolve(rawInput: String): PaperResolveResult {
        val input = rawInput.trim().take(500)
        if (input.isEmpty()) return PaperResolveResult(PaperInputType.TITLE_OR_OCR, emptyList())
        val key = canonicalQuery(input)
        return cache.getOrLoad(key) { resolveUncached(input) }
    }

    suspend fun resolveEvidence(evidence: DocumentEvidence): PaperResolveResult {
        if (evidence.bestQuery.isNotBlank()) return resolve(evidence.bestQuery)
        if (evidence.fingerprints.isEmpty()) return PaperResolveResult(
            PaperInputType.TITLE_OR_OCR, emptyList(), ResolutionStatus.UNCERTAIN)
        // Do not erase source groups: two windows from one sentence are not independent.
        val key = "interior:" + evidence.fingerprints.take(5).joinToString("|") {
            "${it.sourceGroup}:${it.text.lowercase(Locale.ROOT)}"
        }
        return cache.getOrLoad(key) {
            val found = withContext(Dispatchers.Default) {
                interiorDiscovery.discover(RetrievalEvidence(evidence.fingerprints))
            }
            PaperResolveResult(PaperInputType.INTERIOR_TEXT,
                found.verifiedPaper?.let(::listOf) ?: found.candidates.map { it.paper },
                when {
                    found.verifiedPaper != null -> ResolutionStatus.VERIFIED
                    found.unavailableProviders.isNotEmpty() && found.candidates.isEmpty() -> ResolutionStatus.PROVIDERS_UNAVAILABLE
                    else -> ResolutionStatus.UNCERTAIN
                }, found.unavailableProviders)
        }
    }

    private suspend fun resolveUncached(input: String): PaperResolveResult {
        ScholarlyIdentifiers.extractArxivDoiId(input)?.let { return resolveArxivInput(it) }
        ScholarlyIdentifiers.extractDoi(input)?.let { doi ->
            val paper = try { crossrefRepository.getPaperByDoi(doi) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: HttpException) {
                if (error.code() == 404) return PaperResolveResult(PaperInputType.DOI, emptyList())
                throw lookupFailure("Crossref", error)
            } catch (error: Exception) { throw lookupFailure("Crossref", error) }
            return PaperResolveResult(PaperInputType.DOI, listOf(paper), ResolutionStatus.VERIFIED)
        }
        ScholarlyIdentifiers.extractArxivId(input)?.let { return resolveArxivInput(it) }
        val citation = JournalCitationParser.parse(input)
        if (citation != null) {
            var crossrefFailure: Exception? = null
            val crossref = try { crossrefRepository.searchPaperByCitation(citation) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { crossrefFailure = error; emptyList() }
            if (crossref.isNotEmpty()) return citationResult(crossref)
            var inspireFailure: Exception? = null
            val inspire = try { inspireRepository.searchPaperByCitation(citation) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { inspireFailure = error; emptyList() }
            if (inspire.isNotEmpty()) return citationResult(inspire)
            // Never turn an attempted-provider failure into a false not-found.
            if (crossrefFailure != null || inspireFailure != null) {
                throw lookupFailure("Scholarly lookup services", crossrefFailure ?: inspireFailure!!)
            }
            return PaperResolveResult(PaperInputType.JOURNAL_CITATION, emptyList())
        }
        return try { PaperResolveResult(PaperInputType.TITLE_OR_OCR, crossrefRepository.searchPaper(input)) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { throw lookupFailure("Crossref", error) }
    }

    private fun citationResult(papers: List<Paper>) = PaperResolveResult(
        PaperInputType.JOURNAL_CITATION, papers,
        if (papers.size == 1) ResolutionStatus.VERIFIED else ResolutionStatus.CANDIDATES)

    private suspend fun resolveArxivInput(id: String): PaperResolveResult {
        val paper = try { resolveArxivPaper(id) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: PaperNotFoundException) { return PaperResolveResult(PaperInputType.ARXIV, emptyList()) }
        catch (error: Exception) { throw lookupFailure("arXiv metadata services", error) }
        return PaperResolveResult(PaperInputType.ARXIV, listOf(paper), ResolutionStatus.VERIFIED)
    }

    private suspend fun resolveArxivPaper(id: String): Paper {
        try {
            val workId = ScholarlyIdentifiers.arxivWorkId(id) ?: throw IllegalArgumentException("Invalid arXiv ID")
            val response = requestPolicy.request(ScholarlyProvider.SEMANTIC_SCHOLAR) {
                semanticScholarApi.getPaper("ARXIV:$workId")
            }
            val title = response.title?.trim()?.takeIf { it.isNotBlank() }
                ?: error("Semantic Scholar did not return a title")
            check(response.externalIds?.arxiv?.let(ScholarlyIdentifiers::arxivWorkId) == workId) {
                "Semantic Scholar returned a different or missing arXiv ID"
            }
            return Paper(title=title, authors=response.authors.orEmpty().mapNotNull {
                it.name?.trim()?.takeIf(String::isNotBlank)
            }.joinToString(", "), year=response.year, doi=normalizePaperDoi(response.externalIds?.doi),
                url="https://arxiv.org/abs/$id")
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { /* Official metadata is the evidence-aware fallback. */ }

        return requestPolicy.request(ScholarlyProvider.ARXIV) {
            arxivApi.queryById(id).use { body ->
                withContext(Dispatchers.IO) {
                    body.charStream().use { reader ->
                        val parser = Xml.newPullParser()
                        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true)
                        parser.setInput(reader)
                        ArxivFeedParser.parse(parser, id)
                            ?: throw PaperNotFoundException("No paper found on arXiv for $id")
                    }
                }
            }
        }
    }

    private fun lookupFailure(source: String, error: Exception) = PaperResolutionException(
        "$source temporarily unavailable or rate-limited. Check your connection and try again later.", error)

    private fun canonicalQuery(input: String): String {
        ScholarlyIdentifiers.extractArxivDoiId(input)?.let { return "arxiv:$it" }
        ScholarlyIdentifiers.extractDoi(input)?.let { return "doi:$it" }
        ScholarlyIdentifiers.extractArxivId(input)?.let { return "arxiv:$it" }
        return "query:" + input.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ")
    }
}
