package com.example.papereyes.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalLinkOpenerTest {
    @Test
    fun acceptsNormalWebUrls() {
        assertTrue(ExternalLinkOpener.isSupportedWebUrl("https://doi.org/10.1000/example"))
        assertTrue(ExternalLinkOpener.isSupportedWebUrl("http://example.org/paper"))
    }

    @Test
    fun rejectsUnsafeOrMalformedSchemes() {
        assertFalse(ExternalLinkOpener.isSupportedWebUrl("javascript:alert(1)"))
        assertFalse(ExternalLinkOpener.isSupportedWebUrl("file:///data/data/secret"))
        assertFalse(ExternalLinkOpener.isSupportedWebUrl("content://example/item"))
        assertFalse(ExternalLinkOpener.isSupportedWebUrl("https:///missing-host"))
        assertFalse(ExternalLinkOpener.isSupportedWebUrl("https://example.org/\nmalformed"))
    }
}
