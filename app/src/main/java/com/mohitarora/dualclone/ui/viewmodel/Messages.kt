package com.mohitarora.dualclone.ui.viewmodel

import com.mohitarora.dualclone.core.common.UiText
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind

// NOTE: Every user facing sentence produced by view models lives in this one file, as plain English text.
// NOTE: Track B moves each entry to strings.xml and swaps UiText.Plain for UiText.Res here, nowhere else.
object Messages {

    fun plain(text: String): UiText = UiText.Plain(text)

    /** Same wording as the profile chips: the kind label, or the user name for another user. */
    fun profileLabel(profile: Profile): String = when (profile.kind) {
        ProfileKind.PRIMARY -> "Primary profile"
        ProfileKind.DUAL_MESSENGER -> "Dual Messenger"
        ProfileKind.WORK -> "Work profile"
        ProfileKind.SECURE_FOLDER -> "Secure Folder"
        ProfileKind.OTHER_USER -> profile.name.trim().ifEmpty { "Other user" }
    }

    fun cloned(appName: String, profileName: String) = plain("Cloned $appName to $profileName")
    fun removed(appName: String) = plain("Removed the clone of $appName")
    fun profileOwnerProtected() = plain("This app manages the profile. Removing it would delete the profile, so it is left alone")
    fun systemAppInProfile() = plain("This is a system app of the profile, not a clone, so it is left alone")
    fun shizukuNotReady() = plain("Shizuku is not ready")
    fun profileRestricted() = plain("Samsung Knox Secure Folder blocks external shell commands. Open Secure Folder directly to manage apps.")
    fun profilePaused(profileName: String) = plain("$profileName is paused. Resume it before cloning or removing apps.")
    fun launcherRefreshed() = plain("Home screen restarted")
    fun launcherRefreshUnavailable() = plain("No Samsung launcher found, nothing to refresh")
    fun launcherRefreshFailed() = plain("Could not restart the home screen")
    fun activatingDualMessenger() = plain("Activating Samsung Dual Messenger")
    fun dualMessengerActivated() = plain("Dual Messenger activated")
    fun dualMessengerFailed(reason: String) = plain("Could not activate Dual Messenger. $reason")
    fun auditCleared() = plain("Command audit log cleared")
    fun reconcileDone() = plain("Profiles are up to date")
    fun reconcileFailed() = plain("Could not read every profile")
    fun backupSaved() = plain("Backup saved")
    fun backupFailed(reason: String) = plain("Backup failed. $reason")
    fun restoreSummary(cloned: Int, failed: Int, skipped: Int) =
        plain("Restore finished. $cloned cloned, $failed failed, $skipped skipped")

    fun restoreNotSelected() = plain("Not selected, left as it was")

    fun restoreSkipReason(reason: com.mohitarora.dualclone.domain.backup.SkipReason): UiText = plain(
        when (reason) {
            com.mohitarora.dualclone.domain.backup.SkipReason.NOT_INSTALLED_IN_HOST -> "Not installed on this device"
            com.mohitarora.dualclone.domain.backup.SkipReason.NO_MATCHING_PROFILE -> "This device has no matching profile"
            com.mohitarora.dualclone.domain.backup.SkipReason.PROFILE_RESTRICTED -> "That profile blocks shell commands"
        }
    )

    fun failure(action: String, result: OpResult): UiText = when (result) {
        is OpResult.Success -> plain("$action succeeded")
        OpResult.ShizukuUnavailable -> plain("$action failed because Shizuku is not available")
        is OpResult.Failure -> plain("$action failed. ${describe(result.code)}")
    }

    fun describe(code: PmErrorCode): String = when (code) {
        PmErrorCode.NOT_INSTALLED_FOR_USER -> "The app is not installed for that profile."
        PmErrorCode.DEVICE_POLICY_BLOCKED -> "A device policy blocks this change."
        PmErrorCode.OWNER_BLOCKED -> "The profile owner blocks this change."
        PmErrorCode.USER_RESTRICTED -> "This profile does not allow the change."
        PmErrorCode.SECURITY_EXCEPTION -> "Permission was denied by Android."
        PmErrorCode.TIMEOUT -> "The command timed out."
        PmErrorCode.UNKNOWN -> "The command did not succeed."
    }

    fun bulkSummary(verb: String, items: List<Pair<String, OpResult>>): UiText {
        val ok = items.count { it.second is OpResult.Success }
        return plain("$verb $ok of ${items.size} apps")
    }
}
