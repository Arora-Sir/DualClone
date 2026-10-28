package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.runtime.Immutable
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.ui.theme.ThemeConfig
import com.mohitarora.dualclone.update.UpdateState
import com.mohitarora.dualclone.ui.theme.ThemeMode
import com.mohitarora.dualclone.ui.theme.ThemePreset

/** Everything the Settings page renders. View models map their state into this class. */
@Immutable
data class SettingsUiState(
    val theme: ThemeConfig = ThemeConfig(),
    val shizuku: ShizukuState = ShizukuState.Checking,
    val profiles: List<Profile> = emptyList(),
    val isSamsung: Boolean = false,
    val canSetUpDualMessenger: Boolean = false,
    val showAdvancedUsers: Boolean = false,
    val reconcileSummary: String? = null,
    val autoRefreshLauncher: Boolean = false,
    val auditLogCount: Int = 0,
    val update: UpdateState = UpdateState.Idle,
    val currentVersion: String = "",
    val dailyUpdateCheck: Boolean = false
)

/**
 * Callbacks for the Settings page. A null callback hides its row, so rows backed by a feature
 * that is not wired yet never show as dead controls.
 */
@Immutable
class SettingsActions(
    val onBack: () -> Unit,
    val onModeChange: (ThemeMode) -> Unit,
    val onPresetChange: (ThemePreset) -> Unit,
    val onApplyCustomColor: (Long) -> Unit,
    val onPureBlackChange: (Boolean) -> Unit,
    val onEnableDualMessenger: () -> Unit,
    val onClearAuditLog: () -> Unit,
    val onRequestShizukuPermission: () -> Unit,
    val onRefreshShizuku: () -> Unit,
    val onOpenDiagnostics: () -> Unit,
    val onOpenAuditLog: (() -> Unit)? = null,
    val onOpenBackup: (() -> Unit)? = null,
    val onOpenOrphans: (() -> Unit)? = null,
    val onRefreshLauncher: (() -> Unit)? = null,
    val onAutoRefreshLauncherChange: ((Boolean) -> Unit)? = null,
    val onOpenLicenses: (() -> Unit)? = null,
    val onShowAdvancedUsersChange: ((Boolean) -> Unit)? = null,
    val onOpenMore: (() -> Unit)? = null,
    val onCheckUpdate: () -> Unit = {},
    val onDownloadUpdate: () -> Unit = {},
    val onInstallUpdate: () -> Unit = {},
    val onDailyUpdateCheckChange: (Boolean) -> Unit = {},
    val onDismissUpdateFailure: () -> Unit = {}
)
