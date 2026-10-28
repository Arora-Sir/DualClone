package com.mohitarora.dualclone.domain.model

// NOTE: These types are the shared contract between the data layer (Track A) and the UI layer (Track B).
// NOTE: Add fields only with default values so both tracks keep compiling.

enum class ProfileKind { PRIMARY, DUAL_MESSENGER, WORK, SECURE_FOLDER, OTHER_USER }

data class Profile(
    val userId: Int,
    val name: String,
    val kind: ProfileKind,
    val isRunning: Boolean = true,
    val isPaused: Boolean = false,
    val isRestricted: Boolean = false
)

enum class CloneState { INSTALLED, HIDDEN, SUSPENDED, NOT_INSTALLED, UNKNOWN }

enum class PmErrorCode {
    NOT_INSTALLED_FOR_USER,
    DEVICE_POLICY_BLOCKED,
    OWNER_BLOCKED,
    USER_RESTRICTED,
    SECURITY_EXCEPTION,
    TIMEOUT,
    UNKNOWN
}

sealed interface OpResult {
    data class Success(val verified: Boolean) : OpResult
    data class Failure(val code: PmErrorCode, val raw: String) : OpResult
    data object ShizukuUnavailable : OpResult
}

sealed interface ListResult<out T> {
    data class Loaded<T>(val value: T) : ListResult<T>
    data class Unavailable(val reason: String) : ListResult<Nothing>
}

data class CloneEntry(
    val packageName: String,
    val userId: Int,
    val state: CloneState,
    val verifiedAtMillis: Long = 0L,
    val hostInstalled: Boolean = true,
    /** The package is a system app of the profile, not a clone. The UI hides Remove and shows a System tag. */
    val isSystemInProfile: Boolean = false,
    /** Profile owner or profile manager such as Island. Never removable, never a clone. */
    val isProfileOwner: Boolean = false
)

data class BulkItemResult(val packageName: String, val result: OpResult)
