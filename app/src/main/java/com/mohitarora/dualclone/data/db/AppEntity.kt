package com.mohitarora.dualclone.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Host (User 0) app catalog. */
@Entity(
    tableName = "app",
    indices = [Index(value = ["appName"])]
)
data class AppEntity(
    @PrimaryKey
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isLaunchable: Boolean,
    val installTime: Long = 0L,
    val lastUpdatedTime: Long = 0L
)

/** Last verified state of one package in one secondary profile. */
@Entity(
    tableName = "clone_state",
    primaryKeys = ["packageName", "userId"],
    indices = [Index(value = ["userId"])]
)
data class CloneStateEntity(
    val packageName: String,
    val userId: Int,
    /** CloneState enum name. */
    val state: String,
    /** CloneOrigin constant, who created the clone. */
    val origin: String = CloneOrigin.UNKNOWN,
    val verifiedAt: Long = 0L
)

object CloneOrigin {
    const val DUALCLONE = "DUALCLONE"
    const val EXTERNAL = "EXTERNAL"
    const val UNKNOWN = "UNKNOWN"
}

/** Permission overrides the user asked DualClone to remember and re-apply. */
@Entity(
    tableName = "appops_preset",
    primaryKeys = ["userId", "packageName", "op"]
)
data class AppOpsPresetEntity(
    val userId: Int,
    val packageName: String,
    val op: String,
    val mode: String
)
