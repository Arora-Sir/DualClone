package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.data.db.AppDao
import com.mohitarora.dualclone.data.db.CloneStateDao
import com.mohitarora.dualclone.data.db.CommandLogEntry
import com.mohitarora.dualclone.data.repository.AuditLogRepository
import com.mohitarora.dualclone.shizuku.ShizukuDiagnostics
import com.mohitarora.dualclone.shizuku.ShizukuManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DevInfoUiState(
    val totalAppsInDb: Int = 0,
    val clonedAppsInDb: Int = 0,
    val recentLogs: List<CommandLogEntry> = emptyList()
)

@HiltViewModel
class DevInfoViewModel @Inject constructor(
    private val appDao: AppDao,
    private val cloneStateDao: CloneStateDao,
    private val audit: AuditLogRepository,
    val shizukuManager: ShizukuManager
) : ViewModel() {

    val recentLogs: StateFlow<List<CommandLogEntry>> = audit.recent
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _dbStats = MutableStateFlow(Pair(0, 0))
    val dbStats = _dbStats.asStateFlow()

    private val _diagnostics = MutableStateFlow(ShizukuDiagnostics())

    /** Real Shizuku UID and SELinux context, replacing the old hardcoded text. */
    val diagnostics: StateFlow<ShizukuDiagnostics> = _diagnostics.asStateFlow()

    init {
        refreshStats()
    }

    fun refreshStats() {
        viewModelScope.launch(Dispatchers.IO) {
            _dbStats.value = Pair(appDao.getAppCount(), cloneStateDao.installedCount())
            _diagnostics.value = shizukuManager.diagnostics()
        }
    }

    fun clearAuditLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            audit.clear()
        }
    }
}
