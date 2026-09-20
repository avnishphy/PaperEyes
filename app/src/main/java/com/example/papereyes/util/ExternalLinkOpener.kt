package com.example.papereyes.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import java.util.Locale


object ExternalLinkOpener {

    private val allowedSchemes =
        setOf(
            "https",
            "http"
        )


    /**
     * Returns true only for normal web URLs that are safe to hand to an
     * external browser. This deliberately rejects intent:, file:, content:,
     * javascript:, custom application schemes, and malformed/hostless URLs.
     */
    fun isSupportedWebUrl(
        rawUrl: String?
    ): Boolean {

        return parseSupportedWebUri(
            rawUrl
        ) != null
    }


    /**
     * Opens a validated HTTP(S) URL using an external activity.
     *
     * Returns false when the URL is invalid/unsupported or when the device has
     * no activity capable of opening it. Callers can then show a simple,
     * user-facing message without exposing an internal exception.
     */
    fun open(
        context: Context,
        rawUrl: String?
    ): Boolean {

        val uri =
            parseSupportedWebUri(
                rawUrl
            )
                ?: return false


        val intent =
            Intent(
                Intent.ACTION_VIEW,
                uri
            ).apply {

                addCategory(
                    Intent.CATEGORY_BROWSABLE
                )
            }


        return try {

            context.startActivity(
                intent
            )

            true

        } catch (
            _: ActivityNotFoundException
        ) {

            false

        } catch (
            _: SecurityException
        ) {

            false
        }
    }


    private fun parseSupportedWebUri(
        rawUrl: String?
    ): Uri? {

        val cleaned =
            rawUrl
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?: return null


        /*
         * Reject control characters before parsing. Besides being malformed
         * for a normal paper link, they can make logs/UI/debugging ambiguous.
         */
        if (
            cleaned.any {
                it.code < 0x20 ||
                        it.code == 0x7f
            }
        ) {

            return null
        }


        val uri =
            try {

                Uri.parse(
                    cleaned
                )

            } catch (
                _: Exception
            ) {

                return null
            }


        val scheme =
            uri.scheme
                ?.lowercase(
                    Locale.ROOT
                )
                ?: return null


        if (
            scheme !in allowedSchemes
        ) {

            return null
        }


        if (
            uri.host.isNullOrBlank()
        ) {

            return null
        }


        return uri
    }
}
