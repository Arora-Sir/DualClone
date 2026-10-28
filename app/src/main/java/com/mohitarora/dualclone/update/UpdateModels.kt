package com.mohitarora.dualclone.update

import com.mohitarora.dualclone.core.common.UiText

/** Why an update step failed. Mapped to user text in UpdateErrorText. */
enum class UpdateError {
    NETWORK,
    TIMEOUT,
    BAD_RESPONSE,
    RATE_LIMITED,
    WRONG_HOST,
    NO_APK,
    NO_CHECKSUM,
    CHECKSUM_MISMATCH,
    TOO_LARGE,
    DOWNLOAD_INTERRUPTED,
    STORAGE,
    NOT_AVAILABLE
}

class UpdateException(val error: UpdateError, detail: String = error.name, cause: Throwable? = null) :
    Exception(detail, cause)

/** A published stable release that carries a signed APK and its checksum list. */
data class ReleaseInfo(
    val version: SemVer,
    val tag: String,
    val changelog: String,
    val apkName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
    val checksumsUrl: String
)

sealed interface UpdateState {
    data object Idle : UpdateState
    data object Checking : UpdateState
    data object UpToDate : UpdateState
    data class Available(val version: String, val changelog: String, val sizeBytes: Long) : UpdateState

    /** Fraction from 0 to 1, or null while the total size is unknown. */
    data class Downloading(val progress: Float?) : UpdateState
    data class Downloaded(val version: String) : UpdateState
    data class Failed(val error: UpdateError, val reason: UiText) : UpdateState
}

sealed interface CheckResult {
    data object UpToDate : CheckResult
    data class Newer(val release: ReleaseInfo) : CheckResult
}
