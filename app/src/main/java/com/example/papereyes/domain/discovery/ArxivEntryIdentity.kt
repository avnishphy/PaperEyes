package com.example.papereyes.domain.discovery

import com.example.papereyes.data.model.ScholarlyIdentifiers

object ArxivEntryIdentity {
    fun matches(requested: String, returned: String?, title: String?): Boolean {
        if (title.isNullOrBlank() || title.trim().equals("Error", ignoreCase=true)) return false
        val expected = ScholarlyIdentifiers.extractArxivId(requested) ?: return false
        val actual = returned?.let(ScholarlyIdentifiers::extractArxivId) ?: return false
        if (ScholarlyIdentifiers.arxivWorkId(expected) != ScholarlyIdentifiers.arxivWorkId(actual)) return false
        // Explicit version requests must never silently return another version.
        return !Regex("v[1-9][0-9]*$").containsMatchIn(expected) || actual == expected
    }
}
