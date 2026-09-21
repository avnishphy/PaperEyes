package com.example.papereyes.data.remote

import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.normalizePaperDoi
import com.example.papereyes.domain.discovery.AbstractReconstruction
import com.example.papereyes.domain.discovery.PaperCandidate
import com.example.papereyes.domain.discovery.PaperDiscoveryProvider
import com.example.papereyes.domain.discovery.PhraseQuery
import com.example.papereyes.domain.discovery.ProviderCapability
import com.example.papereyes.domain.discovery.RetrievalEvidence
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/** Anonymous basic search. Optional account credit/key support is NOT embedded.
 * A full-text search hit does not expose its matching body text here: only returned
 * abstract text can corroborate phrases. Missing abstract = unverified candidate.
 */
class OpenAlexProvider(
    private val api: OpenAlexApi = OpenAlexClient.api,
    private val requestPolicy: ScholarlyRequestPolicy = ScholarlyRequestPolicy.shared
) : PaperDiscoveryProvider {
    override val id = "OpenAlex"
    override val capabilities = setOf(ProviderCapability.ABSTRACT_SEARCH, ProviderCapability.FULL_TEXT_PHRASE_SEARCH)

    override suspend fun searchEvidence(evidence: RetrievalEvidence): List<PaperCandidate> {
        val query = PhraseQuery.build(evidence.fingerprints)
        if (query.isBlank()) return emptyList()
        val response = requestPolicy.request(ScholarlyProvider.OPENALEX) { api.searchWorks(query) }
        return response.results.orEmpty().take(5).mapNotNull { work ->
            val paper = work.toPaper() ?: return@mapNotNull null
            PaperCandidate(paper, id, AbstractReconstruction.segments(work.abstractInvertedIndex))
        }
    }

    /** Bounded metadata fallback for exact title/OCR queries missing from Crossref. */
    suspend fun searchTitle(query: String): List<Paper> {
        val cleaned = query.trim().take(500)
        if (cleaned.isBlank()) return emptyList()
        val response = requestPolicy.request(ScholarlyProvider.OPENALEX) {
            api.searchWorks(cleaned, perPage = 5)
        }
        return response.results.orEmpty().take(5).mapNotNull { it.toPaper() }
    }

    private fun OpenAlexWork.toPaper(): Paper? {
        val cleanTitle = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val normalizedDoi = normalizePaperDoi(doi)
        val openAlexUrl = id?.takeIf { Regex("https://openalex\\.org/W[0-9]+").matches(it) }
        if (normalizedDoi == null && openAlexUrl == null) return null
        return Paper(
            title = cleanTitle,
            authors = authorships.orEmpty().take(100).mapNotNull {
                it.author?.displayName?.trim()?.takeIf(String::isNotEmpty)
            }.joinToString(", "),
            year = publicationYear,
            doi = normalizedDoi,
            url = normalizedDoi?.let { "https://doi.org/$it" } ?: openAlexUrl
        )
    }
}

object OpenAlexClient {
    val api: OpenAlexApi by lazy {
        Retrofit.Builder().baseUrl("https://api.openalex.org/")
            .client(ScholarlyHttpClient.client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(OpenAlexApi::class.java)
    }
}
