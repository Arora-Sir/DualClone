package com.mohitarora.dualclone.domain.model

// NOTE: Additions to the shared contract. Existing types in Contracts.kt are unchanged.

/** One row of the host app catalog. */
data class CatalogApp(
    val packageName: String,
    val appName: String,
    val isSystemApp: Boolean,
    val isLaunchable: Boolean,
    val installTime: Long = 0L,
    val lastUpdatedTime: Long = 0L
)

/** Who created a clone. SYSTEM marks preinstalled system apps of the profile, which are never clones. */
enum class CloneOriginKind { DUALCLONE, EXTERNAL, UNKNOWN, SYSTEM, PROFILE_OWNER }

/** A persisted clone state row. */
data class StoredClone(
    val packageName: String,
    val userId: Int,
    val state: CloneState,
    val origin: CloneOriginKind = CloneOriginKind.UNKNOWN,
    val verifiedAtMillis: Long = 0L
) {
    fun toEntry(hostInstalled: Boolean = true) =
        CloneEntry(packageName, userId, state, verifiedAtMillis, hostInstalled, isSystemInProfile = origin == CloneOriginKind.SYSTEM, isProfileOwner = origin == CloneOriginKind.PROFILE_OWNER)
}

/** A package in a secondary profile that has no host app in User 0. */
data class OrphanEntry(
    val packageName: String,
    val userId: Int,
    /** True when the package was installed natively in the profile and DualClone never created it. */
    val onlyInProfile: Boolean,
    val lastVerifiedMillis: Long = 0L
) {
    /** Only clones that DualClone created are selected by default. Native apps never are. */
    val selectedByDefault: Boolean get() = !onlyInProfile
}

/** Fixed names of the Samsung Dual Messenger agent, verified on device. One constant for every call site. */
object SamsungDualMessenger {
    const val PACKAGE = "com.samsung.android.da.daagent"
    const val ACTIVITY = ".activity.DualAppActivity"
    const val COMPONENT = "$PACKAGE/$ACTIVITY"
}

object SamsungLauncher {
    const val PACKAGE = "com.sec.android.app.launcher"
}

/** Modes accepted by `appops set`. Anything else is refused before a shell is involved. */
object AppOpModes {
    val ALLOWED: Set<String> = setOf("allow", "deny", "ignore", "default", "foreground")
}
