package com.mohitarora.dualclone.ui.screen.home

import androidx.compose.runtime.Immutable
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.ui.viewmodel.FilterTab
import com.mohitarora.dualclone.ui.viewmodel.SortOrder

/** Callbacks of the Home screen. HomeViewModel methods map one to one. */
@Immutable
class HomeActions(
    val onSettings: () -> Unit,
    val onSearchChange: (String) -> Unit,
    val onTabSelected: (FilterTab) -> Unit,
    val onSortSelected: (SortOrder) -> Unit,
    val onIncludeSystemChange: (Boolean) -> Unit,
    val onUnclonedOnlyChange: (Boolean) -> Unit,
    val onProfileSelected: (Int) -> Unit,
    val onToggleSelection: (String) -> Unit,
    val onStartBulk: (String) -> Unit,
    val onExitBulk: () -> Unit,
    val onToggleClone: (AppItem) -> Unit,
    val onBulkClone: () -> Unit,
    val onBulkRemove: () -> Unit,
    val onRetryFailed: () -> Unit,
    val onDismissBulkSummary: () -> Unit,
    val onRefresh: () -> Unit,
    val onRefreshLauncher: () -> Unit,
    val onDismissLauncherHint: () -> Unit,
    val onRepairHostCopy: (Int) -> Unit,
    val onOpenAppOps: (String) -> Unit,
    val onCloseAppOps: () -> Unit,
    val onToggleAppOp: (String, Boolean) -> Unit,
    val onRequestShizukuPermission: () -> Unit,
    val onRefreshShizuku: () -> Unit,
    val onEventAction: (String) -> Unit = {},
    val onResume: () -> Unit = {}
)
