package com.mohitarora.dualclone.ui.navigation

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.ui.components.FirstRunPrefs
import com.mohitarora.dualclone.ui.screen.AuditLogScreen
import com.mohitarora.dualclone.ui.screen.BackupActions
import com.mohitarora.dualclone.ui.screen.BackupScreen
import com.mohitarora.dualclone.ui.screen.DevInfoScreen
import com.mohitarora.dualclone.ui.screen.HomeScreen
import com.mohitarora.dualclone.ui.screen.OrphansScreen
import com.mohitarora.dualclone.ui.screen.SettingsScreen
import com.mohitarora.dualclone.ui.screen.home.HomeActions
import com.mohitarora.dualclone.ui.screen.settings.SettingsActions
import com.mohitarora.dualclone.ui.screen.settings.SettingsUiState
import com.mohitarora.dualclone.ui.viewmodel.BackupViewModel
import com.mohitarora.dualclone.ui.viewmodel.DevInfoViewModel
import com.mohitarora.dualclone.ui.viewmodel.HomeViewModel
import com.mohitarora.dualclone.ui.viewmodel.OrphansViewModel
import com.mohitarora.dualclone.ui.viewmodel.ProfilesViewModel
import com.mohitarora.dualclone.ui.viewmodel.SettingsViewModel
import com.mohitarora.dualclone.ui.viewmodel.UpdateViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.merge

// NOTE: Route composables are the only place that touches view models.
// NOTE: Screens take plain state and callbacks, so they stay previewable and testable.

@Composable
fun HomeRoute(onSettings: () -> Unit) {
    val vm: HomeViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { FirstRunPrefs(context) }
    val showOnboarding = remember { !prefs.onboardingSeen }
    val actions = remember(vm, onSettings) {
        HomeActions(
            onSettings = onSettings,
            onSearchChange = vm::onSearchQueryChanged,
            onTabSelected = vm::onFilterTabSelected,
            onSortSelected = vm::onSortOrderSelected,
            onIncludeSystemChange = vm::onToggleIncludeSystem,
            onUnclonedOnlyChange = vm::onToggleUnclonedOnly,
            onProfileSelected = vm::selectProfile,
            onToggleSelection = vm::toggleAppSelection,
            onStartBulk = vm::startBulkMode,
            onExitBulk = vm::exitBulkMode,
            onToggleClone = vm::toggleClone,
            onBulkClone = vm::bulkCloneSelected,
            onBulkRemove = vm::bulkRemoveSelected,
            onRetryFailed = vm::retryFailed,
            onDismissBulkSummary = vm::dismissBulkSummary,
            onRefresh = vm::refresh,
            onRefreshLauncher = vm::refreshLauncher,
            onDismissLauncherHint = vm::dismissLauncherHint,
            onRepairHostCopy = vm::repairHostCopy,
            onOpenAppOps = vm::openAppOpsSheet,
            onCloseAppOps = vm::closeAppOpsSheet,
            onToggleAppOp = vm::toggleAppOp,
            onRequestShizukuPermission = vm::requestShizukuPermission,
            onRefreshShizuku = vm::refreshShizukuStatus,
            onResume = vm::onResume
        )
    }
    HomeScreen(
        state = state,
        actions = actions,
        events = vm.events,
        showOnboarding = showOnboarding,
        onOnboardingSeen = { prefs.onboardingSeen = true }
    )
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onMore: () -> Unit,
    onBackup: () -> Unit,
    onOrphans: () -> Unit
) {
    val vm: SettingsViewModel = hiltViewModel()
    val profilesVm: ProfilesViewModel = hiltViewModel()
    val theme by vm.themeConfig.collectAsStateWithLifecycle()
    val shizuku by vm.shizukuState.collectAsStateWithLifecycle()
    val autoRefresh by vm.autoRefreshLauncher.collectAsStateWithLifecycle()
    val log by vm.auditLog.collectAsStateWithLifecycle()
    val reconcile by vm.lastReconcile.collectAsStateWithLifecycle()
    val profiles by profilesVm.state.collectAsStateWithLifecycle()
    val updates: UpdateViewModel = hiltViewModel()
    val updateState by updates.state.collectAsStateWithLifecycle()
    val dailyCheck by updates.dailyCheckEnabled.collectAsStateWithLifecycle()
    InstallRequestHandler(updates)

    val state = SettingsUiState(
        theme = theme,
        shizuku = shizuku,
        profiles = profiles.profiles,
        isSamsung = Build.MANUFACTURER.equals("samsung", ignoreCase = true),
        canSetUpDualMessenger = profiles.canSetUpDualMessenger,
        showAdvancedUsers = profiles.showAdvancedUsers,
        autoRefreshLauncher = autoRefresh,
        auditLogCount = log.size,
        reconcileSummary = reconcileSummary(reconcile.atMillis, reconcile.ok),
        update = updateState,
        currentVersion = updates.currentVersion,
        dailyUpdateCheck = dailyCheck
    )
    val actions = remember(vm, profilesVm, updates) {
        SettingsActions(
            onBack = onBack,
            onModeChange = vm::setMode,
            onPresetChange = vm::setPreset,
            onApplyCustomColor = vm::setCustomColor,
            onPureBlackChange = vm::setPureBlack,
            onEnableDualMessenger = profilesVm::enableDualMessenger,
            onClearAuditLog = vm::clearAuditLog,
            onOpenDiagnostics = {},
            onRequestShizukuPermission = vm::requestShizukuPermission,
            onRefreshShizuku = vm::refreshShizukuStatus,
            onOpenMore = onMore,
            onCheckUpdate = updates::check,
            onDownloadUpdate = updates::download,
            onInstallUpdate = updates::install,
            onDailyUpdateCheckChange = updates::setDailyCheck,
            onDismissUpdateFailure = updates::dismissFailure,
            onOpenBackup = onBackup,
            onOpenOrphans = onOrphans,
            onRefreshLauncher = vm::refreshLauncher,
            onAutoRefreshLauncherChange = vm::setAutoRefreshLauncher,
            onShowAdvancedUsersChange = profilesVm::setShowAdvancedUsers
        )
    }
    val events: Flow<UiEvent> = remember(vm, profilesVm) { merge(vm.events, profilesVm.events) }
    SettingsScreen(state = state, actions = actions, events = events)
}

@Composable
private fun reconcileSummary(atMillis: Long, ok: Boolean): String? =
    if (atMillis <= 0L) null else com.mohitarora.dualclone.ui.screen.home.reconcileText(atMillis, ok)

@Composable
fun DevInfoRoute(onBack: () -> Unit, onAuditLog: () -> Unit) {
    val vm: DevInfoViewModel = hiltViewModel()
    val stats by vm.dbStats.collectAsStateWithLifecycle()
    val logs by vm.recentLogs.collectAsStateWithLifecycle()
    val diagnostics by vm.diagnostics.collectAsStateWithLifecycle()
    DevInfoScreen(
        totalApps = stats.first,
        cloneCount = stats.second,
        logCount = logs.size,
        diagnostics = diagnostics,
        onOpenAuditLog = onAuditLog,
        onBack = onBack
    )
}

@Composable
fun AuditLogRoute(onBack: () -> Unit) {
    val vm: SettingsViewModel = hiltViewModel()
    val logs by vm.auditLog.collectAsStateWithLifecycle()
    AuditLogScreen(logs = logs, onClear = vm::clearAuditLog, onBack = onBack)
}

@Composable
fun BackupRoute(onBack: () -> Unit) {
    val vm: BackupViewModel = hiltViewModel()
    val phase by vm.phase.collectAsStateWithLifecycle()
    val actions = remember(vm) {
        BackupActions(
            onExport = vm::export,
            onOpenBackup = vm::openBackup,
            onSubmitPassword = vm::submitPassword,
            onCloneIncluded = vm::setCloneIncluded,
            onClonesIncluded = vm::setClonesIncluded,
            onPresetsIncluded = vm::setPresetsIncluded,
            onSettingsIncluded = vm::setSettingsIncluded,
            onApply = vm::applyRestore,
            onReset = vm::reset,
            onBack = onBack
        )
    }
    BackupScreen(phase = phase, actions = actions, events = vm.events)
}

@Composable
fun OrphansRoute(onBack: () -> Unit) {
    val vm: OrphansViewModel = hiltViewModel()
    val state by vm.state.collectAsStateWithLifecycle()
    OrphansScreen(state = state, onScan = vm::scan, onToggle = vm::toggle, onPurge = vm::purgeSelected, onBack = onBack)
}

@Composable
fun MoreRoute(onBack: () -> Unit, onAuditLog: () -> Unit, onDiagnostics: () -> Unit) {
    val vm: SettingsViewModel = hiltViewModel()
    val actions = remember(vm) {
        com.mohitarora.dualclone.ui.screen.MoreActions(
            onBack = onBack,
            onOpenAuditLog = onAuditLog,
            onClearAuditLog = vm::clearAuditLog,
            onOpenDiagnostics = onDiagnostics
        )
    }
    com.mohitarora.dualclone.ui.screen.MoreScreen(actions = actions, events = vm.events)
}

/** Acts on one-shot install requests: asks for the unknown sources permission, or starts the system installer. */
@Composable
private fun InstallRequestHandler(updates: UpdateViewModel) {
    val context = LocalContext.current
    var permissionIntent by remember { mutableStateOf<android.content.Intent?>(null) }
    LaunchedEffect(updates) {
        updates.installRequests.collect { request ->
            when (request) {
                is com.mohitarora.dualclone.update.InstallRequest.NeedsPermission -> permissionIntent = request.settingsIntent
                is com.mohitarora.dualclone.update.InstallRequest.Ready ->
                    runCatching { context.startActivity(request.installIntent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
                com.mohitarora.dualclone.update.InstallRequest.Unavailable -> Unit
            }
        }
    }
    permissionIntent?.let { intent ->
        com.mohitarora.dualclone.core.designsystem.components.DcConfirmDialog(
            title = androidx.compose.ui.res.stringResource(com.mohitarora.dualclone.R.string.update_permission_title),
            message = androidx.compose.ui.res.stringResource(com.mohitarora.dualclone.R.string.update_permission_body),
            confirmLabel = androidx.compose.ui.res.stringResource(com.mohitarora.dualclone.R.string.update_permission_open),
            onConfirm = {
                permissionIntent = null
                runCatching { context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)) }
            },
            onDismiss = { permissionIntent = null }
        )
    }
}
