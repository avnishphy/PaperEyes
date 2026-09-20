package com.example.papereyes.domain.citation

import java.util.Calendar


object JournalCitationParser {

    /*
     * ================================================================
     * PUBLIC API
     * ================================================================
     */
    fun parse(
        input: String
    ): JournalCitation? {

        val normalized =
            normalizeInput(
                input
            )


        if (
            normalized.length < 6
        ) {

            return null
        }


        /*
         * PaperResolver should test DOI/arXiv first anyway, but these
         * guards keep the citation parser safe when used independently.
         */
        if (
            isStandaloneDoi(
                normalized
            )
        ) {

            return null
        }


        if (
            isStandaloneArxiv(
                normalized
            )
        ) {

            return null
        }


        val fallbackYear =
            extractUniqueYear(
                normalized
            )


        val candidates =
            buildCandidateStrings(
                normalized
            )


        val matches =
            candidates.mapNotNull { candidate ->

                parseCandidate(
                    candidate =
                        candidate,

                    fallbackYear =
                        fallbackYear,

                    original =
                        normalized
                )
            }


        return matches
            .maxByOrNull {

                it.confidence
            }
    }


    /*
     * ================================================================
     * CANDIDATE PARSING
     * ================================================================
     */
    private fun parseCandidate(
        candidate: String,
        fallbackYear: Int?,
        original: String
    ): JournalCitation? {

        return parseIeeeVerbose(
            candidate,
            original
        )
            ?: parseBiomedicalStyle(
                candidate,
                original
            )
            ?: parseAcsYearFirst(
                candidate,
                original
            )

            /*
             * IMPORTANT: parser order matters here.
             *
             * Journal-specific and unambiguous one-locator forms must run
             * before the generic VOLUME (YEAR) ISSUE LOCATOR parser.
             * Otherwise that regex can backtrack and split a single locator
             * such as `123` into issue=`12`, locator=`3`.
             *
             * The generic issue+locator parser still runs before the
             * Springer-style YEAR VOLUME LOCATOR parser so an input such as
             * `Phys.Rev.D 112 (2025) 3, 034009` keeps volume=112, issue=3.
             */
            ?: parseJhepJcapJinst(
                candidate,
                original
            )
            ?: parsePosStyle(
                candidate,
                original
            )
            ?: parseVolumeYearLocator(
                candidate,
                original
            )

            /*
             * The two-field tail must come after the one-locator forms above.
             */
            ?: parseVolumeYearIssueLocator(
                candidate,
                original
            )

            ?: parseSpringerYearFirst(
                candidate,
                original
            )
            ?: parseVolumeIssueLocatorYear(
                candidate,
                original
            )
            ?: parseVolumeIssueColonLocatorYear(
                candidate,
                original
            )
            ?: parseVolumeNoIssueLocatorYear(
                candidate,
                original
            )
            ?: parseVolumeLocatorYear(
                candidate,
                original
            )
            ?: parseVolumeColonLocatorYear(
                candidate,
                original
            )
            ?: parseVolumeLocatorCommaYear(
                candidate,
                original
            )
            ?: parseCompactVolumeLocatorYear(
                candidate,
                original
            )
            ?: parseVolumeIssueLocatorNoYear(
                candidate,
                original,
                fallbackYear
            )
            ?: parseVolumeLocatorNoYear(
                candidate,
                original,
                fallbackYear
            )
    }


    /*
     * ================================================================
     * IEEE STYLE
     * ================================================================
     *
     * IEEE Trans. Pattern Anal. Mach. Intell.,
     * vol. 45, no. 7, pp. 1234-1245, 2023
     */
    private fun parseIeeeVerbose(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s*
                vol\.?
                \s*
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*,?\s*
                (?:
                    no\.?
                    \s*
                    ([A-Za-z]?\d+)
                    \s*,?\s*
                )?
                (?:
                    pp?\.?
                    \s*
                )?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*,?\s*
                (\d{4})
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            issue =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            year =
                match.groupValues[5]
                    .toIntOrNull(),

            baseConfidence =
                0.98
        )
    }


    /*
     * ================================================================
     * VANCOUVER / MEDICAL
     * ================================================================
     *
     * N Engl J Med. 2024;390(3):123-130
     *
     * Lancet. 2024;403:123-130
     *
     * JAMA. 2024;331(4):301-310
     */
    private fun parseBiomedicalStyle(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \.?
                \s+
                (\d{4})
                \s*;\s*
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                (?:
                    \(
                        ([A-Za-z]?\d+)
                    \)
                )?
                \s*:\s*
                (?:pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            year =
                match.groupValues[2]
                    .toIntOrNull(),

            volume =
                match.groupValues[3],

            issue =
                match.groupValues[4],

            locator =
                match.groupValues[5],

            baseConfidence =
                0.98
        )
    }


    /*
     * ================================================================
     * ACS / CHEMISTRY STYLE
     * ================================================================
     *
     * J. Am. Chem. Soc. 2024, 146, 1234-1245
     */
    private fun parseAcsYearFirst(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s+
                (\d{4})
                \s*,\s*
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*,\s*
                (?:pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            year =
                match.groupValues[2]
                    .toIntOrNull(),

            volume =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            issue =
                null,

            baseConfidence =
                0.96
        )
    }


    /*
     * ================================================================
     * SPRINGER STYLE
     * ================================================================
     *
     * Eur. Phys. J. C (2024) 84:123
     *
     * Journal Name (2024) 12:345
     */
    private fun parseSpringerYearFirst(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*
                \(
                    (\d{4})
                \)
                \s*
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                [:,]?
                \s*
                (?:article\s+)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            year =
                match.groupValues[2]
                    .toIntOrNull(),

            volume =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            issue =
                null,

            baseConfidence =
                0.96
        )
    }


    /*
     * ================================================================
     * JHEP / JCAP / JINST
     * ================================================================
     *
     * JHEP 09 (2025) 123
     * JCAP 03 (2024) 012
     * JINST 19 (2024) P05001
     */
    private fun parseJhepJcapJinst(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (JHEP|JCAP|JINST)
                \s+
                ([A-Za-z0-9]+)
                \s*
                \(
                    (\d{4})
                \)
                \s*
                [,;:]?
                \s*
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*)
                \s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        val journal =
            match.groupValues[1]


        val middle =
            match.groupValues[2]


        return if (
            journal.equals(
                "JINST",
                ignoreCase =
                    true
            )
        ) {

            makeCitation(
                original =
                    original,

                journalRaw =
                    journal,

                volume =
                    middle,

                issue =
                    null,

                year =
                    match.groupValues[3]
                        .toIntOrNull(),

                locator =
                    match.groupValues[4],

                baseConfidence =
                    0.995
            )

        } else {

            /*
             * JHEP / JCAP use the first number as issue/month.
             */
            makeCitation(
                original =
                    original,

                journalRaw =
                    journal,

                volume =
                    null,

                issue =
                    middle.padStart(
                        2,
                        '0'
                    ),

                year =
                    match.groupValues[3]
                        .toIntOrNull(),

                locator =
                    match.groupValues[4],

                baseConfidence =
                    0.995
            )
        }
    }


    /*
     * ================================================================
     * PROCEEDINGS OF SCIENCE
     * ================================================================
     *
     * PoS DIS2024 (2024) 123
     */
    private fun parsePosStyle(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (PoS)
                \s+
                ([A-Za-z0-9_.-]+)
                \s*
                \(
                    (\d{4})
                \)
                \s*
                [,;:]?
                \s*
                (\d+)
                \s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            year =
                match.groupValues[3]
                    .toIntOrNull(),

            locator =
                match.groupValues[4],

            issue =
                null,

            baseConfidence =
                0.98
        )
    }


    /*
     * ================================================================
     * USER'S FORMAT
     * ================================================================
     *
     * Phys.Rev.D 112 (2025) 3, 034009
     */
    private fun parseVolumeYearIssueLocator(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                \(
                    (\d{4})
                \)
                \s*
                (?:
                    no\.?\s*
                )?
                ([A-Za-z]?\d+)
                \s*
                [,;:]?
                \s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            year =
                match.groupValues[3]
                    .toIntOrNull(),

            issue =
                match.groupValues[4],

            locator =
                match.groupValues[5],

            baseConfidence =
                0.99
        )
    }


    /*
     * ================================================================
     * VOLUME (YEAR) LOCATOR
     * ================================================================
     *
     * Phys. Lett. B 850 (2024) 138500
     * J. Phys. G 51 (2024) 045001
     */
    private fun parseVolumeYearLocator(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                \(
                    (\d{4})
                \)
                \s*
                [,;:]?
                \s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            year =
                match.groupValues[3]
                    .toIntOrNull(),

            issue =
                null,

            locator =
                match.groupValues[4],

            baseConfidence =
                0.97
        )
    }


    /*
     * ================================================================
     * VOLUME(ISSUE), LOCATOR (YEAR)
     * ================================================================
     *
     * Journal 42(3), 123-130 (2024)
     */
    private fun parseVolumeIssueLocatorYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                \(
                    ([A-Za-z]?\d+)
                \)
                \s*
                [,;]?
                \s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*
                \(
                    (\d{4})
                \)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            issue =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            year =
                match.groupValues[5]
                    .toIntOrNull(),

            baseConfidence =
                0.98
        )
    }


    /*
     * ================================================================
     * VOLUME(ISSUE):LOCATOR (YEAR)
     * ================================================================
     *
     * PLoS ONE 19(3):e0299999 (2024)
     */
    private fun parseVolumeIssueColonLocatorYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                \(
                    ([A-Za-z]?\d+)
                \)
                \s*:\s*
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*
                \(
                    (\d{4})
                \)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            issue =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            year =
                match.groupValues[5]
                    .toIntOrNull(),

            baseConfidence =
                0.98
        )
    }


    /*
     * ================================================================
     * VOLUME, NO. ISSUE, LOCATOR (YEAR)
     * ================================================================
     *
     * Phys. Rev. D 112, no. 3, 034009 (2025)
     */
    private fun parseVolumeNoIssueLocatorYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*,\s*
                no\.?
                \s*
                ([A-Za-z]?\d+)
                \s*,\s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*
                \(
                    (\d{4})
                \)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            issue =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            year =
                match.groupValues[5]
                    .toIntOrNull(),

            baseConfidence =
                0.99
        )
    }


    /*
     * ================================================================
     * VOLUME, LOCATOR (YEAR)
     * ================================================================
     *
     * Phys. Rev. D 112, 034009 (2025)
     * Nature 630, 123-127 (2024)
     * Science 385, eabc1234 (2024)
     */
    private fun parseVolumeLocatorYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*,\s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*
                \(
                    (\d{4})
                \)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            locator =
                match.groupValues[3],

            year =
                match.groupValues[4]
                    .toIntOrNull(),

            issue =
                null,

            baseConfidence =
                0.96
        )
    }


    /*
     * ================================================================
     * VOLUME:LOCATOR (YEAR)
     * ================================================================
     *
     * Front. Phys. 12:1234567 (2024)
     * Astrophys. J. 950:123 (2023)
     */
    private fun parseVolumeColonLocatorYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*:\s*
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*
                \(
                    (\d{4})
                \)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            locator =
                match.groupValues[3],

            year =
                match.groupValues[4]
                    .toIntOrNull(),

            issue =
                null,

            baseConfidence =
                0.96
        )
    }


    /*
     * ================================================================
     * VOLUME, LOCATOR, YEAR
     * ================================================================
     *
     * Journal 12, 123-130, 2024
     */
    private fun parseVolumeLocatorCommaYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,?\s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*,\s*
                (?:pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*,\s*
                (\d{4})
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        return makeCitation(
            original =
                original,

            journalRaw =
                match.groupValues[1],

            volume =
                match.groupValues[2],

            locator =
                match.groupValues[3],

            year =
                match.groupValues[4]
                    .toIntOrNull(),

            issue =
                null,

            baseConfidence =
                0.93
        )
    }


    /*
     * ================================================================
     * COMPACT STYLE
     * ================================================================
     *
     * J. Phys. G 51 045001 (2024)
     *
     * Only accepted confidently for known / journal-like names.
     */
    private fun parseCompactVolumeLocatorYear(
        text: String,
        original: String
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s+
                ([A-Za-z]?\d+[A-Za-z]?)
                \s+
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*)
                \s*
                \(
                    (\d{4})
                \)
                \s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        val journalRaw =
            match.groupValues[1]


        if (
            !JournalAliases.isKnown(
                journalRaw
            ) &&
            !looksJournalLike(
                journalRaw
            )
        ) {

            return null
        }


        return makeCitation(
            original =
                original,

            journalRaw =
                journalRaw,

            volume =
                match.groupValues[2],

            locator =
                match.groupValues[3],

            year =
                match.groupValues[4]
                    .toIntOrNull(),

            issue =
                null,

            baseConfidence =
                0.90
        )
    }


    /*
     * ================================================================
     * APA-LIKE TAIL WITHOUT YEAR
     * ================================================================
     *
     * Physical Review D, 112(3), 034009
     *
     * If the complete reference contains one unambiguous year:
     *
     * Smith ... (2025). Title. Physical Review D, 112(3), 034009.
     *
     * we recover 2025 from the full reference.
     */
    private fun parseVolumeIssueLocatorNoYear(
        text: String,
        original: String,
        fallbackYear: Int?
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,\s*
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*
                \(
                    ([A-Za-z]?\d+)
                \)
                \s*,\s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        val journalRaw =
            match.groupValues[1]


        if (
            !JournalAliases.isKnown(
                journalRaw
            ) &&
            !looksJournalLike(
                journalRaw
            )
        ) {

            return null
        }


        return makeCitation(
            original =
                original,

            journalRaw =
                journalRaw,

            volume =
                match.groupValues[2],

            issue =
                match.groupValues[3],

            locator =
                match.groupValues[4],

            year =
                fallbackYear,

            baseConfidence =
                if (
                    fallbackYear != null
                ) {
                    0.90
                } else {
                    0.80
                }
        )
    }


    /*
     * ================================================================
     * JOURNAL, VOLUME, LOCATOR — NO YEAR
     * ================================================================
     */
    private fun parseVolumeLocatorNoYear(
        text: String,
        original: String,
        fallbackYear: Int?
    ): JournalCitation? {

        val regex =
            Regex(
                """
                ^\s*
                (.+?)
                \s*,\s*
                ([A-Za-z]?\d+[A-Za-z]?)
                \s*,\s*
                (?:article\s+|pp?\.?\s*)?
                ([A-Za-z]+\d+[A-Za-z0-9]*|\d+[A-Za-z0-9]*(?:-\s*[A-Za-z]?\d+[A-Za-z0-9]*)?)
                \s*\.?\s*$
                """.trimIndent(),
                setOf(
                    RegexOption.COMMENTS,
                    RegexOption.IGNORE_CASE
                )
            )


        val match =
            regex.matchEntire(
                text
            ) ?: return null


        val journalRaw =
            match.groupValues[1]


        if (
            !JournalAliases.isKnown(
                journalRaw
            ) &&
            !looksJournalLike(
                journalRaw
            )
        ) {

            return null
        }


        return makeCitation(
            original =
                original,

            journalRaw =
                journalRaw,

            volume =
                match.groupValues[2],

            locator =
                match.groupValues[3],

            year =
                fallbackYear,

            issue =
                null,

            baseConfidence =
                if (
                    fallbackYear != null
                ) {
                    0.87
                } else {
                    0.76
                }
        )
    }


    /*
     * ================================================================
     * RESULT CREATION
     * ================================================================
     */
    private fun makeCitation(
        original: String,
        journalRaw: String,
        volume: String?,
        issue: String?,
        year: Int?,
        locator: String?,
        baseConfidence: Double
    ): JournalCitation? {

        val cleanedJournal =
            cleanJournalName(
                journalRaw
            )


        if (
            cleanedJournal.length < 2
        ) {

            return null
        }


        if (
            !cleanedJournal.any {
                it.isLetter()
            }
        ) {

            return null
        }


        val validYear =
            year
                ?.takeIf {

                    isReasonableYear(
                        it
                    )
                }


        if (
            year != null &&
            validYear == null
        ) {

            return null
        }


        val knownJournal =
            JournalAliases.isKnown(
                cleanedJournal
            )


        val canonicalJournal =
            JournalAliases.canonicalize(
                cleanedJournal
            )


        val normalizedVolume =
            cleanOptionalField(
                volume
            )


        val normalizedIssue =
            cleanOptionalField(
                issue
            )


        val normalizedLocator =
            cleanLocator(
                locator
            )


        /*
         * A citation with no volume and no issue is generally too weak,
         * except special formats that already carry high confidence.
         */
        if (
            normalizedVolume == null &&
            normalizedIssue == null
        ) {

            return null
        }


        if (
            normalizedLocator == null
        ) {

            return null
        }


        var confidence =
            baseConfidence


        if (knownJournal) {

            confidence +=
                0.02
        }


        if (
            normalizedIssue != null
        ) {

            confidence +=
                0.005
        }


        confidence =
            confidence.coerceIn(
                0.0,
                0.995
            )


        return JournalCitation(
            raw =
                original,

            journal =
                canonicalJournal,

            journalRaw =
                cleanedJournal,

            volume =
                normalizedVolume,

            issue =
                normalizedIssue,

            year =
                validYear,

            locator =
                normalizedLocator,

            confidence =
                confidence
        )
    }


    /*
     * ================================================================
     * AUTHOR / TITLE PREFIX HANDLING
     * ================================================================
     *
     * Examples:
     *
     * A. Smith et al., Phys. Rev. D 112, 034009 (2025)
     *
     * Smith, J. (2025). Paper title.
     * Physical Review D, 112(3), 034009.
     *
     * We try likely suffixes rather than forcing the parser to
     * understand author-name syntax.
     */
    private fun buildCandidateStrings(
        text: String
    ): List<String> {

        val candidates =
            linkedSetOf<String>()


        candidates.add(
            text
        )


        /*
         * Try suffixes after commas.
         */
        text.forEachIndexed { index, char ->

            if (
                char == ','
            ) {

                val tail =
                    text
                        .substring(
                            index + 1
                        )
                        .trim()


                if (
                    tail.length >= 8
                ) {

                    candidates.add(
                        tail
                    )
                }
            }
        }


        /*
         * Try suffixes after sentence boundaries. This catches
         * APA-style:
         *
         * Author. (2025). Title. Journal, volume(issue), page.
         */
        Regex("""[.!?]\s+""")
            .findAll(
                text
            )
            .forEach { match ->

                val tail =
                    text
                        .substring(
                            match.range.last + 1
                        )
                        .trim()


                if (
                    tail.length >= 8
                ) {

                    candidates.add(
                        tail
                    )
                }
            }


        return candidates.toList()
    }


    /*
     * ================================================================
     * YEAR FALLBACK
     * ================================================================
     *
     * Use a year from the full reference only when there is exactly
     * one plausible year.
     */
    private fun extractUniqueYear(
        text: String
    ): Int? {

        val years =
            Regex(
                """\b(?:18|19|20|21)\d{2}\b"""
            )
                .findAll(
                    text
                )
                .mapNotNull {

                    it.value
                        .toIntOrNull()
                }
                .filter {

                    isReasonableYear(
                        it
                    )
                }
                .distinct()
                .toList()


        return if (
            years.size == 1
        ) {

            years.first()

        } else {

            null
        }
    }


    /*
     * ================================================================
     * NORMALIZATION
     * ================================================================
     */
    private fun normalizeInput(
        value: String
    ): String {

        return value
            .replace(
                '\u00A0',
                ' '
            )
            .replace(
                '–',
                '-'
            )
            .replace(
                '—',
                '-'
            )
            .replace(
                '\n',
                ' '
            )
            .replace(
                '\r',
                ' '
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }


    private fun cleanJournalName(
        value: String
    ): String {

        return value
            .trim()
            .trim(
                ',',
                ';',
                ':'
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
    }


    private fun cleanOptionalField(
        value: String?
    ): String? {

        return value
            ?.trim()
            ?.takeIf {

                it.isNotBlank()
            }
    }


    private fun cleanLocator(
        value: String?
    ): String? {

        return value
            ?.replace(
                Regex(
                    """(?i)^(?:article|pp?|pages?)\.?\s+"""
                ),
                ""
            )
            ?.replace(
                " ",
                ""
            )
            ?.replace(
                '–',
                '-'
            )
            ?.replace(
                '—',
                '-'
            )
            ?.trim()
            ?.takeIf {

                it.isNotBlank()
            }
    }


    /*
     * ================================================================
     * FALSE-POSITIVE PROTECTION
     * ================================================================
     */
    private fun looksJournalLike(
        value: String
    ): Boolean {

        if (
            JournalAliases.isKnown(
                value
            )
        ) {

            return true
        }


        val lower =
            value.lowercase()


        if (
            '.' in value
        ) {

            return true
        }


        val journalWords =
            listOf(
                "journal",
                "review",
                "reviews",
                "letters",
                "physics",
                "science",
                "nature",
                "proceedings",
                "transactions",
                "annals",
                "communications",
                "reports",
                "bulletin",
                "astrophysical",
                "astronomical",
                "chemistry",
                "chemical",
                "medical",
                "medicine",
                "nuclear",
                "physical",
                "applied",
                "frontiers",
                "plos",
                "ieee",
                "acm"
            )


        return journalWords.any {

            it in lower
        }
    }


    private fun isReasonableYear(
        year: Int
    ): Boolean {

        val currentYear =
            Calendar
                .getInstance()
                .get(
                    Calendar.YEAR
                )


        return year in
                1600..(currentYear + 1)
    }


    /*
     * ================================================================
     * DOI / ARXIV GUARDS
     * ================================================================
     */
    private fun isStandaloneDoi(
        value: String
    ): Boolean {

        return Regex(
            """(?i)^(?:https?://(?:dx\.)?doi\.org/)?10\.\d{4,9}/\S+$"""
        ).matches(
            value
        )
    }


    private fun isStandaloneArxiv(
        value: String
    ): Boolean {

        return Regex(
            """(?i)^(?:https?://(?:www\.)?arxiv\.org/(?:abs|pdf)/|arxiv:\s*)?\d{4}\.\d{4,5}(?:v\d+)?(?:\.pdf)?$"""
        ).matches(
            value
        )
    }
}