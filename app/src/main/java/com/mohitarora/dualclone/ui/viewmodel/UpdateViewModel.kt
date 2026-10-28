package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.update.InstallRequest
import com.mohitarora.dualclone.update.UpdateRepository
import com.mohitarora.dualclone.update.UpdateState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Binds the Updates group in Settings.
 * Checking is user initiated. Download starts only from download(), after the changelog was shown.
 * Collect installRequests once and start the Intent from the Activity.
 */
@HiltViewModel
class UpdateViewModel @Inject constructor(
    private val repository: UpdateRepository
) : ViewModel() {

    val state: StateFlow<UpdateState> = repository.state
    val dailyCheckEnabled: StateFlow<Boolean> = repository.dailyCheckEnabled
    val currentVersion: String = repository.currentVersion

    private val installChannel = Channel<InstallRequest>(Channel.BUFFERED)

    /** One-shot requests for the screen to act on, for example launch the installer or open the unknown sources settings. */
    val installRequests: Flow<InstallRequest> = installChannel.receiveAsFlow()

    private val eventChannel = UiEventChannel()
    val events: Flow<UiEvent> = eventChannel.events

    fun check() {
        viewModelScope.launch { repository.check() }
    }

    fun download() {
        viewModelScope.launch { repository.download() }
    }

    fun install() {
        viewModelScope.launch { installChannel.send(repository.installRequest()) }
    }

    fun setDailyCheck(enabled: Boolean) = repository.setDailyCheck(enabled)

    /** Returns a Failed state to the last stable one so the screen can show its normal rows again. */
    fun dismissFailure() = repository.reset()
}
