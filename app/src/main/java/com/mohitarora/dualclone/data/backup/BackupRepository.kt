package com.mohitarora.dualclone.data.backup

import com.mohitarora.dualclone.data.repository.AppOpsRepositoryImpl
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.ThemeRepository
import com.mohitarora.dualclone.domain.backup.BackupCodec
import com.mohitarora.dualclone.domain.backup.BackupModel
import com.mohitarora.dualclone.domain.backup.BackupParts
import com.mohitarora.dualclone.domain.backup.BackupUpdateSettings
import com.mohitarora.dualclone.domain.backup.BackupPreset
import com.mohitarora.dualclone.domain.backup.BackupProfile
import com.mohitarora.dualclone.domain.backup.BackupReadResult
import com.mohitarora.dualclone.domain.backup.BackupTheme
import com.mohitarora.dualclone.domain.backup.RestorePlan
import com.mohitarora.dualclone.domain.backup.RestorePlanner
import com.mohitarora.dualclone.domain.model.BulkItemResult
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.AppOpsPresetStore
import com.mohitarora.dualclone.domain.repo.Clock
import com.mohitarora.dualclone.domain.repo.HostPackages
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.ui.theme.ThemeMode
import com.mohitarora.dualclone.ui.theme.ThemePreset
import kotlinx.coroutines.flow.first
import java.io.InputStream
import java.io.OutputStream

/** One clone attempt with its target profile, so the same package in two profiles stays distinguishable. */
data class CloneOutcome(val userId: Int, val packageName: String, val result: OpResult)

data class RestoreReport(
    val cloneResults: List<BulkItemResult>,
    val presetsApplied: Int,
    val presetsFailed: Int,
    val themeApplied: Boolean,
    val cloneOutcomes: List<CloneOutcome> = emptyList(),
    val updateSettingsApplied: Boolean = false
)

/** Exports and restores the clone list, remembered permissions and theme. App data is never touched. */
class BackupRepository(
    private val profiles: ProfileRepository,
    private val clones: CloneStateRepository,
    private val hostPackages: HostPackages,
    private val presets: AppOpsPresetStore,
    private val appOps: AppOpsRepositoryImpl,
    private val theme: ThemeRepository,
    private val clock: Clock,
    private val appVersion: String,
    private val dailyUpdateCheck: () -> Boolean = { false },
    private val setDailyUpdateCheck: (Boolean) -> Unit = {}
) {

    /** Writes only the chosen sections. At least one part is required. */
    suspend fun export(out: OutputStream, password: CharArray?, parts: BackupParts = BackupParts()) {
        require(parts.isValid) { "Choose at least one backup section" }
        val targets = profiles.profiles.value.filter { ProfilePolicy.canRunShell(it) }
        val kinds = targets.associate { it.userId to it.kind }
        val backupProfiles = if (!parts.cloneList) emptyList() else targets.map { p ->
            val installed = clones.clonesFor(p.userId).first()
                .values.filter { it.state == CloneState.INSTALLED && it.hostInstalled && !it.isSystemInProfile && !it.isProfileOwner }
                .map { it.packageName }.sorted()
            BackupProfile(p.userId, p.kind, installed)
        }.filter { it.clones.isNotEmpty() }
        val backupPresets = if (!parts.appOpsPresets) emptyList() else presets.all().mapNotNull { p ->
            kinds[p.userId]?.let { BackupPreset(it, p.userId, p.packageName, p.op, p.mode) }
        }
        val t = theme.config.value
        BackupCodec.write(
            BackupModel(
                createdAtMillis = clock.now(),
                appVersion = appVersion,
                profiles = backupProfiles,
                appOpsPresets = backupPresets,
                theme = if (parts.settings) BackupTheme(t.preset.name, t.isPureBlack, t.customColorHex, t.mode.name, t.surfaceLift) else null,
                updateSettings = if (parts.settings) BackupUpdateSettings(dailyUpdateCheck()) else null,
                parts = parts
            ),
            out,
            password
        )
    }

    /** Reads a file and builds the dry run. Nothing on the device changes. */
    suspend fun preview(input: InputStream, password: CharArray?): PreviewResult =
        when (val r = BackupCodec.read(input, password)) {
            is BackupReadResult.Ok -> PreviewResult.Ready(
                RestorePlanner.plan(r.model, hostPackages.snapshot(), profiles.profiles.value)
            )
            BackupReadResult.NeedsPassword -> PreviewResult.NeedsPassword
            BackupReadResult.WrongPasswordOrCorrupt -> PreviewResult.WrongPassword
            is BackupReadResult.Invalid -> PreviewResult.Invalid(r.reason)
        }

    /** Applies only the items whose include flag is true. */
    suspend fun apply(plan: RestorePlan): RestoreReport {
        val results = ArrayList<BulkItemResult>()
        val outcomes = ArrayList<CloneOutcome>()
        plan.clones.filter { it.include }.groupBy { it.targetUserId }.forEach { (userId, items) ->
            val batch = clones.cloneAll(items.map { it.packageName }, userId)
            results += batch
            batch.forEach { outcomes += CloneOutcome(userId, it.packageName, it.result) }
        }
        var ok = 0
        var failed = 0
        for (item in plan.presets.filter { it.include }) {
            val p = item.preset
            val r = appOps.setRemembered(p.packageName, p.op, p.mode, item.targetUserId, remember = true)
            if (r is OpResult.Success) ok++ else failed++
        }
        val u = plan.updateSettings
        if (plan.applyUpdateSettings && u != null) setDailyUpdateCheck(u.dailyCheck)
        var themeApplied = false
        val t = plan.theme
        if (plan.applyTheme && t != null) {
            ThemePreset.entries.firstOrNull { it.name == t.preset }?.let { theme.setPreset(it) }
            if (t.preset == ThemePreset.CUSTOM.name && t.customColorHex != 0L) theme.setCustomColor(t.customColorHex)
            theme.setPureBlack(t.pureBlack)
            theme.setSurfaceLift(t.surfaceLift)
            ThemeMode.entries.firstOrNull { it.name == t.mode }?.let { theme.setMode(it) }
            themeApplied = true
        }
        val updateApplied = plan.applyUpdateSettings && u != null
        return RestoreReport(results, ok, failed, themeApplied, outcomes, updateApplied)
    }
}

sealed interface PreviewResult {
    data class Ready(val plan: RestorePlan) : PreviewResult
    data object NeedsPassword : PreviewResult
    data object WrongPassword : PreviewResult
    data class Invalid(val reason: String) : PreviewResult
}
