package com.mohitarora.dualclone.domain.policy

import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.pm.ParsedUser

/** The single place that decides what DualClone may do with a given user. */
object ProfilePolicy {

    /** Apps that own or manage a work profile. Removing one destroys the profile, so they are protected by name as a fallback. */
    val KNOWN_PROFILE_MANAGERS: Set<String> = setOf(
        "com.oasisfeng.island",
        "com.oasisfeng.island.fdroid",
        "net.typeblog.shelter",
        "com.android.managedprovisioning",
        "com.google.android.apps.work.clouddpc"
    )

    private val DUAL_IDS = 95..99
    private val KNOX_IDS = 150..159

    fun kindOf(user: ParsedUser): ProfileKind = when {
        user.userId == 0 -> ProfileKind.PRIMARY
        user.isKnox || user.userId in KNOX_IDS || user.name.contains("secure folder", ignoreCase = true) ->
            ProfileKind.SECURE_FOLDER
        // NOTE: Samsung Dual Messenger users do not carry FLAG_MANAGED_PROFILE, so they are matched by id or name before managed profiles.
        user.userId in DUAL_IDS || user.name.contains("dual", ignoreCase = true) -> ProfileKind.DUAL_MESSENGER
        user.isManaged -> ProfileKind.WORK
        else -> ProfileKind.OTHER_USER
    }

    fun toProfile(user: ParsedUser): Profile {
        val kind = kindOf(user)
        return Profile(
            userId = user.userId,
            name = user.name,
            kind = kind,
            isRunning = user.running,
            isPaused = user.isQuiet,
            isRestricted = kind == ProfileKind.SECURE_FOLDER
        )
    }

    /** No shell command may ever target Secure Folder (Knox) or the primary user. */
    fun canRunShell(kind: ProfileKind): Boolean = kind != ProfileKind.SECURE_FOLDER && kind != ProfileKind.PRIMARY

    fun canRunShell(profile: Profile): Boolean = canRunShell(profile.kind)

    /**
     * Targets shown in the profile chips. Secure Folder is shown so the Knox guidance card can open.
     * Full users such as a second human account stay hidden unless the advanced switch is on.
     */
    fun visibleTargets(users: List<ParsedUser>, showAdvancedUsers: Boolean): List<Profile> =
        users.map(::toProfile).filter { p ->
            when (p.kind) {
                ProfileKind.PRIMARY -> false
                ProfileKind.OTHER_USER -> showAdvancedUsers
                else -> true
            }
        }

    /** Preferred default when nothing was selected before: Dual Messenger, then Work, then any shell-capable target. */
    fun defaultSelection(profiles: List<Profile>): Profile? =
        profiles.firstOrNull { it.kind == ProfileKind.DUAL_MESSENGER }
            ?: profiles.firstOrNull { it.kind == ProfileKind.WORK }
            ?: profiles.firstOrNull { canRunShell(it) }
}

/** Package names of this app, so it can never be cloned into a profile. */
class HostIdentity(applicationId: String) {
    private val base = applicationId.removeSuffix(".debug")
    val packageNames: Set<String> = setOf(base, "$base.debug")
    fun isHost(packageName: String): Boolean = packageName in packageNames
}
