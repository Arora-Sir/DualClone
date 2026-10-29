package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.data.repository.AppOpsRepositoryImpl
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.Reconciler
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    catalog: CatalogRepository,
    clones: CloneStateRepository,
    profiles: ProfileRepository,
    appOps: AppOpsRepositoryImpl,
    shizuku: ShizukuManager,
    reconciler: Reconciler,
    settings: AppSettings
) : ViewModel() {

    private val eventChannel = UiEventChannel()
    private val controller = HomeController(viewModelScope, catalog, clones, profiles, appOps, shizuku, reconciler, settings, eventChannel)

    val state: StateFlow<HomeUiState> = controller.state

    /** One-shot snackbar messages. Collect once per screen. */
    val events: Flow<UiEvent> = eventChannel.events

    fun onSearchQueryChanged(value: String) = controller.onSearchQueryChanged(value)
    fun onSortOrderSelected(value: SortOrder) = controller.onSortOrderSelected(value)
    fun onFilterTabSelected(value: FilterTab) = controller.onFilterTabSelected(value)
    fun onToggleIncludeSystem(value: Boolean) = controller.onToggleIncludeSystem(value)
    fun onToggleUnclonedOnly(value: Boolean) = controller.onToggleUnclonedOnly(value)
    fun toggleAppSelection(packageName: String) = controller.toggleAppSelection(packageName)
    fun startBulkMode(packageName: String) = controller.startBulkMode(packageName)
    fun exitBulkMode() = controller.exitBulkMode()
    fun selectProfile(userId: Int) = controller.selectProfile(userId)
    fun requestShizukuPermission() = controller.requestShizukuPermission()
    fun refreshShizukuStatus() = controller.refreshShizukuStatus()
    fun refresh() = controller.refresh()
    fun onResume() = controller.onResume()
    fun toggleClone(app: AppItem) = controller.toggleClone(app)
    fun bulkCloneSelected() = controller.bulkCloneSelected()
    fun bulkRemoveSelected() = controller.bulkRemoveSelected()
    fun retryFailed() = controller.retryFailed()
    fun dismissBulkSummary() = controller.dismissBulkSummary()
    fun refreshLauncher() = controller.refreshLauncher()
    fun dismissLauncherHint() = controller.dismissLauncherHint()
    fun onEventAction(actionId: String) = controller.onEventAction(actionId)
    fun repairHostCopy(userId: Int) = controller.repairHostCopy(userId)
    fun openAppOpsSheet(packageName: String) = controller.openAppOpsSheet(packageName)
    fun closeAppOpsSheet() = controller.closeAppOpsSheet()
    fun toggleAppOp(op: String, allow: Boolean) = controller.toggleAppOp(op, allow)
}
