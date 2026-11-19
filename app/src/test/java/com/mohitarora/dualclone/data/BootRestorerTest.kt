package com.mohitarora.dualclone.data

import com.mohitarora.dualclone.data.repository.BootRestorer
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.ReconcileReport
import com.mohitarora.dualclone.domain.model.CloneOriginKind
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.model.StoredClone
import com.mohitarora.dualclone.domain.pm.CommandExit
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.shizuku.FakeShizukuGateway
import com.mohitarora.dualclone.testing.FakeClock
import com.mohitarora.dualclone.testing.FakeHostPackages
import com.mohitarora.dualclone.testing.FakeProfileRepository
import com.mohitarora.dualclone.testing.InMemoryCloneStateStore
import com.mohitarora.dualclone.testing.InMemorySettings
import com.mohitarora.dualclone.testing.RecordingAudit
import com.mohitarora.dualclone.testing.profile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootRestorerTest {

    private val gateway = FakeShizukuGateway()
    private val store = InMemoryCloneStateStore()
    private val profiles = FakeProfileRepository(listOf(profile(95, ProfileKind.DUAL_MESSENGER), profile(10, ProfileKind.WORK)))
    private val repo = CloneStateRepository(
        gateway, store, RecordingAudit(), FakeHostPackages(setOf("com.example.alpha")), profiles,
        HostIdentity("com.mohitarora.dualclone.debug"), FakeClock(50_000L)
    )
    private val okReport = ReconcileReport(listOf(95, 10), emptyList(), 0)

    private fun restorer(report: ReconcileReport = okReport, enabled: Boolean = true) =
        BootRestorer({ report }, repo, profiles, InMemorySettings(initialRestore = enabled))

    private fun lost(pkg: String, noticedAt: Long, userId: Int = 95) =
        runBlocking { store.put(StoredClone(pkg, userId, CloneState.LOST, CloneOriginKind.DUALCLONE, noticedAt)) }

    @Test
    fun restoresClonesLostSinceTheBootBroadcast() = runBlocking {
        lost("com.example.alpha", noticedAt = 60_000L)
        gateway.on("pm install-existing --user 95 com.example.alpha", "Package a installed for user: 95")
        gateway.on("pm path --user 95", "package:/x/base.apk")
        assertEquals(BootRestorer.Outcome.DONE, restorer().run(bootBroadcastMillis = 55_000L))
        assertEquals(CloneState.INSTALLED, store.load(95)["com.example.alpha"]?.state)
    }

    @Test
    fun leavesAnOlderLossAloneBecauseTheUserMayHaveRemovedItByHand() = runBlocking {
        lost("com.example.alpha", noticedAt = 10_000L)
        assertEquals(BootRestorer.Outcome.DONE, restorer().run(bootBroadcastMillis = 55_000L))
        assertTrue(gateway.commandsStartingWith("pm install-existing").isEmpty())
        assertEquals(CloneState.LOST, store.load(95)["com.example.alpha"]?.state)
    }

    @Test
    fun doesNothingWhenTheSettingIsOff() = runBlocking {
        lost("com.example.alpha", noticedAt = 60_000L)
        assertEquals(BootRestorer.Outcome.DISABLED, restorer(enabled = false).run(55_000L))
        assertTrue(gateway.history.isEmpty())
    }

    @Test
    fun retriesWhileShizukuIsNotReady() = runBlocking {
        lost("com.example.alpha", noticedAt = 60_000L)
        val skipped = ReconcileReport(emptyList(), emptyList(), 0, skippedReason = "Shizuku is not available")
        assertEquals(BootRestorer.Outcome.RETRY, restorer(skipped).run(55_000L))
        assertTrue(gateway.commandsStartingWith("pm install-existing").isEmpty())
    }

    @Test
    fun retriesWhenTheDualMessengerListingFailed() = runBlocking {
        lost("com.example.alpha", noticedAt = 60_000L)
        val failedListing = ReconcileReport(listOf(10), listOf(95), 0)
        assertEquals(BootRestorer.Outcome.RETRY, restorer(failedListing).run(55_000L))
        assertTrue(gateway.commandsStartingWith("pm install-existing").isEmpty())
    }

    @Test
    fun retriesWhenShizukuDropsDuringTheRestore() = runBlocking {
        lost("com.example.alpha", noticedAt = 60_000L)
        gateway.on("pm install-existing --user 95", "", exitCode = CommandExit.UNAVAILABLE)
        assertEquals(BootRestorer.Outcome.RETRY, restorer().run(55_000L))
        assertEquals(CloneState.LOST, store.load(95)["com.example.alpha"]?.state)
    }

    @Test
    fun neverTouchesAWorkProfile() = runBlocking {
        lost("com.example.alpha", noticedAt = 60_000L, userId = 10)
        assertEquals(BootRestorer.Outcome.DONE, restorer().run(55_000L))
        assertTrue(gateway.commandsStartingWith("pm install-existing").isEmpty())
    }
}
