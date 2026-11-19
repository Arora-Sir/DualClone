package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.CloneRepository
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import timber.log.Timber

/**
 * Brings back Dual Messenger clones that Samsung's Dual App agent removed right after the device booted.
 *
 * The agent runs its cleanup when it receives BOOT_COMPLETED and deletes launchable apps from the Dual Messenger profile that are not whitelisted and were not installed through adb.
 * App data goes with them, so a restore brings back an empty app.
 * The run is scheduled a while after boot so it never races the cleanup.
 * It only restores clones first noticed missing after the boot broadcast, so an app removed earlier by hand stays removed.
 */
class BootRestorer(
    private val reconcile: suspend () -> ReconcileReport,
    private val clones: CloneRepository,
    private val profiles: ProfileRepository,
    private val settings: AppSettings
) {
    enum class Outcome { DONE, RETRY, DISABLED }

    suspend fun run(bootBroadcastMillis: Long): Outcome {
        if (!settings.autoRestoreClones.value) return Outcome.DISABLED
        val report = reconcile()
        // NOTE: A skipped run means Shizuku is not ready yet, so the worker tries again later.
        if (report.skippedReason != null) return Outcome.RETRY
        val dualMessenger = profiles.profiles.value.filter { it.kind == ProfileKind.DUAL_MESSENGER && !it.isPaused }
        var retry = false
        for (profile in dualMessenger) {
            // NOTE: Without a fresh listing the store cannot know what the agent removed, so this profile is retried as a whole.
            if (profile.userId in report.failedUsers) {
                retry = true
                continue
            }
            val result = clones.restoreLost(profile.userId, bootBroadcastMillis)
            if (result.shizukuUnavailable) retry = true
            Timber.tag("BootRestore").d("User %d restored=%d failed=%d", profile.userId, result.restored.size, result.failed.size)
        }
        return if (retry) Outcome.RETRY else Outcome.DONE
    }
}
