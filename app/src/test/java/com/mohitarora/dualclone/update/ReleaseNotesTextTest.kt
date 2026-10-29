package com.mohitarora.dualclone.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ReleaseNotesTextTest {

    @Test
    fun headingsBecomeUppercaseLines() {
        assertEquals("FIXED\n• One thing.", ReleaseNotesText.plain("### Fixed\n* One thing."))
    }

    @Test
    fun bulletsUseOneSymbolWhateverTheMarker() {
        assertEquals("• a\n• b\n• c", ReleaseNotesText.plain("* a\n- b\n+ c"))
    }

    @Test
    fun backticksBoldAndLinksAreRemoved() {
        assertEquals("Check abc and see the site.", ReleaseNotesText.plain("Check `abc` and **see** the [site](https://example.com)."))
    }

    @Test
    fun theReleaseBodyOfV102ReadsCleanly() {
        val body = "### Fixed\n* Apps refresh on start.\n\n### Verify this release\n\nSigning certificate SHA-256:\n`ce587f`"
        val text = ReleaseNotesText.plain(body)
        assertFalse(text.contains("#"))
        assertFalse(text.contains("`"))
        assertEquals("FIXED\n• Apps refresh on start.\n\nVERIFY THIS RELEASE\n\nSigning certificate SHA-256:\nce587f", text)
    }

    @Test
    fun blankInputStaysBlank() {
        assertEquals("", ReleaseNotesText.plain("   \n\n"))
    }
}
