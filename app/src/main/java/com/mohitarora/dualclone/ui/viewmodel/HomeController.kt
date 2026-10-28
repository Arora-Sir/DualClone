package com.mohitarora.dualclone.ui.viewmodel

import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.data.repository.AppOpsRepositoryImpl
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.Reconciler
import com.mohitarora.dualclone.domain.model.CatalogApp
import com.mohitarora.dualclone.domain.model.CloneEntry
import com.mohitarora.dualclone.domain.model.CloneState
import com.mohitarora.dualclone.domain.model.ListResult
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.shizuku.ShizukuManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import timber.log.Timber

/**
 * The Home logic as a plain class so HomeViewModel and the legacy MainViewModel adapter share one implementation.
 * It owns no Android types. All device effects go through repositories.
 */
@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HomeController(
    private val scope: CoroutineScope,
    private val catalog: CatalogRepository,
    private val clones: CloneStateRepository,
    private val profiles: ProfileRepository,
    private val appOps: AppOpsRepositoryImpl,
    private val shizuku: ShizukuManager,
    private val reconciler: Reconciler,
    private val settings: AppSettings,
    val events: UiEventChannel
) {
    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SortOrder.NAME_ASC)
    private val tab = MutableStateFlow(FilterTab.ALL)
    private val includeSystem = MutableStateFlow(true)
    private val unclonedOnly = MutableStateFlow(false)
    private val selected = MutableStateFlow<Set<String>>(emptySet())
    private val bulkMode = MutableStateFlow(false)
    private val refreshing = MutableStateFlow(false)
    private val operating = MutableStateFlow<Set<String>>(emptySet())
    private val appOpsPackage = MutableStateFlow<String?>(null)
    private val appOpsState = MutableStateFlow<Map<String, String>>(emptyMap())
    private val bulkSummary = MutableStateFlow<BulkSummary?>(null)
    private val catalogLoaded = MutableStateFlow(false)

    private val cloneMap: Flow<Map<String, CloneEntry>> = profiles.selected
        .map { it?.userId }
        .distinctUntilChanged()
        .flatMapLatest { id -> if (id == null) flowOf(emptyMap()) else clones.clonesFor(id) }

    private val catalogApps: Flow<List<CatalogApp>> = query.debounce(150).distinctUntilChanged()
        .flatMapLatest { catalog.observe(it) }

    private data class Filters(val sort: SortOrder, val tab: FilterTab, val system: Boolean, val uncloned: Boolean, val query: String)
    private data class Transient(
        val selected: Set<String>, val bulk: Boolean, val refreshing: Boolean,
        val operating: Set<String>, val summary: BulkSummary?
    )
    private data class Platform(
        val shizuku: ShizukuState, val profiles: List<Profile>, val selected: Profile?,
        val stale: Boolean, val hint: Boolean
    )

    private val filters = combine(sort, tab, includeSystem, unclonedOnly, query) { s, t, sys, u, q -> Filters(s, t, sys, u, q) }
    private val transient = combine(selected, bulkMode, refreshing, operating, bulkSummary) { s, b, r, o, sum ->
        Transient(s, b, r, o, sum)
    }
    private val platform = combine(
        shizuku.shizukuState, profiles.profiles, profiles.selected,
        clones.staleUsers, clones.launcherRefreshHint
    ) { z, list, sel, stale, hint ->
        Platform(z, list, sel, sel != null && sel.userId in stale, hint)
    }
    private val appOpsSheet = combine(appOpsPackage, appOpsState, settings.lastReconcile) { p, s, r -> Triple(p, s, r) }
    private val counts = combine(catalog.userAppCount, catalog.systemAppCount, clones.hostCopies()) { u, s, h ->
        Triple(u, s, h.map { it.first })
    }

    val state: StateFlow<HomeUiState> = combine(
        catalogApps, cloneMap, filters, transient, platform
    ) { apps, cloneEntries, f, t, p ->
        BaseState(apps, cloneEntries, f, t, p)
    }.combine(appOpsSheet) { base, sheet -> base to sheet }
        .combine(counts) { (base, sheet), c -> build(base, sheet, c) }
        .stateIn(scope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    private data class BaseState(
        val apps: List<CatalogApp>, val clones: Map<String, CloneEntry>,
        val filters: Filters, val transient: Transient, val platform: Platform
    )

    private fun build(
        base: BaseState,
        sheet: Triple<String?, Map<String, String>, com.mohitarora.dualclone.domain.repo.ReconcileStatus>,
        counts: Triple<Int, Int, List<Int>>
    ): HomeUiState {
        val f = base.filters
        val items = base.apps.map { app ->
            buildAppItem(app, base.clones[app.packageName], app.packageName in base.transient.operating)
        }
        val catalogBase = if (f.system) items else items.filter { !it.isSystemApp }
        val visible = items.filter { app ->
            (f.system || !app.isSystemApp) &&
                when (f.tab) {
                    FilterTab.ALL -> true
                    FilterTab.INSTALLED -> !app.isCloned
                    FilterTab.CLONED -> app.isCloned
                } &&
                (!f.uncloned || !app.isCloned)
        }
        val profile = base.platform.selected
        return HomeUiState(
            apps = sortApps(visible, f.sort),
            searchQuery = f.query, sortOrder = f.sort, filterTab = f.tab,
            includeSystem = f.system, unclonedOnly = f.uncloned,
            selectedPackages = base.transient.selected, isBulkMode = base.transient.bulk,
            isRefreshing = base.transient.refreshing,
            isLoading = !catalogLoaded.value && items.isEmpty(),
            totalCount = catalogBase.size, installedCount = catalogBase.count { !it.isCloned },
            clonedCount = catalogBase.count { it.isCloned },
            userAppCount = counts.first, systemAppCount = counts.second,
            shizukuState = base.platform.shizuku, profiles = base.platform.profiles, selectedProfile = profile,
            isCloneStateStale = base.platform.stale,
            isSelectedProfilePaused = profile?.isPaused == true,
            showLauncherRefreshHint = base.platform.hint,
            hostCopyUserIds = counts.third,
            operatingPackages = base.transient.operating,
            activeAppOpsPackage = sheet.first, activeAppOpsState = sheet.second,
            bulkSummary = base.transient.summary, lastReconcile = sheet.third
        )
    }

    init {
        scope.launch {
            if (catalog.isEmpty()) catalog.refresh()
            catalogLoaded.value = true
            // NOTE: Package events only arrive while the app runs, so apps installed or removed in between need this pass on every start.
            catalog.refresh()
        }
    }

    // region inputs

    fun onSearchQueryChanged(value: String) { query.value = value }
    fun onSortOrderSelected(value: SortOrder) { sort.value = value }
    fun onFilterTabSelected(value: FilterTab) { tab.value = value }
    fun onToggleIncludeSystem(value: Boolean) { includeSystem.value = value }
    fun onToggleUnclonedOnly(value: Boolean) { unclonedOnly.value = value }

    fun toggleAppSelection(packageName: String) {
        // NOTE: Profile owners and profile system apps can never be bulk selected.
        if (state.value.apps.any { it.packageName == packageName && (it.isProfileOwner || it.isProfileSystemApp) }) return
        selected.update { if (packageName in it) it - packageName else it + packageName }
        if (selected.value.isEmpty()) bulkMode.value = false
    }

    fun startBulkMode(initialPackage: String) {
        if (state.value.apps.any { it.packageName == initialPackage && (it.isProfileOwner || it.isProfileSystemApp) }) return
        bulkMode.value = true
        selected.value = setOf(initialPackage)
    }

    fun exitBulkMode() {
        bulkMode.value = false
        selected.value = emptySet()
    }

    fun selectProfile(userId: Int) = profiles.select(userId)
    fun requestShizukuPermission() = shizuku.requestPermission()
    fun refreshShizukuStatus() = shizuku.refreshStatus()
    fun dismissBulkSummary() { bulkSummary.value = null }
    fun dismissLauncherHint() = clones.dismissLauncherHint()

    // endregion

    // region actions

    fun refresh() {
        if (refreshing.value) return
        scope.launch {
            refreshing.value = true
            try {
                catalog.refresh()
                val report = reconciler.run()
                events.send(UiEvent(if (report.ok) Messages.reconcileDone() else Messages.reconcileFailed()))
            } finally {
                refreshing.value = false
            }
        }
    }

    /** Reconcile on ON_RESUME. Quiet and cheap, no snackbar. */
    fun onResume() {
        val userId = profiles.selected.value?.userId ?: return
        if (!ProfilePolicy.canRunShell(profiles.selected.value!!)) return
        scope.launch { clones.reconcile(userId) }
    }

    fun toggleClone(app: AppItem) {
        if (app.isProfileOwner) {
            events.send(UiEvent(Messages.profileOwnerProtected()))
            return
        }
        if (app.isProfileSystemApp) {
            events.send(UiEvent(Messages.systemAppInProfile()))
            return
        }
        val profile = guard() ?: return
        scope.launch {
            operating.update { it + app.packageName }
            try {
                if (app.isCloned) {
                    val result = clones.remove(app.packageName, profile.userId)
                    events.send(
                        UiEvent(
                            if (result is OpResult.Success) Messages.removed(app.appName) else Messages.failure("Removing ${app.appName}", result)
                        )
                    )
                    if (result is OpResult.Success) maybeAutoRefreshLauncher()
                } else {
                    val result = clones.clone(app.packageName, profile.userId)
                    events.send(
                        UiEvent(
                            if (result is OpResult.Success) Messages.cloned(app.appName, Messages.profileLabel(profile)) else Messages.failure("Cloning ${app.appName}", result)
                        )
                    )
                }
            } finally {
                operating.update { it - app.packageName }
            }
        }
    }

    fun bulkCloneSelected() = runBulk(BulkKind.CLONE, selected.value.toList())
    fun bulkRemoveSelected() = runBulk(BulkKind.REMOVE, selected.value.toList())

    /** Re-runs only the failed packages of the last summary. */
    fun retryFailed() {
        val summary = bulkSummary.value ?: return
        runBulk(summary.kind, summary.failed.map { it.packageName })
    }

    private fun runBulk(kind: BulkKind, requested: List<String>) {
        val protected = state.value.apps.filter { it.isProfileOwner || it.isProfileSystemApp }.map { it.packageName }.toSet()
        val packages = requested.filter { it !in protected }
        val profile = guard() ?: return
        if (packages.isEmpty()) return
        exitBulkMode()
        scope.launch {
            operating.update { it + packages }
            try {
                val results = if (kind == BulkKind.CLONE) clones.cloneAll(packages, profile.userId) else clones.removeAll(packages, profile.userId)
                val summary = BulkSummary(kind, profile.userId, results)
                bulkSummary.value = summary
                val verb = if (kind == BulkKind.CLONE) "Cloned" else "Removed"
                events.send(UiEvent(Messages.bulkSummary(verb, results.map { it.packageName to it.result })))
                if (kind == BulkKind.REMOVE && results.any { it.result is OpResult.Success }) maybeAutoRefreshLauncher()
            } finally {
                operating.update { it - packages.toSet() }
            }
        }
    }

    fun refreshLauncher() {
        scope.launch {
            val ran = clones.refreshLauncher()
            events.send(UiEvent(if (ran) Messages.launcherRefreshed() else Messages.launcherRefreshUnavailable()))
        }
    }

    private suspend fun maybeAutoRefreshLauncher() {
        if (settings.autoRefreshLauncher.value) clones.refreshLauncher()
    }

    /** Returns the selected profile if operations are allowed, otherwise explains why and returns null. */
    private fun guard(): Profile? {
        if (shizuku.shizukuState.value !is ShizukuState.Ready) {
            events.send(UiEvent(Messages.shizukuNotReady()))
            return null
        }
        val profile = profiles.selected.value ?: return null
        if (!ProfilePolicy.canRunShell(profile)) {
            events.send(UiEvent(Messages.profileRestricted()))
            return null
        }
        if (profile.isPaused) {
            events.send(UiEvent(Messages.profilePaused(Messages.profileLabel(profile))))
            return null
        }
        return profile
    }

    /** Removes DualClone itself from a profile where it was cloned by mistake. */
    fun repairHostCopy(userId: Int) {
        scope.launch {
            val pkg = clones.hostCopies().first().firstOrNull { it.first == userId }?.second ?: return@launch
            val result = clones.remove(pkg, userId)
            events.send(UiEvent(if (result is OpResult.Success) Messages.plain("Removed DualClone from the profile") else Messages.failure("Removing DualClone", result)))
        }
    }

    // endregion

    // region AppOps

    fun openAppOpsSheet(packageName: String) {
        appOpsPackage.value = packageName
        loadAppOps(packageName)
    }

    fun closeAppOpsSheet() {
        appOpsPackage.value = null
        appOpsState.value = emptyMap()
    }

    fun toggleAppOp(op: String, allow: Boolean) {
        val pkg = appOpsPackage.value ?: return
        val userId = profiles.selected.value?.userId ?: return
        scope.launch {
            val result = appOps.setRemembered(pkg, op, if (allow) "allow" else "deny", userId, remember = true)
            if (result is OpResult.Success) loadAppOps(pkg) else events.send(UiEvent(Messages.failure("Setting $op", result)))
        }
    }

    private fun loadAppOps(packageName: String) {
        val userId = profiles.selected.value?.userId ?: return
        scope.launch {
            val results = APP_OPS.map { op ->
                async {
                    val value = withTimeoutOrNull(4_000L) {
                        when (val r = appOps.get(packageName, op, userId)) {
                            is ListResult.Loaded -> r.value
                            is ListResult.Unavailable -> "unknown"
                        }
                    } ?: "timeout"
                    op to value
                }
            }.awaitAll()
            appOpsState.value = results.toMap()
        }
    }

    // endregion

    private fun sortApps(apps: List<AppItem>, order: SortOrder): List<AppItem> = when (order) {
        SortOrder.NAME_ASC -> apps.sortedBy { it.appName.lowercase() }
        SortOrder.NAME_DESC -> apps.sortedByDescending { it.appName.lowercase() }
        SortOrder.RECENTLY_UPDATED -> apps.sortedByDescending { it.lastUpdatedTime }
        SortOrder.INSTALL_TIME -> apps.sortedByDescending { it.installTime }
    }

    private companion object {
        val APP_OPS = listOf("MANAGE_EXTERNAL_STORAGE", "REQUEST_INSTALL_PACKAGES", "WRITE_SETTINGS", "SYSTEM_ALERT_WINDOW")
    }
}

/** Maps a catalog app and its clone entry for the selected profile to the item the UI shows. */
fun buildAppItem(app: CatalogApp, entry: CloneEntry?, operating: Boolean): AppItem {
    val state = entry?.state ?: CloneState.NOT_INSTALLED
    val systemInProfile = entry?.isSystemInProfile == true
    val profileOwner = entry?.isProfileOwner == true
    val present = state == CloneState.INSTALLED || state == CloneState.HIDDEN || state == CloneState.SUSPENDED
    return AppItem(
        packageName = app.packageName, appName = app.appName, isSystemApp = app.isSystemApp,
        isLaunchable = app.isLaunchable,
        // NOTE: A preinstalled system app of the profile is present but is not a clone.
        isCloned = present && !systemInProfile && !profileOwner,
        installTime = app.installTime, lastUpdatedTime = app.lastUpdatedTime,
        isOperating = operating, cloneState = state, isProfileSystemApp = systemInProfile, isProfileOwner = profileOwner
    )
}
