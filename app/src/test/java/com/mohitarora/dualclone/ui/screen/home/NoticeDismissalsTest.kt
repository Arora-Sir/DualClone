package com.mohitarora.dualclone.ui.screen.home

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NoticeDismissalsTest {

    @Test
    fun nothingIsDismissedAtFirst() {
        assertFalse(NoticeDismissals.isDismissed("", NoticeDismissals.pausedKey(10)))
        assertEquals(emptyList<String>(), NoticeDismissals.keys(""))
    }

    @Test
    fun dismissingAKeyHidesOnlyThatNotice() {
        val raw = NoticeDismissals.dismiss("", NoticeDismissals.pausedKey(10))
        assertTrue(NoticeDismissals.isDismissed(raw, "paused:10"))
        assertFalse(NoticeDismissals.isDismissed(raw, "paused:95"))
        assertFalse(NoticeDismissals.isDismissed(raw, "stale"))
    }

    @Test
    fun dismissingTheSameKeyTwiceStoresItOnce() {
        val raw = NoticeDismissals.dismiss(NoticeDismissals.dismiss("", "stale"), "stale")
        assertEquals(listOf("stale"), NoticeDismissals.keys(raw))
    }

    @Test
    fun aNoticeThatNoLongerAppliesIsForgottenSoItReturnsWithTheProblem() {
        val raw = NoticeDismissals.dismiss(NoticeDismissals.dismiss("", "paused:10"), "stale")
        val kept = NoticeDismissals.keepActive(raw, activeKeys = listOf("stale"))
        assertEquals(listOf("stale"), NoticeDismissals.keys(kept))
        assertFalse(NoticeDismissals.isDismissed(kept, "paused:10"))
    }

    @Test
    fun tappingTheProfileChipAgainReopensItsPausedNoticeOnly() {
        val raw = NoticeDismissals.dismiss(NoticeDismissals.dismiss("", "paused:10"), "paused:95")
        val reopened = NoticeDismissals.reopenPaused(raw, userId = 10)
        assertFalse(NoticeDismissals.isDismissed(reopened, "paused:10"))
        assertTrue(NoticeDismissals.isDismissed(reopened, "paused:95"))
    }

    @Test
    fun reopeningWhenNothingWasDismissedChangesNothing() {
        assertEquals("", NoticeDismissals.reopenPaused("", userId = 10))
    }
}
