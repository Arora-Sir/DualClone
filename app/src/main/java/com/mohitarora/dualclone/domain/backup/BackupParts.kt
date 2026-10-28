package com.mohitarora.dualclone.domain.backup

/**
 * Which sections a backup file holds. Each flag is one export checkbox.
 * cloneList is the per-profile list of cloned apps, appOpsPresets are the remembered permission overrides,
 * and settings are the theme and the update preference. Secrets and the command log are never part of a backup.
 */
data class BackupParts(
    val cloneList: Boolean = true,
    val appOpsPresets: Boolean = true,
    val settings: Boolean = true
) {
    /** A backup needs at least one section. Export refuses an empty selection. */
    val isValid: Boolean get() = cloneList || appOpsPresets || settings

    /** Header names written to the file, for example CLONE_LIST. */
    fun names(): List<String> = buildList {
        if (cloneList) add(CLONE_LIST)
        if (appOpsPresets) add(APPOPS_PRESETS)
        if (settings) add(SETTINGS)
    }

    companion object {
        const val CLONE_LIST = "CLONE_LIST"
        const val APPOPS_PRESETS = "APPOPS_PRESETS"
        const val SETTINGS = "SETTINGS"

        val EVERYTHING = BackupParts()

        /** Clone list plus the app ops presets, no theme or update settings. */
        val CLONE_LIST_ONLY = BackupParts(cloneList = true, appOpsPresets = true, settings = false)

        /** Theme and update settings only. */
        val SETTINGS_ONLY = BackupParts(cloneList = false, appOpsPresets = false, settings = true)

        fun fromNames(names: List<*>): BackupParts = BackupParts(
            cloneList = CLONE_LIST in names,
            appOpsPresets = APPOPS_PRESETS in names,
            settings = SETTINGS in names
        )
    }
}

/** Update preference stored with the settings part. */
data class BackupUpdateSettings(val dailyCheck: Boolean)
