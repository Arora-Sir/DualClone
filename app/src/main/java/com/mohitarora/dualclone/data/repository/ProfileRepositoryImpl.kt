package com.mohitarora.dualclone.data.repository

import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.pm.ParsedUser
import com.mohitarora.dualclone.domain.pm.PmOutputParser
import com.mohitarora.dualclone.domain.policy.ProfilePolicy
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.domain.repo.ShizukuGateway
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber

/**
 * Single source of truth for the profile list and the selected profile.
 * A failed `pm list users` keeps the previous list so a transient error never empties the chips.
 */
class ProfileRepositoryImpl(
    private val gateway: ShizukuGateway,
    private val settings: AppSettings,
    scope: CoroutineScope
) : ProfileRepository {

    private val rawUsers = MutableStateFlow<List<ParsedUser>>(emptyList())
    private val explicitSelection = MutableStateFlow<Int?>(settings.selectedProfileId.value)

    val users: StateFlow<List<ParsedUser>> = rawUsers

    override val profiles: StateFlow<List<Profile>> =
        combine(rawUsers, settings.showAdvancedUsers) { users, advanced ->
            ProfilePolicy.visibleTargets(users, advanced)
        }.stateIn(scope, SharingStarted.Eagerly, emptyList())

    override val selected: StateFlow<Profile?> =
        combine(profiles, explicitSelection) { list, id ->
            list.firstOrNull { it.userId == id } ?: ProfilePolicy.defaultSelection(list)
        }.stateIn(scope, SharingStarted.Eagerly, null)

    override suspend fun refresh() {
        val out = gateway.run("pm", "list", "users")
        if (out.exitCode != 0) {
            Timber.tag("Profiles").w("pm list users failed, keeping previous list: %s", out.stderr)
            return
        }
        val parsed = PmOutputParser.parseUsers(out.stdout)
        if (parsed.isEmpty()) {
            Timber.tag("Profiles").w("pm list users returned no users, keeping previous list")
            return
        }
        rawUsers.value = parsed
    }

    override fun select(userId: Int) {
        explicitSelection.value = userId
        settings.setSelectedProfileId(userId)
    }
}
