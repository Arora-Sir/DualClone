package com.mohitarora.dualclone.e2e

import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.WorkManager
import com.mohitarora.dualclone.domain.repo.CloneRepository
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import dagger.hilt.android.testing.HiltAndroidRule
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import javax.inject.Inject

/**
 * Shared setup for emulator tests. The CI script creates a managed profile and passes its id as the managedUserId argument.
 * The throwaway package is the test APK itself. It is installed for User 0, is not a system app and can never be the host app,
 * so cloning and removing it is safe.
 */
abstract class E2eBase {
    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var gateway: ShizukuGateway
    @Inject lateinit var cloneRepository: CloneRepository
    @Inject lateinit var profileRepository: ProfileRepository

    protected val throwawayPackage: String = InstrumentationRegistry.getInstrumentation().context.packageName
    protected var managedUserId: Int = -1

    @Before
    fun setUpProfile() {
        hilt.inject()
        // NOTE: The real app configures WorkManager in its Application class. The Hilt test application does not, and a catalog refresh schedules work.
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        runCatching { WorkManager.getInstance(context) }.onFailure {
            WorkManager.initialize(context, Configuration.Builder().build())
        }
        val fromArgs = InstrumentationRegistry.getArguments().getString("managedUserId")?.toIntOrNull()
        managedUserId = fromArgs ?: runBlocking { findManagedUser() }
        assumeTrue("No managed profile available. Run scripts/ci/run_e2e.sh", managedUserId > 0)
        runBlocking { sh("pm", "uninstall", "--user", "$managedUserId", throwawayPackage) }
    }

    @After
    fun cleanUp() {
        if (managedUserId > 0) runBlocking { sh("pm", "uninstall", "--user", "$managedUserId", throwawayPackage) }
    }

    protected suspend fun sh(vararg command: String) = gateway.run(*command)

    /**
     * Refreshes the profile list and waits until the managed profile is in it.
     * The list is derived on a background scope, so reading it straight after refresh() races the update.
     */
    protected suspend fun awaitManagedProfile() {
        profileRepository.refresh()
        withTimeout(10_000) { profileRepository.profiles.first { list -> list.any { it.userId == managedUserId } } }
    }

    protected suspend fun packagesOf(userId: Int): Set<String> =
        sh("pm", "list", "packages", "--user", "$userId").stdout.lineSequence()
            .filter { it.startsWith("package:") }.map { it.removePrefix("package:").trim() }.toSet()

    private suspend fun findManagedUser(): Int {
        val users = sh("pm", "list", "users").stdout
        return Regex("""UserInfo\{(\d+):[^:]*:([0-9a-fA-F]+)\}""").findAll(users)
            .map { it.groupValues[1].toInt() to it.groupValues[2].toInt(16) }
            .firstOrNull { (id, flags) -> id != 0 && (flags and 0x20) != 0 }?.first ?: -1
    }
}
