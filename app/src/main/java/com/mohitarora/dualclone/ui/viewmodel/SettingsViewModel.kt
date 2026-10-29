package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.data.db.CommandLogEntry
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.data.repository.AuditLogRepository
import com.mohitarora.dualclone.data.repository.CloneStateRepository
import com.mohitarora.dualclone.data.repository.Reconciler
import com.mohitarora.dualclone.data.repository.ThemeRepository
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.ReconcileStatus
import com.mohitarora.dualclone.shizuku.ShizukuDiagnostics
import com.mohitarora.dualclone.shizuku.ShizukuManager
import com.mohitarora.dualclone.ui.theme.ThemeConfig
import com.mohitarora.dualclone.ui.theme.ThemeMode
import com.mohitarora.dualclone.ui.theme.ThemePreset
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val theme: ThemeRepository,
    private val settings: AppSettings,
    private val audit: AuditLogRepository,
    private val reconciler: Reconciler,
    private val clones: CloneStateRepository,
    private val shizuku: ShizukuManager
) : ViewModel() {

    private val eventChannel = UiEventChannel()
    val events: Flow<UiEvent> = eventChannel.events

    val themeConfig: StateFlow<ThemeConfig> = theme.config
    val shizukuState: StateFlow<ShizukuState> = shizuku.shizukuState
    val autoRefreshLauncher: StateFlow<Boolean> = settings.autoRefreshLauncher
    val lastReconcile: StateFlow<ReconcileStatus> = settings.lastReconcile

    val auditLog: StateFlow<List<CommandLogEntry>> = audit.recent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _diagnostics = MutableStateFlow(ShizukuDiagnostics())
    val diagnostics: StateFlow<ShizukuDiagnostics> = _diagnostics.asStateFlow()

    private val _isReconciling = MutableStateFlow(false)
    val isReconciling: StateFlow<Boolean> = _isReconciling.asStateFlow()

    fun setPreset(preset: ThemePreset) = theme.setPreset(preset)
    fun setCustomColor(colorHex: Long) = theme.setCustomColor(colorHex)
    fun setPureBlack(value: Boolean) = theme.setPureBlack(value)
    fun setSurfaceLift(value: Int) = theme.setSurfaceLift(value)
    fun setMode(mode: ThemeMode) = theme.setMode(mode)
    fun setAutoRefreshLauncher(value: Boolean) = settings.setAutoRefreshLauncher(value)
    fun requestShizukuPermission() = shizuku.requestPermission()
    fun refreshShizukuStatus() = shizuku.refreshStatus()

    /** Clears the command audit log. The log screen asks for confirmation first. */
    fun clearAuditLog() {
        viewModelScope.launch {
            audit.clear()
            eventChannel.send(UiEvent(Messages.auditCleared()))
        }
    }

    fun loadDiagnostics() {
        viewModelScope.launch { _diagnostics.value = withContext(Dispatchers.IO) { shizuku.diagnostics() } }
    }

    fun reconcileNow() {
        if (_isReconciling.value) return
        viewModelScope.launch {
            _isReconciling.value = true
            try {
                val report = reconciler.run()
                eventChannel.send(UiEvent(if (report.ok) Messages.reconcileDone() else Messages.reconcileFailed()))
            } finally {
                _isReconciling.value = false
            }
        }
    }

    fun refreshLauncher() {
        viewModelScope.launch {
            val ran = clones.refreshLauncher()
            eventChannel.send(UiEvent(if (ran) Messages.launcherRefreshed() else Messages.launcherRefreshUnavailable()))
        }
    }
}
