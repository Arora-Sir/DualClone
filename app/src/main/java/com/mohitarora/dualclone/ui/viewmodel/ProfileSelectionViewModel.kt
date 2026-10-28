package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.repo.ProfileRepository
import com.mohitarora.dualclone.receiver.PackageEventMonitor
import com.mohitarora.dualclone.shizuku.ShizukuManager
import com.mohitarora.dualclone.work.ReconcileScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Scope this to the activity with hiltViewModel(viewModelStoreOwner = activity) so every screen sees one
 * selected profile. It also starts the live package monitor and the periodic reconcile once per process.
 */
@HiltViewModel
class ProfileSelectionViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val shizuku: ShizukuManager,
    monitor: PackageEventMonitor,
    scheduler: ReconcileScheduler
) : ViewModel() {

    val profiles: StateFlow<List<Profile>> = repository.profiles
    val selected: StateFlow<Profile?> = repository.selected

    init {
        monitor.start()
        scheduler.ensurePeriodic()
        viewModelScope.launch {
            shizuku.shizukuState.collect { if (it is ShizukuState.Ready) repository.refresh() }
        }
    }

    fun select(userId: Int) = repository.select(userId)

    fun refresh() {
        viewModelScope.launch { repository.refresh() }
    }
}
