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
            val title = work.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val doi = normalizePaperDoi(work.doi)
            val openAlexUrl = work.id?.takeIf { Regex("https://openalex\\.org/W[0-9]+").matches(it) }
            if (doi == null && openAlexUrl == null) return@mapNotNull null
            val paper = Paper(title=title, authors=work.authorships.orEmpty().take(100).mapNotNull {
                it.author?.displayName?.trim()?.takeIf(String::isNotEmpty)
            }.joinToString(", "), year=work.publicationYear, doi=doi,
                url=doi?.let { "https://doi.org/$it" } ?: openAlexUrl)
            PaperCandidate(paper, id, AbstractReconstruction.segments(work.abstractInvertedIndex))
        }
    }
}

object OpenAlexClient {
    val api: OpenAlexApi by lazy {
        Retrofit.Builder().baseUrl("https://api.openalex.org/")
            .client(ScholarlyHttpClient.client)
            .addConverterFactory(GsonConverterFactory.create()).build().create(OpenAlexApi::class.java)
    }
}
