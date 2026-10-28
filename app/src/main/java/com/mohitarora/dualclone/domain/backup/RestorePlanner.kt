package com.mohitarora.dualclone.domain.backup

import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.policy.ProfilePolicy

enum class SkipReason { NOT_INSTALLED_IN_HOST, NO_MATCHING_PROFILE, PROFILE_RESTRICTED }

data class PlanClone(val packageName: String, val targetUserId: Int, val include: Boolean = true)

data class PlanPreset(val preset: BackupPreset, val targetUserId: Int, val include: Boolean = true)

data class SkippedItem(val label: String, val reason: SkipReason)

/** Dry run result shown before anything is applied. Each item carries a checkbox state. */
data class RestorePlan(
    val clones: List<PlanClone>,
    val presets: List<PlanPreset>,
    val skipped: List<SkippedItem>,
    val theme: BackupTheme?,
    val applyTheme: Boolean = theme != null,
    val updateSettings: BackupUpdateSettings? = null,
    val applyUpdateSettings: Boolean = updateSettings != null,
    /** Sections the file holds. The UI disables the checkbox of every absent section. */
    val parts: BackupParts = BackupParts.EVERYTHING
) {
    val hasCloneList: Boolean get() = parts.cloneList
    val hasAppOpsPresets: Boolean get() = parts.appOpsPresets
    val hasSettings: Boolean get() = parts.settings
}

/** Maps a backup onto the profiles of this device. Profile ids differ between devices, so kinds are matched. */
object RestorePlanner {

    fun plan(model: BackupModel, hostPackages: Set<String>, targets: List<Profile>): RestorePlan {
        val clones = ArrayList<PlanClone>()
        val skipped = ArrayList<SkippedItem>()

        fun targetFor(kind: ProfileKind, userId: Int): Profile? {
            val sameKind = targets.filter { it.kind == kind }
            return sameKind.firstOrNull { it.userId == userId } ?: sameKind.firstOrNull()
        }

        for (profile in if (model.parts.cloneList) model.profiles else emptyList()) {
            val target = targetFor(profile.kind, profile.userId)
            if (target == null) {
                profile.clones.forEach { skipped.add(SkippedItem(it, SkipReason.NO_MATCHING_PROFILE)) }
                continue
            }
            if (!ProfilePolicy.canRunShell(target)) {
                profile.clones.forEach { skipped.add(SkippedItem(it, SkipReason.PROFILE_RESTRICTED)) }
                continue
            }
            for (pkg in profile.clones.distinct()) {
                if (pkg in hostPackages) clones.add(PlanClone(pkg, target.userId))
                else skipped.add(SkippedItem(pkg, SkipReason.NOT_INSTALLED_IN_HOST))
            }
        }

        val presets = ArrayList<PlanPreset>()
        for (p in if (model.parts.appOpsPresets) model.appOpsPresets else emptyList()) {
            val target = targetFor(p.kind, p.userId)
            when {
                target == null -> skipped.add(SkippedItem("${p.packageName} ${p.op}", SkipReason.NO_MATCHING_PROFILE))
                !ProfilePolicy.canRunShell(target) -> skipped.add(SkippedItem("${p.packageName} ${p.op}", SkipReason.PROFILE_RESTRICTED))
                p.packageName !in hostPackages -> skipped.add(SkippedItem("${p.packageName} ${p.op}", SkipReason.NOT_INSTALLED_IN_HOST))
                else -> presets.add(PlanPreset(p, target.userId))
            }
        }
        return RestorePlan(
            clones = clones, presets = presets, skipped = skipped,
            theme = if (model.parts.settings) model.theme else null,
            updateSettings = if (model.parts.settings) model.updateSettings else null,
            parts = model.parts
        )
    }
}
