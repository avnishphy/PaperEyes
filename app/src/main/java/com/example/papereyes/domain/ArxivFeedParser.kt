package com.example.papereyes.domain

import com.example.papereyes.data.model.Paper
import com.example.papereyes.data.model.normalizePaperDoi
import com.example.papereyes.domain.discovery.ArxivEntryIdentity
import org.xmlpull.v1.XmlPullParser

/** Entry-scoped parsing: a feed error or unrelated entry is never a paper match. */
internal object ArxivFeedParser {
    fun parse(parser: XmlPullParser, requestedId: String): Paper? {
        var insideEntry = false
        var insideAuthor = false
        var id: String? = null
        var title: String? = null
        var published: String? = null
        var doi: String? = null
        val authors = mutableListOf<String>()
        var event = parser.eventType
        var events = 0
        while (event != XmlPullParser.END_DOCUMENT && ++events <= 20_000) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "entry" -> {
                        insideEntry = true; insideAuthor = false
                        id = null; title = null; published = null; doi = null; authors.clear()
                    }
                    "id" -> if (insideEntry) id = parser.nextText().trim()
                    "title" -> if (insideEntry) title = clean(parser.nextText())
                    "published" -> if (insideEntry) published = parser.nextText().trim()
                    "doi" -> if (insideEntry) doi = normalizePaperDoi(parser.nextText())
                    "author" -> if (insideEntry) insideAuthor = true
                    "name" -> if (insideEntry && insideAuthor) {
                        clean(parser.nextText()).takeIf { it.isNotBlank() && authors.size < 200 }?.let(authors::add)
                    }
                }
                XmlPullParser.END_TAG -> when (parser.name) {
                    "author" -> insideAuthor = false
                    "entry" -> {
                        if (ArxivEntryIdentity.matches(requestedId, id, title)) {
                            return Paper(title=title.orEmpty(), authors=authors.joinToString(", "),
                                year=published?.take(4)?.toIntOrNull(), doi=doi,
                                url="https://arxiv.org/abs/$requestedId")
                        }
                        insideEntry = false
                    }
                }
            }
            event = parser.next()
        }
        return null
    }
    private fun clean(text: String) = text.replace(Regex("\\s+"), " ").trim()
}
