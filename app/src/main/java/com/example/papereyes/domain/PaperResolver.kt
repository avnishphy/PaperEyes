package com.example.papereyes.domain

import android.util.Xml
import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.ScholarlyIdentifiers
import com.example.papereyes.data.remote.ArxivClient
import com.example.papereyes.data.remote.InspireRepository
import com.example.papereyes.data.remote.PaperRepository
import com.example.papereyes.data.remote.SemanticScholarClient
import com.example.papereyes.domain.citation.JournalCitationParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import retrofit2.HttpException


enum class PaperInputType {

    DOI,

    ARXIV,

    JOURNAL_CITATION,

    TITLE_OR_OCR
}


data class PaperResolveResult(
    val inputType: PaperInputType,
    val papers: List<Paper>
)


class PaperResolutionException(
    message: String,
    cause: Throwable? = null
) : Exception(
    message,
    cause
)


private class PaperNotFoundException(
    message: String
) : Exception(
    message
)


class PaperResolver(
    private val crossrefRepository: PaperRepository = PaperRepository(),
    private val inspireRepository: InspireRepository = InspireRepository(),
    private val semanticScholarApi: com.example.papereyes.data.remote.SemanticScholarApi = SemanticScholarClient.api,
    private val arxivApi: com.example.papereyes.data.remote.ArxivApi = ArxivClient.api
) {


    /**
     * Low-latency title lookup for Live Scan. Semantic Scholar exposes a
     * dedicated closest-title endpoint, so a clean OCR title can resolve in a
     * single request instead of entering Crossref's rate-limited list search.
     *
     * This is intentionally a best-effort optimization: any non-cancellation
     * failure returns null so callers can fall back to the normal resolver.
     */
    suspend fun resolveFastTitle(
        rawTitle: String
    ): Paper? {
        val title = rawTitle.trim()
        if (title.isBlank()) return null

        val response =
            try {
                semanticScholarApi.searchPaperMatch(title)
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                return null
            }

        val match = response.data?.firstOrNull() ?: return null
        val matchedTitle = match.title
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        val authors = match.authors
            .orEmpty()
            .mapNotNull { author ->
                author.name?.trim()?.takeIf { it.isNotBlank() }
            }
            .joinToString(", ")

        val arxivId = match.externalIds?.arxiv?.trim()?.takeIf { it.isNotBlank() }

        return Paper(
            title = matchedTitle,
            authors = authors,
            year = match.year,
            doi = match.externalIds?.doi,
            url = arxivId?.let { "https://arxiv.org/abs/$it" } ?: match.url
        )
    }


    suspend fun resolve(
        rawInput: String
    ): PaperResolveResult {

        val input =
            rawInput.trim()


        if (
            input.isBlank()
        ) {

            return PaperResolveResult(
                inputType =
                    PaperInputType.TITLE_OR_OCR,

                papers =
                    emptyList()
            )
        }


        /*
         * ============================================================
         * ARXIV DOI ALIAS
         * ============================================================
         *
         * arXiv exposes DOI-style identifiers such as:
         *
         *     https://doi.org/10.48550/arXiv.2609.20448
         *
         * These must go through the arXiv resolver rather than the
         * generic Crossref DOI resolver.
         *
         * This check MUST happen before generic DOI detection because
         * 10.48550/arXiv.xxxxx is also syntactically a valid DOI.
         */
        val arxivDoiId =
            extractArxivDoi(
                input
            )


        if (
            arxivDoiId != null
        ) {

            return resolveArxivInput(
                arxivDoiId
            )
        }


        /*
         * ============================================================
         * DOI
         * ============================================================
         *
         * DOI remains the strongest general-purpose identifier.
         *
         * arXiv DOI aliases have already been removed from this path
         * above.
         */
        val doi =
            extractDoi(
                input
            )


        if (
            doi != null
        ) {

            val paper =
                try {

                    crossrefRepository
                        .getPaperByDoi(
                            doi
                        )

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: HttpException
                ) {

                    if (
                        exception.code() == 404
                    ) {

                        return PaperResolveResult(
                            inputType =
                                PaperInputType.DOI,

                            papers =
                                emptyList()
                        )
                    }


                    throw lookupFailure(
                        source =
                            "Crossref",

                        cause =
                            exception
                    )

                } catch (
                    exception: Exception
                ) {

                    throw lookupFailure(
                        source =
                            "Crossref",

                        cause =
                            exception
                    )
                }


            return PaperResolveResult(
                inputType =
                    PaperInputType.DOI,

                papers =
                    listOf(
                        paper
                    )
            )
        }


        /*
         * ============================================================
         * ARXIV
         * ============================================================
         */
        val arxivId =
            extractArxivId(
                input
            )


        if (
            arxivId != null
        ) {

            return resolveArxivInput(
                arxivId
            )
        }


        /*
         * ============================================================
         * JOURNAL CITATION
         * ============================================================
         *
         * Examples:
         *
         * Phys.Rev.D 112 (2025) 3, 034009
         *
         * Phys. Rev. D 112, 034009 (2025)
         *
         * JHEP 09 (2025) 123
         *
         * N Engl J Med. 2024;390(3):123-130
         *
         * J. Am. Chem. Soc. 2024, 146, 1234-1245
         *
         * IEEE Trans. Pattern Anal. Mach. Intell.,
         * vol. 45, no. 7, pp. 1234-1245, 2023
         *
         *
         * This comes AFTER DOI/arXiv detection so the citation parser
         * cannot steal those stronger identifiers.
         */
        val journalCitation =
            JournalCitationParser.parse(
                input
            )


        if (
            journalCitation != null
        ) {

            /*
             * Crossref remains the primary general-purpose scholarly
             * metadata source.
             */
            var crossrefFailure: Exception? =
                null


            val crossrefPapers =
                try {

                    crossrefRepository
                        .searchPaperByCitation(
                            journalCitation
                        )

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: Exception
                ) {

                    crossrefFailure =
                        exception


                    emptyList()
                }


            if (
                crossrefPapers.isNotEmpty()
            ) {

                return PaperResolveResult(
                    inputType =
                        PaperInputType.JOURNAL_CITATION,

                    papers =
                        crossrefPapers
                )
            }


            val inspirePapers =
                try {

                    inspireRepository
                        .searchPaperByCitation(
                            journalCitation
                        )

                } catch (
                    exception: CancellationException
                ) {

                    throw exception

                } catch (
                    exception: Exception
                ) {

                    if (
                        crossrefFailure != null
                    ) {

                        throw PaperResolutionException(
                            message =
                                "Scholarly lookup services are temporarily unavailable. Check your connection and try again.",

                            cause =
                                exception
                        )
                    }


                    /*
                     * Crossref completed successfully and found no match. An
                     * INSPIRE outage should not turn that valid no-match into
                     * a network error.
                     */
                    emptyList()
                }


            if (
                inspirePapers.isNotEmpty()
            ) {

                return PaperResolveResult(
                    inputType =
                        PaperInputType.JOURNAL_CITATION,

                    papers =
                        inspirePapers
                )
            }


            if (
                crossrefFailure != null
            ) {

                throw lookupFailure(
                    source =
                        "Crossref",

                    cause =
                        crossrefFailure
                )
            }


            return PaperResolveResult(
                inputType =
                    PaperInputType.JOURNAL_CITATION,

                papers =
                    emptyList()
            )
        }


        /*
         * ============================================================
         * NORMAL TITLE / OCR
         * ============================================================
         */
        val papers =
            try {

                crossrefRepository
                    .searchPaper(
                        input
                    )

            } catch (
                exception: CancellationException
            ) {

                throw exception

            } catch (
                exception: Exception
            ) {

                throw lookupFailure(
                    source =
                        "Crossref",

                    cause =
                        exception
                )
            }


        return PaperResolveResult(
            inputType =
                PaperInputType.TITLE_OR_OCR,

            papers =
                papers
        )
    }


    private suspend fun resolveArxivInput(
        arxivId: String
    ): PaperResolveResult {

        val paper =
            try {

                resolveArxivPaper(
                    arxivId
                )

            } catch (
                exception: CancellationException
            ) {

                throw exception

            } catch (
                exception: PaperNotFoundException
            ) {

                return PaperResolveResult(
                    inputType =
                        PaperInputType.ARXIV,

                    papers =
                        emptyList()
                )

            } catch (
                exception: Exception
            ) {

                throw PaperResolutionException(
                    message =
                        "arXiv metadata services are temporarily unavailable. Check your connection and try again.",

                    cause =
                        exception
                )
            }


        return PaperResolveResult(
            inputType =
                PaperInputType.ARXIV,

            papers =
                listOf(
                    paper
                )
        )
    }


    private fun lookupFailure(
        source: String,
        cause: Exception
    ): PaperResolutionException {

        return PaperResolutionException(
            message =
                "$source is temporarily unavailable. Check your connection and try again.",

            cause =
                cause
        )
    }


    /*
     * ================================================================
     * ARXIV RESOLUTION
     * ================================================================
     */
    private suspend fun resolveArxivPaper(
        arxivId: String
    ): Paper {

        /*
         * First try Semantic Scholar.
         *
         * If it rate-limits us or another API/network error occurs,
         * fall back to the official arXiv API.
         */
        try {

            return resolveArxivWithSemanticScholar(
                arxivId
            )

        } catch (
            exception: CancellationException
        ) {

            throw exception

        } catch (
            exception: HttpException
        ) {

            return resolveArxivWithOfficialApi(
                arxivId
            )

        } catch (
            exception: Exception
        ) {

            return resolveArxivWithOfficialApi(
                arxivId
            )
        }
    }


    /*
     * ================================================================
     * SEMANTIC SCHOLAR
     * ================================================================
     */
    private suspend fun resolveArxivWithSemanticScholar(
        arxivId: String
    ): Paper {

        val response =
            semanticScholarApi.getPaper(
                paperId =
                    "ARXIV:$arxivId"
            )


        val title =
            response.title
                ?.trim()
                ?.takeIf {

                    it.isNotBlank()
                }
                ?: throw Exception(
                    "Semantic Scholar did not return a title."
                )


        val authors =
            response.authors
                ?.mapNotNull {

                    it.name
                        ?.trim()
                        ?.takeIf { name ->

                            name.isNotBlank()
                        }
                }
                ?.joinToString(
                    ", "
                )
                ?: ""


        return Paper(
            title =
                title,

            authors =
                authors,

            year =
                response.year,

            doi =
                response
                    .externalIds
                    ?.doi,

            url =
                "https://arxiv.org/abs/$arxivId"
        )
    }


    /*
     * ================================================================
     * OFFICIAL ARXIV FALLBACK
     * ================================================================
     */
    private suspend fun resolveArxivWithOfficialApi(
        arxivId: String
    ): Paper {

        val responseBody = arxivApi.queryById(arxivId)

        return responseBody.use { body ->
            withContext(Dispatchers.IO) {
                body.charStream().use { reader ->
                    val parser = Xml.newPullParser()
                    parser.setFeature(
                        XmlPullParser.FEATURE_PROCESS_NAMESPACES,
                        true
                    )
                    parser.setInput(reader)
                    parseArxivResponse(
                        parser = parser,
                        arxivId = arxivId
                    )
                }
            }
        }
    }


    /*
     * ================================================================
     * ARXIV XML PARSING
     * ================================================================
     */
    private fun parseArxivResponse(
        parser: XmlPullParser,
        arxivId: String
    ): Paper {

        var title: String? =
            null


        val authors =
            mutableListOf<String>()


        var doi: String? =
            null


        var published: String? =
            null


        var insideEntry =
            false


        var insideAuthor =
            false


        var eventType =
            parser.eventType


        while (
            eventType !=
            XmlPullParser.END_DOCUMENT
        ) {

            when (
                eventType
            ) {

                XmlPullParser.START_TAG -> {

                    when (
                        parser.name
                            .lowercase()
                    ) {

                        "entry" -> {

                            insideEntry =
                                true
                        }


                        "title" -> {

                            if (
                                insideEntry &&
                                title == null
                            ) {

                                title =
                                    parser
                                        .nextText()
                                        .normalizeWhitespace()
                            }
                        }


                        "author" -> {

                            if (
                                insideEntry
                            ) {

                                insideAuthor =
                                    true
                            }
                        }


                        "name" -> {

                            if (
                                insideEntry &&
                                insideAuthor
                            ) {

                                val author =
                                    parser
                                        .nextText()
                                        .normalizeWhitespace()


                                if (
                                    author.isNotBlank()
                                ) {

                                    authors.add(
                                        author
                                    )
                                }
                            }
                        }


                        "published" -> {

                            if (
                                insideEntry
                            ) {

                                published =
                                    parser
                                        .nextText()
                                        .trim()
                            }
                        }


                        "doi" -> {

                            if (
                                insideEntry
                            ) {

                                doi =
                                    parser
                                        .nextText()
                                        .trim()
                                        .takeIf {

                                            it.isNotBlank()
                                        }
                            }
                        }
                    }
                }


                XmlPullParser.END_TAG -> {

                    when (
                        parser.name
                            .lowercase()
                    ) {

                        "author" -> {

                            insideAuthor =
                                false
                        }


                        "entry" -> {

                            insideEntry =
                                false
                        }
                    }
                }
            }


            eventType =
                parser.next()
        }


        val finalTitle =
            title
                ?.takeIf {

                    it.isNotBlank()
                }
                ?: throw PaperNotFoundException(
                    "No paper found on arXiv for $arxivId"
                )


        val year =
            published
                ?.take(
                    4
                )
                ?.toIntOrNull()


        return Paper(
            title =
                finalTitle,

            authors =
                authors.joinToString(
                    ", "
                ),

            year =
                year,

            doi =
                doi,

            url =
                "https://arxiv.org/abs/$arxivId"
        )
    }


    /*
     * ================================================================
     * ARXIV DOI ALIAS DETECTION
     * ================================================================
     *
     * Examples:
     *
     * 10.48550/arXiv.2609.20448
     *
     * https://doi.org/10.48550/arXiv.2609.20448
     *
     * http://doi.org/10.48550/arXiv.2609.20448
     *
     * Versioned forms are also accepted:
     *
     * 10.48550/arXiv.2609.20448v2
     */
    private fun extractArxivDoi(input: String): String? =
        ScholarlyIdentifiers.extractArxivDoiId(input)


    /*
     * ================================================================
     * DOI DETECTION
     * ================================================================
     */
    private fun extractDoi(input: String): String? =
        ScholarlyIdentifiers.extractDoi(input)


    /*
     * ================================================================
     * ARXIV DETECTION
     * ================================================================
     */
    private fun extractArxivId(input: String): String? =
        ScholarlyIdentifiers.extractArxivId(input)


    /*
     * ================================================================
     * UTILITY
     * ================================================================
     */
    private fun String.normalizeWhitespace(): String {

        return this
            .replace(
                Regex(
                    "\\s+"
                ),
                " "
            )
            .trim()
    }
}