package com.example.papereyes.ui.common

import com.example.papereyes.domain.PaperResolutionException


/**
 * Converts failures into text that is safe and useful to display in the UI.
 *
 * PaperResolutionException messages are intentionally written by PaperEyes and
 * may be shown to the user. Arbitrary Exception.message values are not shown:
 * they can contain implementation details, file paths, HTTP internals, device
 * information, or third-party response text.
 */
fun Throwable.toUserFacingMessage(
    fallback: String
): String {

    if (
        this is PaperResolutionException
    ) {

        val safeMessage =
            message
                ?.replace(
                    Regex("\\s+"),
                    " "
                )
                ?.trim()
                ?.takeIf {
                    it.isNotBlank()
                }
                ?.take(
                    240
                )


        if (
            safeMessage != null
        ) {

            return safeMessage
        }
    }


    return fallback
}
