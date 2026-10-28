package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.AppOpsPresetStore
import com.mohitarora.dualclone.domain.repo.AppOpsRepository
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import com.mohitarora.dualclone.domain.repo.Clock
import com.mohitarora.dualclone.domain.repo.CloneRepository
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ReconcileStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

data class ReconcileReport(
    val reconciledUsers: List<Int>,
    val failedUsers: List<Int>,
    val presetsReapplied: Int,
    val skippedReason: String? = null
) {
    val ok: Boolean get() = skippedReason == null && failedUsers.isEmpty()
}

/**
 * Brings the app back in line with the device. Used by the periodic worker, by package events and by pull to refresh.
 * Runs are serialized so an event burst never starts overlapping reconciles.
 */
class Reconciler(
    private val catalog: CatalogRepository,
    private val profiles: ProfileRepository,
    private val clones: CloneStateRepository,
    private val appOps: AppOpsRepository,
    private val presets: AppOpsPresetStore,
    private val settings: AppSettings,
    private val clock: Clock
) {
    private val runLock = Mutex()

    suspend fun run(): ReconcileReport = runLock.withLock {
        profiles.refresh()
        val targets = profiles.profiles.value
        if (targets.isEmpty()) {
            return finish(ReconcileReport(emptyList(), emptyList(), 0, "No profiles found or Shizuku is not available"))
        }
        catalog.refresh()
        clones.pruneUsers(targets.map { it.userId }.toSet() + 0)

        val done = ArrayList<Int>()
        val failed = ArrayList<Int>()
        for (profile in targets.filter { ProfilePolicy.canRunShell(it) && !it.isPaused }) {
            when (val r = clones.reconcile(profile.userId)) {
                is ListResult.Loaded -> done.add(profile.userId)
                is ListResult.Unavailable -> {
                    Timber.tag("Reconcile").w("User %d not reconciled: %s", profile.userId, r.reason)
                    failed.add(profile.userId)
                }
            }
        }
        val reapplied = reapplyPresets(done)
        finish(ReconcileReport(done, failed, reapplied))
    }

    /** Re-applies remembered permission overrides whose live value drifted, for example after the host app updated. */
    private suspend fun reapplyPresets(reconciledUsers: List<Int>): Int {
        var count = 0
        for (preset in presets.all().filter { it.userId in reconciledUsers }) {
            val installed = clones.clonesFor(preset.userId).first()[preset.packageName]?.state == CloneState.INSTALLED
            if (!installed) continue
            val current = (appOps.get(preset.packageName, preset.op, preset.userId) as? ListResult.Loaded)?.value ?: continue
            if (current != preset.mode && appOps.set(preset.packageName, preset.op, preset.mode, preset.userId) is OpResult.Success) {
                count++
            }
        }
        return count
    }

    private fun finish(report: ReconcileReport): ReconcileReport {
        val detail = report.skippedReason
            ?: "Checked ${report.reconciledUsers.size} profiles, re-applied ${report.presetsReapplied} permission presets" +
            if (report.failedUsers.isEmpty()) "" else ", ${report.failedUsers.size} profiles could not be read"
        settings.setLastReconcile(ReconcileStatus(clock.now(), report.ok, detail))
        return report
    }
}
