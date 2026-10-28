package com.mohitarora.dualclone.data

import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.domain.model.CloneOriginKind
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.pm.CommandExit
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.domain.repo.CommandOutput
import com.mohitarora.dualclone.shizuku.FakeShizukuGateway
import com.mohitarora.dualclone.testing.FakeClock
import com.mohitarora.dualclone.testing.FakeHostPackages
import com.mohitarora.dualclone.testing.FakeProfileRepository
import com.mohitarora.dualclone.testing.InMemoryCloneStateStore
import com.mohitarora.dualclone.testing.RecordingAudit
import com.mohitarora.dualclone.testing.profile
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CloneStateRepositoryTest {

    private val gateway = FakeShizukuGateway()
    private val store = InMemoryCloneStateStore()
    private val audit = RecordingAudit()
    private val clock = FakeClock(5_000L)
    private val host = HostIdentity("com.mohitarora.dualclone.debug")
    private val hostPackages = FakeHostPackages(setOf("com.whatsapp", "com.example.alpha", "com.example.beta"))
    private val profiles = FakeProfileRepository(
        listOf(profile(95, ProfileKind.DUAL_MESSENGER), profile(10, ProfileKind.WORK), profile(150, ProfileKind.SECURE_FOLDER))
    )
    private val repo = CloneStateRepository(gateway, store, audit, hostPackages, profiles, host, clock)

    private fun packages(vararg names: String) = names.joinToString("\n") { "package:$it" }

    private fun deviceHas(userId: Int, vararg names: String) {
        gateway.on("pm list packages --user $userId", packages(*names))
        gateway.on("pm list packages -u --user $userId", packages(*names))
    }

    private fun clones(userId: Int) = runBlocking { repo.clonesFor(userId).first() }

    @Test
    fun reconcileStoresTheListedPackages() = runBlocking {
        deviceHas(95, "com.android.settings", "com.whatsapp")
        val result = repo.reconcile(95)
        assertEquals(ListResult.Loaded(2), result)
        assertEquals(CloneState.INSTALLED, clones(95)["com.whatsapp"]?.state)
        assertEquals(5_000L, clones(95)["com.whatsapp"]?.verifiedAtMillis)
        assertTrue(clones(95)["com.whatsapp"]!!.hostInstalled)
        assertFalse(clones(95)["com.android.settings"]!!.hostInstalled)
    }

    @Test
    fun failedListNeverOverwritesStateAndMarksItStale() = runBlocking {
        deviceHas(95, "com.android.settings", "com.whatsapp")
        repo.reconcile(95)

        gateway.on("pm list packages --user 95", "", exitCode = 1, stderr = "boom")
        val failed = repo.reconcile(95)
        assertTrue(failed is ListResult.Unavailable)
        assertEquals(setOf("com.android.settings", "com.whatsapp"), clones(95).keys)
        assertEquals(setOf(95), repo.staleUsers.value)

        deviceHas(95, "com.android.settings", "com.whatsapp")
        repo.reconcile(95)
        assertTrue(repo.staleUsers.value.isEmpty())
    }

    @Test
    fun emptySuccessfulListIsTreatedAsFailure() = runBlocking {
        deviceHas(95, "com.android.settings", "com.whatsapp")
        repo.reconcile(95)
        gateway.on("pm list packages --user 95", "", exitCode = 0)
        assertTrue(repo.reconcile(95) is ListResult.Unavailable)
        assertEquals(2, clones(95).size)
    }

    @Test
    fun timedOutListIsTreatedAsFailure() = runBlocking {
        deviceHas(95, "com.whatsapp")
        repo.reconcile(95)
        gateway.on("pm list packages --user 95", CommandOutput(-1, "package:com.other.thing", "", timedOut = true))
        assertTrue(repo.reconcile(95) is ListResult.Unavailable)
        assertEquals(setOf("com.whatsapp"), clones(95).keys)
    }

    @Test
    fun staleSnapshotCannotOverwriteANewerOperation() = runBlocking {
        gateway.on("pm path --user 95 com.example.alpha", "package:/data/app/alpha/base.apk")
        gateway.on("pm install-existing --user 95", "Package com.example.alpha installed for user: 95")
        // NOTE: The device keeps reporting an old list that lacks the clone, as a lagging package service might.
        deviceHas(95, "com.android.settings")
        // NOTE: A mutation lands during every listing, so every snapshot is stale and none may be applied.
        gateway.beforeEach = { cmd ->
            if (cmd == "pm list packages --user 95") {
                assertTrue(repo.clone("com.example.alpha", 95) is OpResult.Success)
            }
        }
        repo.reconcile(95)
        assertEquals(0, store.replaceCount)
        assertEquals(CloneState.INSTALLED, clones(95)["com.example.alpha"]?.state)
        assertEquals(CloneOriginKind.DUALCLONE, store.load(95)["com.example.alpha"]?.origin)
    }

    @Test
    fun concurrentOperationsOnOneUserAreSerialized() = runBlocking {
        gateway.on("pm install-existing --user 95", "Package x installed for user: 95")
        gateway.onDynamic("pm path --user 95") { CommandOutput(0, "package:/data/app/x/base.apk", "") }
        val active = java.util.concurrent.atomic.AtomicInteger()
        var maxActive = 0
        gateway.beforeEach = { cmd ->
            if (cmd.startsWith("pm install-existing")) {
                maxActive = maxOf(maxActive, active.incrementAndGet())
                kotlinx.coroutines.yield()
                active.decrementAndGet()
            }
        }
        val results = listOf("com.example.alpha", "com.example.beta", "com.whatsapp").map { pkg ->
            async { repo.clone(pkg, 95) }
        }.awaitAll()
        assertTrue(results.all { it is OpResult.Success })
        assertEquals(1, maxActive)
        assertEquals(3, clones(95).size)
    }

    @Test
    fun cloneRunsInstallThenVerifiesWithPmPath() = runBlocking {
        gateway.on("pm install-existing --user 95 com.whatsapp", "Package com.whatsapp installed for user: 95")
        gateway.on("pm path --user 95 com.whatsapp", "package:/data/app/wa/base.apk")
        val result = repo.clone("com.whatsapp", 95)
        assertEquals(OpResult.Success(verified = true), result)
        assertEquals(
            listOf("pm install-existing --user 95 com.whatsapp", "pm path --user 95 com.whatsapp"),
            gateway.history
        )
        assertEquals(listOf(95, 95), audit.rows.map { it.userId })
        assertEquals(CloneState.INSTALLED, clones(95)["com.whatsapp"]?.state)
    }

    @Test
    fun cloneThatCannotBeVerifiedDoesNotFlipState() = runBlocking {
        gateway.on("pm install-existing --user 95", "Package com.whatsapp installed for user: 95")
        gateway.on("pm path --user 95", "", exitCode = 1)
        val result = repo.clone("com.whatsapp", 95)
        assertTrue(result is OpResult.Failure)
        assertNull(clones(95)["com.whatsapp"])
    }

    @Test
    fun failedInstallReturnsParsedErrorCode() = runBlocking {
        gateway.on("pm install-existing", "", exitCode = 1, stderr = "Failure [INSTALL_FAILED_USER_RESTRICTED]")
        val result = repo.clone("com.whatsapp", 95) as OpResult.Failure
        assertEquals(PmErrorCode.USER_RESTRICTED, result.code)
        assertEquals(1, audit.rows.size)
        assertFalse(audit.rows.single().success)
    }

    @Test
    fun removeVerifiesThenDropsStateAndOffersLauncherRefresh() = runBlocking {
        deviceHas(10, "com.android.settings", "com.whatsapp")
        repo.reconcile(10)
        gateway.on("pm uninstall --user 10 com.whatsapp", "Success")
        gateway.on("pm path --user 10 com.whatsapp", "", exitCode = 1)
        val result = repo.remove("com.whatsapp", 10)
        assertEquals(OpResult.Success(verified = true), result)
        assertNull(clones(10)["com.whatsapp"])
        assertTrue(repo.launcherRefreshHint.value)
    }

    @Test
    fun removeKeepsStateWhenPackageIsStillPresent() = runBlocking {
        deviceHas(10, "com.android.settings", "com.whatsapp")
        repo.reconcile(10)
        gateway.on("pm uninstall --user 10", "Success")
        gateway.on("pm path --user 10 com.whatsapp", "package:/data/app/wa/base.apk")
        assertTrue(repo.remove("com.whatsapp", 10) is OpResult.Failure)
        assertNotNull(clones(10)["com.whatsapp"])
        assertFalse(repo.launcherRefreshHint.value)
    }

    @Test
    fun removeOfAnAlreadyMissingPackageHealsState() = runBlocking {
        deviceHas(10, "com.android.settings", "com.whatsapp")
        repo.reconcile(10)
        gateway.on("pm uninstall --user 10", "Failure [not installed for 10]", exitCode = 1)
        gateway.on("pm path --user 10", "", exitCode = 1)
        assertTrue(repo.remove("com.whatsapp", 10) is OpResult.Success)
        assertNull(clones(10)["com.whatsapp"])
    }

    @Test
    fun secureFolderNeverReceivesAShellCommand() = runBlocking {
        assertTrue(repo.clone("com.whatsapp", 150) is OpResult.Failure)
        assertTrue(repo.remove("com.whatsapp", 150) is OpResult.Failure)
        assertTrue(repo.reconcile(150) is ListResult.Unavailable)
        assertTrue(repo.cloneAll(listOf("com.whatsapp"), 150).all { it.result is OpResult.Failure })
        assertTrue(gateway.history.isEmpty())
    }

    @Test
    fun hostAppCanNeverBeCloned() = runBlocking {
        val result = repo.clone("com.mohitarora.dualclone", 95)
        assertEquals(PmErrorCode.USER_RESTRICTED, (result as OpResult.Failure).code)
        assertTrue(repo.clone("com.mohitarora.dualclone.debug", 10) is OpResult.Failure)
        assertTrue(gateway.history.isEmpty())
    }

    @Test
    fun hostCopyInAProfileIsHiddenFromListsButDetected() = runBlocking {
        deviceHas(10, "com.android.settings", "com.mohitarora.dualclone.debug")
        repo.reconcile(10)
        assertNull(clones(10)["com.mohitarora.dualclone.debug"])
        assertEquals(listOf(10 to "com.mohitarora.dualclone.debug"), repo.hostCopies().first())

        gateway.on("pm uninstall --user 10 com.mohitarora.dualclone.debug", "Success")
        gateway.on("pm path --user 10", "", exitCode = 1)
        assertTrue(repo.remove("com.mohitarora.dualclone.debug", 10) is OpResult.Success)
        assertTrue(repo.hostCopies().first().isEmpty())
    }

    @Test
    fun bulkReturnsOneResultPerPackageAndStopsWhenShizukuDisappears() = runBlocking {
        gateway.on("pm install-existing --user 95 com.example.alpha", "Package a installed for user: 95")
        gateway.on("pm path --user 95", "package:/x/base.apk")
        gateway.on("pm install-existing --user 95 com.example.beta", "", exitCode = CommandExit.UNAVAILABLE)
        val results = repo.cloneAll(listOf("com.example.alpha", "com.example.beta", "com.whatsapp", "com.example.alpha"), 95)
        assertEquals(listOf("com.example.alpha", "com.example.beta", "com.whatsapp"), results.map { it.packageName })
        assertTrue(results[0].result is OpResult.Success)
        assertEquals(OpResult.ShizukuUnavailable, results[1].result)
        assertEquals(OpResult.ShizukuUnavailable, results[2].result)
        assertEquals(2, gateway.commandsStartingWith("pm install-existing").size)
    }

    @Test
    fun hiddenPackagesAreClassifiedByProbingKnownAmbiguousOnes() = runBlocking {
        deviceHas(10, "com.android.settings", "com.whatsapp")
        repo.reconcile(10)
        gateway.on("pm list packages --user 10", packages("com.android.settings"))
        gateway.on("pm list packages -u --user 10", packages("com.android.settings", "com.whatsapp", "com.never.seen"))
        gateway.on("dumpsys package com.whatsapp", com.mohitarora.dualclone.testing.Fixtures.DUMPSYS_USER_LINES)
        repo.reconcile(10)
        assertEquals(CloneState.HIDDEN, clones(10)["com.whatsapp"]?.state)
        assertNull(clones(10)["com.never.seen"])
        assertTrue(gateway.commandsStartingWith("dumpsys package com.never.seen").isEmpty())
    }

    @Test
    fun launcherRefreshRunsOnlyWhenTheSamsungLauncherExists() = runBlocking {
        gateway.on("pm path com.sec.android.app.launcher", "", exitCode = 1)
        assertFalse(repo.refreshLauncher())
        assertTrue(gateway.commandsStartingWith("am force-stop").isEmpty())

        gateway.on("pm path com.sec.android.app.launcher", "package:/system/priv-app/TouchWizHome.apk")
        gateway.on("am force-stop com.sec.android.app.launcher", "")
        assertTrue(repo.refreshLauncher())
        assertEquals(1, gateway.commandsStartingWith("am force-stop com.sec.android.app.launcher").size)
    }

    @Test
    fun invalidPackageNamesAreRejectedBeforeAnyCommand() = runBlocking {
        assertTrue(repo.clone("com.x; rm -rf /", 95) is OpResult.Failure)
        assertTrue(repo.remove("not a package", 95) is OpResult.Failure)
        assertTrue(gateway.history.isEmpty())
    }
}
