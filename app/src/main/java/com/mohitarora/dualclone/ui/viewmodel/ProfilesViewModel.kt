package com.mohitarora.dualclone.ui.viewmodel

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.data.repository.DualMessengerSetup
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfilesUiState(
    val profiles: List<Profile> = emptyList(),
    val selected: Profile? = null,
    val showAdvancedUsers: Boolean = false,
    /** True on a Samsung device that has no Dual Messenger user yet, so the setup row should show. */
    val canSetUpDualMessenger: Boolean = false,
    val isWorking: Boolean = false
)

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val settings: AppSettings,
    private val dualMessenger: DualMessengerSetup
) : ViewModel() {

    private val eventChannel = UiEventChannel()
    val events: Flow<UiEvent> = eventChannel.events

    private val working = kotlinx.coroutines.flow.MutableStateFlow(false)

    val state: StateFlow<ProfilesUiState> = combine(
        repository.profiles, repository.selected, settings.showAdvancedUsers, working
    ) { profiles, selected, advanced, busy ->
        ProfilesUiState(
            profiles = profiles,
            selected = selected,
            showAdvancedUsers = advanced,
            canSetUpDualMessenger = isSamsung && profiles.none { it.kind == ProfileKind.DUAL_MESSENGER } && profiles.isNotEmpty(),
            isWorking = busy
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ProfilesUiState())

    fun select(userId: Int) = repository.select(userId)

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }

    fun setShowAdvancedUsers(value: Boolean) = settings.setShowAdvancedUsers(value)

    fun enableDualMessenger() {
        if (working.value) return
        viewModelScope.launch {
            working.value = true
            eventChannel.send(UiEvent(Messages.activatingDualMessenger()))
            val failure = dualMessenger.enable()
            working.value = false
            eventChannel.send(UiEvent(if (failure == null) Messages.dualMessengerActivated() else Messages.dualMessengerFailed(failure)))
        }
    }

    private val isSamsung: Boolean get() = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
}
