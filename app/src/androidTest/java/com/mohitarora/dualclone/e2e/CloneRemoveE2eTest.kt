package com.mohitarora.dualclone.e2e

import com.mohitarora.dualclone.domain.model.OpResult
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@HiltAndroidTest
class CloneRemoveE2eTest : E2eBase() {

    @Test
    fun clone_then_verified_remove() = runBlocking {
        val cloned = cloneRepository.clone(throwawayPackage, managedUserId)
        assertTrue("clone result was $cloned", cloned is OpResult.Success)
        assertTrue("package must be listed in the profile", throwawayPackage in packagesOf(managedUserId))
        assertTrue("pm path must resolve", sh("pm", "path", "--user", "$managedUserId", throwawayPackage).stdout.contains("package:"))

        val removed = cloneRepository.remove(throwawayPackage, managedUserId)
        assertTrue("remove result was $removed", removed is OpResult.Success && removed.verified)
        assertFalse("package must be gone after remove", throwawayPackage in packagesOf(managedUserId))
        assertTrue("host app must be untouched", throwawayPackage in packagesOf(0))
    }

    @Test
    fun removing_a_package_that_is_not_cloned_does_not_touch_user_zero() = runBlocking {
        cloneRepository.remove(throwawayPackage, managedUserId)
        assertTrue(throwawayPackage in packagesOf(0))
    }

    @Test
    fun user_zero_is_never_a_clone_target() = runBlocking {
        val result = cloneRepository.clone(throwawayPackage, 0)
        assertFalse("cloning into User 0 must not succeed, got $result", result is OpResult.Success && result.verified)
    }

    @Test
    fun managed_profile_is_listed_as_a_target() = runBlocking {
        awaitManagedProfile()
        val profiles = profileRepository.profiles.value
        val managed = profiles.firstOrNull { it.userId == managedUserId }
        assertTrue("managed user $managedUserId missing from ${profiles.map { it.userId }}", managed != null)
        assertFalse(managed!!.isPaused)
    }
}
