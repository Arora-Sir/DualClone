package com.mohitarora.dualclone.data

import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.OrphanRepository
import com.mohitarora.dualclone.data.repository.ProfileRepositoryImpl
import com.mohitarora.dualclone.domain.model.CloneOriginKind
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.model.StoredClone
import com.mohitarora.dualclone.domain.policy.HostIdentity
import com.mohitarora.dualclone.shizuku.FakeShizukuGateway
import com.mohitarora.dualclone.testing.FakeClock
import com.mohitarora.dualclone.testing.FakeHostPackages
import com.mohitarora.dualclone.testing.FakeProfileRepository
import com.mohitarora.dualclone.testing.Fixtures
import com.mohitarora.dualclone.testing.InMemoryCloneStateStore
import com.mohitarora.dualclone.testing.InMemorySettings
import com.mohitarora.dualclone.testing.RecordingAudit
import com.mohitarora.dualclone.testing.profile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OrphanAndProfileTest {

    private val gateway = FakeShizukuGateway()
    private val store = InMemoryCloneStateStore()
    private val profiles = FakeProfileRepository(listOf(profile(10, ProfileKind.WORK), profile(150, ProfileKind.SECURE_FOLDER)))
    private val host = HostIdentity("com.mohitarora.dualclone")
    private val clones = CloneStateRepository(gateway, store, RecordingAudit(), FakeHostPackages(), profiles, host, FakeClock())
    private val orphans = OrphanRepository(gateway, store, profiles, clones, host)

    @Test
    fun orphanIsAThirdPartyPackageInTheProfileWithNoHostApp() = runBlocking {
        gateway.on("pm list packages -3 --user 10", "package:com.whatsapp\npackage:com.gone.clone\npackage:com.oasisfeng.island\npackage:com.native.only\npackage:com.mohitarora.dualclone")
        gateway.on("pm list packages --user 0", "package:com.whatsapp\npackage:com.android.settings")
        gateway.on("dpm list-owners", Fixtures.DPM_OWNERS)
        store.put(StoredClone("com.gone.clone", 10, CloneState.INSTALLED, CloneOriginKind.DUALCLONE, 77L))

        val found = (orphans.detect(10) as ListResult.Loaded).value
        assertEquals(listOf("com.gone.clone", "com.native.only"), found.map { it.packageName })
        val ours = found.first { it.packageName == "com.gone.clone" }
        val native = found.first { it.packageName == "com.native.only" }
        assertFalse(ours.onlyInProfile)
        assertTrue(ours.selectedByDefault)
        assertEquals(77L, ours.lastVerifiedMillis)
        assertTrue(native.onlyInProfile)
        assertFalse("native apps must never be selected by default", native.selectedByDefault)
    }

    @Test
    fun failedHostListMeansUnavailableNotEveryAppIsAnOrphan() = runBlocking {
        gateway.on("pm list packages -3 --user 10", "package:com.whatsapp")
        gateway.on("pm list packages --user 0", "", exitCode = 1)
        assertTrue(orphans.detect(10) is ListResult.Unavailable)
    }

    @Test
    fun secureFolderIsNeverScanned() = runBlocking {
        assertTrue(orphans.detect(150) is ListResult.Unavailable)
        assertTrue(gateway.history.isEmpty())
    }

    @Test
    fun purgeUsesTheVerifiedRemovalPath() = runBlocking {
        gateway.on("pm uninstall --user 10 com.gone.clone", "Success")
        gateway.on("pm path --user 10", "", exitCode = 1)
        val results = orphans.purge(listOf("com.gone.clone"), 10)
        assertEquals(1, results.size)
        assertTrue(gateway.history.any { it.startsWith("pm path --user 10") })
    }

    @Test
    fun failedUserListKeepsThePreviousProfiles() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val settings = InMemorySettings()
            val repo = ProfileRepositoryImpl(gateway, settings, scope)
            gateway.on("pm list users", Fixtures.USERS_TABLET)
            repo.refresh()
            assertEquals(listOf(10, 95, 150), repo.profiles.value.map { it.userId })
            assertEquals(95, repo.selected.value?.userId)

            gateway.on("pm list users", "", exitCode = 1, stderr = "binder died")
            repo.refresh()
            assertEquals(listOf(10, 95, 150), repo.profiles.value.map { it.userId })

            settings.setShowAdvancedUsers(true)
            assertEquals(listOf(10, 11, 95, 150), repo.profiles.value.map { it.userId })

            repo.select(10)
            assertEquals(10, repo.selected.value?.userId)
            assertEquals(10, settings.selectedProfileId.value)
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun selectionFallsBackWhenTheChosenProfileDisappears() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        try {
            val settings = InMemorySettings().also { it.setSelectedProfileId(11) }
            val repo = ProfileRepositoryImpl(gateway, settings, scope)
            gateway.on("pm list users", Fixtures.USERS_TABLET)
            repo.refresh()
            assertEquals(95, repo.selected.value?.userId)
        } finally {
            scope.cancel()
        }
    }
}

class SystemAppsInProfileTest {

    private val gateway = FakeShizukuGateway()
    private val store = InMemoryCloneStateStore()
    private val profiles = FakeProfileRepository(listOf(profile(10, ProfileKind.WORK)))
    private val host = HostIdentity("com.mohitarora.dualclone")
    private val repo = CloneStateRepository(gateway, store, RecordingAudit(), FakeHostPackages(setOf("com.whatsapp")), profiles, host, FakeClock())

    private fun device() {
        val all = "package:com.google.android.accessibility\npackage:com.android.settings\npackage:com.whatsapp"
        gateway.on("pm list packages --user 10", all)
        gateway.on("pm list packages -u --user 10", all)
        gateway.on("pm list packages -s --user 10", "package:com.google.android.accessibility\npackage:com.android.settings")
    }

    @Test
    fun profileSystemAppsAreFlaggedAndOnlyRealClonesAreNotSystem() = runBlocking {
        device()
        repo.reconcile(10)
        val map = repo.clonesFor(10).first()
        assertTrue(map.getValue("com.google.android.accessibility").isSystemInProfile)
        assertTrue(map.getValue("com.android.settings").isSystemInProfile)
        assertFalse(map.getValue("com.whatsapp").isSystemInProfile)
    }

    @Test
    fun systemAppsAreNeverRemoved() = runBlocking {
        device()
        repo.reconcile(10)
        val result = repo.remove("com.google.android.accessibility", 10)
        assertTrue(result is com.mohitarora.dualclone.domain.model.OpResult.Failure)
        assertTrue(gateway.commandsStartingWith("pm uninstall").isEmpty())
        assertTrue(repo.removeAll(listOf("com.android.settings"), 10).all { it.result is com.mohitarora.dualclone.domain.model.OpResult.Failure })
    }

    @Test
    fun clonedCountExcludesSystemApps() = runBlocking {
        device()
        repo.reconcile(10)
        val catalog = listOf("com.google.android.accessibility", "com.android.settings", "com.whatsapp").map {
            com.mohitarora.dualclone.domain.model.CatalogApp(it, it, isSystemApp = it != "com.whatsapp", isLaunchable = true)
        }
        val map = repo.clonesFor(10).first()
        val items = catalog.map { com.mohitarora.dualclone.ui.viewmodel.buildAppItem(it, map[it.packageName], false) }
        assertEquals(1, items.count { it.isCloned })
        assertEquals(2, items.count { it.isProfileSystemApp })
        assertTrue(items.single { it.packageName == "com.whatsapp" }.isCloned)
        assertFalse(items.single { it.packageName == "com.android.settings" }.isCloned)
    }

    @Test
    fun failedSystemListKeepsThePreviousClassification() = runBlocking {
        device()
        repo.reconcile(10)
        gateway.on("pm list packages -s --user 10", "", exitCode = 1)
        repo.reconcile(10)
        assertTrue(repo.clonesFor(10).first().getValue("com.android.settings").isSystemInProfile)
    }

    @Test
    fun orphanDetectionNeverReportsSystemPackages() = runBlocking {
        gateway.on("pm list packages -3 --user 10", "package:com.leaked.system\npackage:com.real.orphan")
        gateway.on("pm list packages --user 0", "package:com.whatsapp")
        gateway.on("pm list packages -s --user 10", "package:com.leaked.system")
        val orphans = OrphanRepository(gateway, store, profiles, repo, host)
        val found = (orphans.detect(10) as ListResult.Loaded).value
        assertEquals(listOf("com.real.orphan"), found.map { it.packageName })
        store.put(StoredClone("com.real.orphan", 10, CloneState.INSTALLED, CloneOriginKind.SYSTEM, 1L))
        assertTrue((orphans.detect(10) as ListResult.Loaded).value.isEmpty())
    }
}

class ProfileOwnerTest {

    private val gateway = FakeShizukuGateway()
    private val store = InMemoryCloneStateStore()
    private val profiles = FakeProfileRepository(listOf(profile(10, ProfileKind.WORK), profile(95, ProfileKind.DUAL_MESSENGER)))
    private val host = HostIdentity("com.mohitarora.dualclone")
    private val repo = CloneStateRepository(gateway, store, RecordingAudit(), FakeHostPackages(setOf("com.whatsapp")), profiles, host, FakeClock())

    private fun device() {
        val all = "package:com.oasisfeng.island\npackage:com.custom.dpc\npackage:com.whatsapp"
        gateway.on("pm list packages --user 10", all)
        gateway.on("pm list packages -u --user 10", all)
        gateway.on("pm list packages -s --user 10", "")
        gateway.on("dpm list-owners", "User 10: admin=com.custom.dpc/.Receiver,ManagedProfileOwner(parentUserId=0)\nUser 95: admin=com.other.thing/.R,ManagedProfileOwner(parentUserId=0)\n")
    }

    @Test
    fun ownerFromDpmAndKnownManagersAreFlaggedForThatProfileOnly() = runBlocking {
        device()
        repo.reconcile(10)
        val map = repo.clonesFor(10).first()
        assertTrue(map.getValue("com.oasisfeng.island").isProfileOwner)
        assertTrue(map.getValue("com.custom.dpc").isProfileOwner)
        assertFalse(map.getValue("com.whatsapp").isProfileOwner)
    }

    @Test
    fun ownerNamedForAnotherUserIsNotFlaggedHere() {
        val out = "User 10: admin=com.custom.dpc/.R,ManagedProfileOwner\nUser 95: admin=com.other.thing/.R,ManagedProfileOwner\n"
        assertEquals(setOf("com.custom.dpc"), com.mohitarora.dualclone.domain.pm.PmOutputParser.parseOwnerPackagesForUser(out, 10))
        assertEquals(setOf("com.other.thing"), com.mohitarora.dualclone.domain.pm.PmOutputParser.parseOwnerPackagesForUser(out, 95))
    }

    @Test
    fun ownersAreNeverRemovedEvenBeforeAReconcile() = runBlocking {
        val before = repo.remove("com.oasisfeng.island", 10)
        assertEquals(com.mohitarora.dualclone.domain.model.PmErrorCode.OWNER_BLOCKED, (before as com.mohitarora.dualclone.domain.model.OpResult.Failure).code)
        device()
        repo.reconcile(10)
        assertTrue(repo.remove("com.custom.dpc", 10) is com.mohitarora.dualclone.domain.model.OpResult.Failure)
        assertTrue(repo.removeAll(listOf("com.oasisfeng.island", "net.typeblog.shelter"), 10).all { it.result is com.mohitarora.dualclone.domain.model.OpResult.Failure })
        assertTrue(gateway.commandsStartingWith("pm uninstall").isEmpty())
    }

    @Test
    fun ownersAreNotClonesInTheItemMapping() = runBlocking {
        device()
        repo.reconcile(10)
        val map = repo.clonesFor(10).first()
        val items = listOf("com.oasisfeng.island", "com.custom.dpc", "com.whatsapp").map {
            com.mohitarora.dualclone.ui.viewmodel.buildAppItem(
                com.mohitarora.dualclone.domain.model.CatalogApp(it, it, false, true), map[it], false
            )
        }
        assertEquals(listOf(false, false, true), items.map { it.isCloned })
        assertEquals(listOf(true, true, false), items.map { it.isProfileOwner })
    }

    @Test
    fun failedDpmFallsBackToTheBuiltInList() = runBlocking {
        device()
        gateway.on("dpm list-owners", "", exitCode = 1)
        repo.reconcile(10)
        val map = repo.clonesFor(10).first()
        assertTrue(map.getValue("com.oasisfeng.island").isProfileOwner)
        assertFalse(map.getValue("com.custom.dpc").isProfileOwner)
    }

    @Test
    fun orphanSweepSkipsOwnersAndManagers() = runBlocking {
        gateway.on("pm list packages -3 --user 10", "package:com.oasisfeng.island\npackage:net.typeblog.shelter\npackage:com.custom.dpc\npackage:com.real.orphan")
        gateway.on("pm list packages --user 0", "package:com.whatsapp")
        gateway.on("dpm list-owners", "User 10: admin=com.custom.dpc/.R,ManagedProfileOwner\n")
        val found = (OrphanRepository(gateway, store, profiles, repo, host).detect(10) as ListResult.Loaded).value
        assertEquals(listOf("com.real.orphan"), found.map { it.packageName })
    }
}
