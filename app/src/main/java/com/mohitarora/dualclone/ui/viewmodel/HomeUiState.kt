package com.mohitarora.dualclone.ui.viewmodel

import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.domain.model.BulkItemResult
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.repo.ReconcileStatus

enum class BulkKind { CLONE, REMOVE }

/** Result list of one bulk run, so the UI can show a per package summary and retry the failures. */
data class BulkSummary(val kind: BulkKind, val userId: Int, val items: List<BulkItemResult>) {
    val failed: List<BulkItemResult> get() = items.filter { it.result !is com.mohitarora.dualclone.domain.model.OpResult.Success }
}

data class HomeUiState(
    val apps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val filterTab: FilterTab = FilterTab.ALL,
    val includeSystem: Boolean = true,
    val unclonedOnly: Boolean = false,
    val selectedPackages: Set<String> = emptySet(),
    val isBulkMode: Boolean = false,
    val isRefreshing: Boolean = false,
    /** True until the catalog has loaded once, so the screen shows a skeleton and not an empty state. */
    val isLoading: Boolean = true,
    val totalCount: Int = 0,
    val installedCount: Int = 0,
    val clonedCount: Int = 0,
    val userAppCount: Int = 0,
    val systemAppCount: Int = 0,
    val shizukuState: ShizukuState = ShizukuState.Checking,
    val profiles: List<Profile> = emptyList(),
    val selectedProfile: Profile? = null,
    /** The last package listing for the selected profile failed, so the shown clone state may be old. */
    val isCloneStateStale: Boolean = false,
    /** The selected profile is paused (quiet mode). Operations are blocked and a warning should show. */
    val isSelectedProfilePaused: Boolean = false,
    /** Offer the Refresh launcher action after a verified removal. */
    val showLauncherRefreshHint: Boolean = false,
    /** User ids that hold a copy of DualClone itself. The Home screen offers a repair. */
    val hostCopyUserIds: List<Int> = emptyList(),
    val operatingPackages: Set<String> = emptySet(),
    val activeAppOpsPackage: String? = null,
    val activeAppOpsState: Map<String, String> = emptyMap(),
    val bulkSummary: BulkSummary? = null,
    val lastReconcile: ReconcileStatus = ReconcileStatus()
)
