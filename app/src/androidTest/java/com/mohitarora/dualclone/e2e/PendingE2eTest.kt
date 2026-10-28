package com.mohitarora.dualclone.e2e

import com.mohitarora.dualclone.domain.repo.CatalogRepository
import com.mohitarora.dualclone.data.backup.BackupRepository
import com.mohitarora.dualclone.data.backup.PreviewResult
import com.mohitarora.dualclone.data.repository.OrphanRepository
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject

@HiltAndroidTest
class OrphanSweepE2eTest : E2eBase() {
    @Inject lateinit var orphans: OrphanRepository

    // NOTE: A package that still has a host app in User 0 must never be reported as an orphan.
    // NOTE: The positive case needs a second throwaway APK installed only in the profile and is not covered here.
    @Test
    fun cloned_package_with_a_host_is_not_an_orphan() = runBlocking {
        assertTrue(cloneRepository.clone(throwawayPackage, managedUserId) is OpResult.Success)
        val result = orphans.detect(managedUserId)
        assertTrue("detect returned $result", result is ListResult.Loaded)
        val found = (result as ListResult.Loaded).value.map { it.packageName }
        assertFalse("$throwawayPackage has a host and must not be an orphan", throwawayPackage in found)
    }
}

@HiltAndroidTest
class BackupRoundTripE2eTest : E2eBase() {
    @Inject lateinit var backup: BackupRepository
    @Inject lateinit var catalog: CatalogRepository

    @Test
    fun export_remove_import_restores_clone() = runBlocking {
        // NOTE: The host app list comes from the catalog database, which the app fills at startup. A test has to fill it itself.
        catalog.refresh()
        assertTrue(cloneRepository.clone(throwawayPackage, managedUserId) is OpResult.Success)
        cloneRepository.reconcile(managedUserId)
        awaitManagedProfile()
        val file = ByteArrayOutputStream().also { backup.export(it, password = null) }.toByteArray()

        assertTrue(cloneRepository.remove(throwawayPackage, managedUserId) is OpResult.Success)
        assertFalse(throwawayPackage in packagesOf(managedUserId))

        val preview = backup.preview(ByteArrayInputStream(file), password = null)
        assertTrue("preview was $preview", preview is PreviewResult.Ready)
        val plan = (preview as PreviewResult.Ready).plan
        assertTrue("backup must list the clone", plan.clones.any { it.packageName == throwawayPackage })

        backup.apply(plan)
        assertTrue("clone must be back after restore", throwawayPackage in packagesOf(managedUserId))
    }
}
