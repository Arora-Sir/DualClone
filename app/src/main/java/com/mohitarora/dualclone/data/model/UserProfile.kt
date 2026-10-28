package com.mohitarora.dualclone.data.model

enum class ProfileType {
    PRIMARY,
    DUAL_APP,
    SECURE_FOLDER,
    WORK_PROFILE,
    OTHER
}

data class UserProfile(
    val userId: Int,
    val name: String,
    val type: ProfileType,
    val isRunning: Boolean = true,
    val isRestricted: Boolean = false,
    val restrictionReason: String? = null
) {
    val displayName: String
        get() = when (type) {
            ProfileType.PRIMARY -> "Primary ($userId)"
            ProfileType.DUAL_APP -> "Dual Messenger ($userId)"
            ProfileType.SECURE_FOLDER -> "Secure Folder ($userId)"
            ProfileType.WORK_PROFILE -> "Island / Work ($userId)"
            ProfileType.OTHER -> if (name.isNotBlank()) "$name ($userId)" else "Profile ($userId)"
        }
}
