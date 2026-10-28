package com.mohitarora.dualclone.data.model

sealed class ShizukuState {
    data object Checking : ShizukuState()
    data object Unavailable : ShizukuState()
    data object PermissionRequired : ShizukuState()
    data object User95Missing : ShizukuState()
    data object Ready : ShizukuState()
}
