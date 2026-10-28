package com.mohitarora.dualclone.ui.viewmodel

import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.data.model.UserProfile

enum class SortOrder {
    NAME_ASC,
    NAME_DESC,
    RECENTLY_UPDATED,
    INSTALL_TIME
}

enum class FilterTab {
    ALL,
    INSTALLED,
    CLONED
}

enum class FilterPreset {
    ALL,
    USER_ONLY,
    SYSTEM_ONLY,
    CLONED_ONLY,
    NOT_CLONED
}

data class MainUiState(
    val apps: List<AppItem> = emptyList(),
    val searchQuery: String = "",
    val sortOrder: SortOrder = SortOrder.NAME_ASC,
    val filterTab: FilterTab = FilterTab.ALL,
    val includeSystem: Boolean = true,
    val unclonedOnly: Boolean = false,
    val filterPreset: FilterPreset = FilterPreset.ALL,
    val selectedPackages: Set<String> = emptySet(),
    val isBulkMode: Boolean = false,
    val isRefreshing: Boolean = false,
    val isLoading: Boolean = false,
    val totalCount: Int = 0,
    val installedCount: Int = 0,
    val clonedCount: Int = 0,
    val userAppCount: Int = 0,
    val systemAppCount: Int = 0,
    val shizukuState: ShizukuState = ShizukuState.Unavailable,
    val activeAppOpsPackage: String? = null,
    val activeAppOpsState: Map<String, String> = emptyMap(),
    val operatingPackages: Set<String> = emptySet(),
    val statusMessage: String? = null,
    val availableProfiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null
)
