package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.core.common.UiText
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.data.model.ProfileType
import com.mohitarora.dualclone.data.model.UserProfile
import com.mohitarora.dualclone.data.repository.AppOpsRepositoryImpl
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.DualMessengerSetup
import com.mohitarora.dualclone.data.repository.Reconciler
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.receiver.PackageEventMonitor
import com.mohitarora.dualclone.shizuku.ShizukuManager
import com.mohitarora.dualclone.work.ReconcileScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Compatibility adapter for screens written against the old single view model.
 * All logic lives in HomeController. New code uses HomeViewModel, ProfilesViewModel and the other feature view models.
 * Profile selection is held by ProfileRepository, so two instances of this class can no longer disagree (R3).
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    catalog: CatalogRepository,
    clones: CloneStateRepository,
    private val profiles: ProfileRepository,
    appOps: AppOpsRepositoryImpl,
    shizuku: ShizukuManager,
    reconciler: Reconciler,
    settings: AppSettings,
    private val dualMessenger: DualMessengerSetup,
    monitor: PackageEventMonitor,
    scheduler: ReconcileScheduler
) : ViewModel() {

    private val eventChannel = UiEventChannel()
    private val controller = HomeController(viewModelScope, catalog, clones, profiles, appOps, shizuku, reconciler, settings, eventChannel)

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage = _statusMessage.asStateFlow()

    val searchQuery: StateFlow<String> = controller.state.map { it.searchQuery }.stateIn(viewModelScope, SharingStarted.Eagerly, "")
    val sortOrder: StateFlow<SortOrder> = controller.state.map { it.sortOrder }.stateIn(viewModelScope, SharingStarted.Eagerly, SortOrder.NAME_ASC)
    val filterTab: StateFlow<FilterTab> = controller.state.map { it.filterTab }.stateIn(viewModelScope, SharingStarted.Eagerly, FilterTab.ALL)
    val includeSystem: StateFlow<Boolean> = controller.state.map { it.includeSystem }.stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val unclonedOnly: StateFlow<Boolean> = controller.state.map { it.unclonedOnly }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val selectedPackages: StateFlow<Set<String>> = controller.state.map { it.selectedPackages }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val isBulkMode: StateFlow<Boolean> = controller.state.map { it.isBulkMode }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val isRefreshing: StateFlow<Boolean> = controller.state.map { it.isRefreshing }.stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val operatingPackages: StateFlow<Set<String>> = controller.state.map { it.operatingPackages }.stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())
    val activeAppOpsPackage: StateFlow<String?> = controller.state.map { it.activeAppOpsPackage }.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val activeAppOpsState: StateFlow<Map<String, String>> = controller.state.map { it.activeAppOpsState }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyMap())
    val availableProfiles: StateFlow<List<UserProfile>> = profiles.profiles.map { list -> list.filter { it.kind != ProfileKind.SECURE_FOLDER }.map { it.toLegacy() } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val selectedProfile: StateFlow<UserProfile?> = profiles.selected.map { it?.toLegacy() }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val uiState: StateFlow<MainUiState> = controller.state.map { s ->
        MainUiState(
            apps = s.apps, searchQuery = s.searchQuery, sortOrder = s.sortOrder, filterTab = s.filterTab,
            includeSystem = s.includeSystem, unclonedOnly = s.unclonedOnly,
            selectedPackages = s.selectedPackages, isBulkMode = s.isBulkMode,
            isRefreshing = s.isRefreshing, isLoading = s.isLoading || s.isRefreshing,
            totalCount = s.totalCount, installedCount = s.installedCount, clonedCount = s.clonedCount,
            userAppCount = s.userAppCount, systemAppCount = s.systemAppCount, shizukuState = s.shizukuState,
            activeAppOpsPackage = s.activeAppOpsPackage, activeAppOpsState = s.activeAppOpsState,
            operatingPackages = s.operatingPackages,
            availableProfiles = s.profiles.filter { it.kind != ProfileKind.SECURE_FOLDER }.map { it.toLegacy() },
            selectedProfile = s.selectedProfile?.toLegacy()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MainUiState())

    init {
        monitor.start()
        scheduler.ensurePeriodic()
        viewModelScope.launch {
            controller.events.events.collect { event ->
                _statusMessage.value = (event.message as? UiText.Plain)?.value
            }
        }
        viewModelScope.launch {
            shizuku.shizukuState.collect { if (it is com.mohitarora.dualclone.data.model.ShizukuState.Ready) profiles.refresh() }
        }
    }

    fun onSearchQueryChanged(query: String) = controller.onSearchQueryChanged(query)
    fun onSortOrderSelected(order: SortOrder) = controller.onSortOrderSelected(order)
    fun onFilterPresetSelected(@Suppress("UNUSED_PARAMETER") preset: FilterPreset) = Unit
    fun onFilterTabSelected(tab: FilterTab) = controller.onFilterTabSelected(tab)
    fun onToggleIncludeSystem(include: Boolean) = controller.onToggleIncludeSystem(include)
    fun onToggleUnclonedOnly(value: Boolean) = controller.onToggleUnclonedOnly(value)
    fun toggleAppSelection(packageName: String) = controller.toggleAppSelection(packageName)
    fun startBulkMode(initialPackage: String) = controller.startBulkMode(initialPackage)
    fun exitBulkMode() = controller.exitBulkMode()
    fun requestShizukuPermission() = controller.requestShizukuPermission()
    fun refreshShizukuStatus() = controller.refreshShizukuStatus()
    fun selectProfile(profile: UserProfile) = controller.selectProfile(profile.userId)
    fun refreshCatalog() = controller.refresh()
    fun toggleClone(app: AppItem) = controller.toggleClone(app)
    fun bulkCloneSelected() = controller.bulkCloneSelected()
    fun bulkRemoveSelected() = controller.bulkRemoveSelected()
    fun openAppOpsSheet(packageName: String) = controller.openAppOpsSheet(packageName)
    fun closeAppOpsSheet() = controller.closeAppOpsSheet()
    fun toggleAppOp(op: String, allow: Boolean) = controller.toggleAppOp(op, allow)
    fun dismissStatusMessage() { _statusMessage.value = null }

    fun enableTabletDualMessenger() {
        viewModelScope.launch {
            _statusMessage.value = (Messages.activatingDualMessenger() as UiText.Plain).value
            val failure = dualMessenger.enable()
            _statusMessage.value = ((if (failure == null) Messages.dualMessengerActivated() else Messages.dualMessengerFailed(failure)) as UiText.Plain).value
        }
    }

    private fun Profile.toLegacy(): UserProfile = UserProfile(
        userId = userId,
        name = name,
        type = when (kind) {
            ProfileKind.PRIMARY -> ProfileType.PRIMARY
            ProfileKind.DUAL_MESSENGER -> ProfileType.DUAL_APP
            ProfileKind.WORK -> ProfileType.WORK_PROFILE
            ProfileKind.SECURE_FOLDER -> ProfileType.SECURE_FOLDER
            ProfileKind.OTHER_USER -> ProfileType.OTHER
        },
        isRunning = isRunning,
        isRestricted = isRestricted,
        restrictionReason = if (isRestricted) (Messages.profileRestricted() as UiText.Plain).value else null
    )
}
