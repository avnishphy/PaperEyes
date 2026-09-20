package com.example.papereyes.domain.citation


object JournalAliases {

    private val aliases: Map<String, String> =
        buildMap {

            fun add(
                canonical: String,
                vararg names: String
            ) {

                put(
                    normalizeKey(canonical),
                    canonical
                )

                names.forEach { name ->

                    put(
                        normalizeKey(name),
                        canonical
                    )
                }
            }


            /*
             * ========================================================
             * APS
             * ========================================================
             */
            add(
                "Physical Review Letters",
                "PRL",
                "Phys Rev Lett",
                "Phys. Rev. Lett.",
                "Physical Review Letters"
            )

            add(
                "Physical Review A",
                "PRA",
                "Phys Rev A",
                "Phys. Rev. A"
            )

            add(
                "Physical Review B",
                "PRB",
                "Phys Rev B",
                "Phys. Rev. B"
            )

            add(
                "Physical Review C",
                "PRC",
                "Phys Rev C",
                "Phys. Rev. C"
            )

            add(
                "Physical Review D",
                "PRD",
                "Phys Rev D",
                "Phys. Rev. D",
                "Phys.Rev.D"
            )

            add(
                "Physical Review E",
                "PRE",
                "Phys Rev E",
                "Phys. Rev. E"
            )

            add(
                "Physical Review X",
                "PRX",
                "Phys Rev X",
                "Phys. Rev. X"
            )

            add(
                "Physical Review Research",
                "PR Research",
                "Phys Rev Research",
                "Phys. Rev. Research"
            )

            add(
                "Physical Review Applied",
                "PR Applied",
                "Phys Rev Applied",
                "Phys. Rev. Applied"
            )

            add(
                "Physical Review Accelerators and Beams",
                "PRAB",
                "Phys Rev Accel Beams",
                "Phys. Rev. Accel. Beams"
            )

            add(
                "Reviews of Modern Physics",
                "RMP",
                "Rev Mod Phys",
                "Rev. Mod. Phys."
            )


            /*
             * ========================================================
             * HIGH ENERGY / NUCLEAR
             * ========================================================
             */
            add(
                "Journal of High Energy Physics",
                "JHEP",
                "J. High Energy Phys.",
                "Journal of High Energy Physics"
            )

            add(
                "Journal of Cosmology and Astroparticle Physics",
                "JCAP",
                "J. Cosmol. Astropart. Phys."
            )

            add(
                "Journal of Instrumentation",
                "JINST",
                "J. Instrum.",
                "Journal of Instrumentation"
            )

            add(
                "Physics Letters B",
                "PLB",
                "Phys Lett B",
                "Phys. Lett. B",
                "Physics Letters B"
            )

            add(
                "Physics Letters A",
                "PLA",
                "Phys Lett A",
                "Phys. Lett. A"
            )

            add(
                "Nuclear Physics A",
                "NPA",
                "Nucl Phys A",
                "Nucl. Phys. A"
            )

            add(
                "Nuclear Physics B",
                "NPB",
                "Nucl Phys B",
                "Nucl. Phys. B"
            )

            add(
                "The European Physical Journal A",
                "EPJA",
                "Eur Phys J A",
                "Eur. Phys. J. A"
            )

            add(
                "The European Physical Journal C",
                "EPJC",
                "Eur Phys J C",
                "Eur. Phys. J. C"
            )

            add(
                "Journal of Physics G",
                "J Phys G",
                "J. Phys. G",
                "J. Phys. G: Nucl. Part. Phys.",
                "Journal of Physics G"
            )

            add(
                "Chinese Physics C",
                "CPC",
                "Chin Phys C",
                "Chin. Phys. C"
            )

            add(
                "Reports on Progress in Physics",
                "Rep Prog Phys",
                "Rep. Prog. Phys."
            )

            add(
                "Physics Reports",
                "Phys Rep",
                "Phys. Rep."
            )

            add(
                "Progress in Particle and Nuclear Physics",
                "Prog Part Nucl Phys",
                "Prog. Part. Nucl. Phys."
            )

            add(
                "Annals of Physics",
                "Ann Phys",
                "Ann. Phys."
            )

            add(
                "Nuclear Instruments and Methods in Physics Research Section A",
                "NIM A",
                "Nucl Instrum Meth A",
                "Nucl. Instrum. Meth. A",
                "Nucl. Instrum. Methods Phys. Res. A"
            )

            add(
                "Nuclear Instruments and Methods in Physics Research Section B",
                "NIM B",
                "Nucl Instrum Meth B",
                "Nucl. Instrum. Meth. B"
            )

            add(
                "Proceedings of Science",
                "PoS",
                "Proc Sci"
            )


            /*
             * ========================================================
             * GENERAL PHYSICS
             * ========================================================
             */
            add(
                "New Journal of Physics",
                "NJP",
                "New J Phys",
                "New J. Phys."
            )

            add(
                "Journal of Applied Physics",
                "J Appl Phys",
                "J. Appl. Phys."
            )

            add(
                "Applied Physics Letters",
                "APL",
                "Appl Phys Lett",
                "Appl. Phys. Lett."
            )

            add(
                "Journal of Chemical Physics",
                "J Chem Phys",
                "J. Chem. Phys."
            )

            add(
                "Physics of Plasmas",
                "Phys Plasmas",
                "Phys. Plasmas"
            )

            add(
                "Physica A: Statistical Mechanics and its Applications",
                "Physica A"
            )


            /*
             * ========================================================
             * CHEMISTRY
             * ========================================================
             */
            add(
                "Journal of the American Chemical Society",
                "JACS",
                "J Am Chem Soc",
                "J. Am. Chem. Soc."
            )

            add(
                "Chemical Reviews",
                "Chem Rev",
                "Chem. Rev."
            )

            add(
                "The Journal of Physical Chemistry A",
                "J Phys Chem A",
                "J. Phys. Chem. A"
            )

            add(
                "The Journal of Physical Chemistry B",
                "J Phys Chem B",
                "J. Phys. Chem. B"
            )

            add(
                "The Journal of Physical Chemistry C",
                "J Phys Chem C",
                "J. Phys. Chem. C"
            )

            add(
                "The Journal of Physical Chemistry Letters",
                "J Phys Chem Lett",
                "J. Phys. Chem. Lett."
            )

            add(
                "Physical Chemistry Chemical Physics",
                "PCCP",
                "Phys Chem Chem Phys",
                "Phys. Chem. Chem. Phys."
            )

            add(
                "ACS Nano",
                "ACS Nano"
            )

            add(
                "Nano Letters",
                "Nano Lett",
                "Nano Lett."
            )

            add(
                "Angewandte Chemie International Edition",
                "Angew Chem Int Ed",
                "Angew. Chem. Int. Ed."
            )


            /*
             * ========================================================
             * ASTRONOMY / ASTROPHYSICS
             * ========================================================
             */
            add(
                "The Astrophysical Journal",
                "ApJ",
                "Astrophys J",
                "Astrophys. J."
            )

            add(
                "The Astrophysical Journal Letters",
                "ApJL",
                "Astrophys J Lett",
                "Astrophys. J. Lett."
            )

            add(
                "The Astrophysical Journal Supplement Series",
                "ApJS",
                "Astrophys J Suppl",
                "Astrophys. J. Suppl."
            )

            add(
                "The Astronomical Journal",
                "AJ",
                "Astron J",
                "Astron. J."
            )

            add(
                "Monthly Notices of the Royal Astronomical Society",
                "MNRAS",
                "Mon Not R Astron Soc",
                "Mon. Not. R. Astron. Soc."
            )

            add(
                "Astronomy & Astrophysics",
                "A&A",
                "Astron Astrophys",
                "Astron. Astrophys."
            )

            add(
                "Publications of the Astronomical Society of the Pacific",
                "PASP"
            )

            add(
                "Publications of the Astronomical Society of Japan",
                "PASJ"
            )

            add(
                "Nature Astronomy",
                "Nat Astron",
                "Nat. Astron."
            )

            add(
                "Icarus",
                "Icarus"
            )


            /*
             * ========================================================
             * GENERAL SCIENCE
             * ========================================================
             */
            add(
                "Nature",
                "Nature"
            )

            add(
                "Science",
                "Science"
            )

            add(
                "Science Advances",
                "Sci Adv",
                "Sci. Adv."
            )

            add(
                "Nature Physics",
                "Nat Phys",
                "Nat. Phys."
            )

            add(
                "Nature Communications",
                "Nat Commun",
                "Nat. Commun."
            )

            add(
                "Communications Physics",
                "Commun Phys",
                "Commun. Phys."
            )

            add(
                "Scientific Reports",
                "Sci Rep",
                "Sci. Rep."
            )

            add(
                "Proceedings of the National Academy of Sciences",
                "PNAS",
                "Proc Natl Acad Sci",
                "Proc. Natl. Acad. Sci."
            )

            add(
                "Cell",
                "Cell"
            )


            /*
             * ========================================================
             * MEDICAL / BIOLOGY
             * ========================================================
             */
            add(
                "The New England Journal of Medicine",
                "NEJM",
                "N Engl J Med",
                "N. Engl. J. Med."
            )

            add(
                "The Lancet",
                "Lancet",
                "The Lancet"
            )

            add(
                "JAMA",
                "JAMA"
            )

            add(
                "BMJ",
                "BMJ"
            )

            add(
                "PLoS ONE",
                "PLoS ONE",
                "PLOS ONE"
            )

            add(
                "PLoS Biology",
                "PLoS Biol",
                "PLOS Biology"
            )

            add(
                "eLife",
                "eLife"
            )

            add(
                "Frontiers in Physics",
                "Front Phys",
                "Front. Phys."
            )


            /*
             * ========================================================
             * COMPUTER SCIENCE / ENGINEERING
             * ========================================================
             */
            add(
                "IEEE Transactions on Pattern Analysis and Machine Intelligence",
                "IEEE TPAMI",
                "IEEE Trans Pattern Anal Mach Intell",
                "IEEE Trans. Pattern Anal. Mach. Intell."
            )

            add(
                "IEEE Transactions on Neural Networks and Learning Systems",
                "IEEE TNNLS",
                "IEEE Trans Neural Netw Learn Syst",
                "IEEE Trans. Neural Netw. Learn. Syst."
            )

            add(
                "IEEE Transactions on Information Theory",
                "IEEE Trans Inf Theory",
                "IEEE Trans. Inf. Theory"
            )

            add(
                "Journal of Machine Learning Research",
                "JMLR",
                "J Mach Learn Res"
            )

            add(
                "ACM Computing Surveys",
                "ACM Comput Surv",
                "ACM Comput. Surv."
            )

            add(
                "ACM Transactions on Graphics",
                "ACM Trans Graph",
                "ACM Trans. Graph."
            )

            add(
                "Nature Machine Intelligence",
                "Nat Mach Intell",
                "Nat. Mach. Intell."
            )
        }


    fun canonicalize(
        raw: String
    ): String {

        return aliases[
            normalizeKey(raw)
        ] ?: raw.trim()
    }


    fun isKnown(
        raw: String
    ): Boolean {

        return aliases.containsKey(
            normalizeKey(raw)
        )
    }


    fun normalizeKey(
        value: String
    ): String {

        return value
            .lowercase()
            .replace(
                "&",
                " and "
            )
            .replace(
                Regex("""[._,:;]+"""),
                " "
            )
            .replace(
                Regex("""[^\p{L}\p{N}]+"""),
                " "
            )
            .replace(
                Regex("""\s+"""),
                " "
            )
            .trim()
    }
}