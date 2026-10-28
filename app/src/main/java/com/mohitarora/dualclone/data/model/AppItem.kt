package com.mohitarora.dualclone.data.model

import com.mohitarora.dualclone.domain.model.CloneState

data class AppItem(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isLaunchable: Boolean,
    val isCloned: Boolean = false,
    val installTime: Long = 0L,
    val lastUpdatedTime: Long = 0L,
    val isOperating: Boolean = false,
    /** Finer state behind isCloned. Hidden and suspended copies count as cloned. */
    val cloneState: CloneState = if (isCloned) CloneState.INSTALLED else CloneState.NOT_INSTALLED,
    /** Preinstalled system app of the selected profile. Never a clone, so no Remove button, show a System tag. */
    val isProfileSystemApp: Boolean = false,
    /** Profile owner or manager (Island, Shelter). Not a clone, never removable and never bulk selectable. */
    val isProfileOwner: Boolean = false
) {
    val isClonedUser95: Boolean get() = isCloned
}
