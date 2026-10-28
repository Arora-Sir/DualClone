package com.mohitarora.dualclone.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.common.UiEventChannel
import com.mohitarora.dualclone.core.common.UiText
import com.mohitarora.dualclone.data.backup.BackupRepository
import com.mohitarora.dualclone.data.backup.RestoreReport
import com.mohitarora.dualclone.data.backup.PreviewResult
import com.mohitarora.dualclone.domain.backup.BackupParts
import com.mohitarora.dualclone.domain.backup.RestorePlan
import com.mohitarora.dualclone.domain.model.OpResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject

sealed interface BackupPhase {
    data object Idle : BackupPhase
    data object Working : BackupPhase
    data object NeedsPassword : BackupPhase
    data object WrongPassword : BackupPhase
    data class Preview(val plan: RestorePlan) : BackupPhase
    data class Done(val cloned: Int, val failed: Int, val skipped: Int, val lines: List<RestoreLine> = emptyList()) : BackupPhase
    data class Error(val reason: String) : BackupPhase
}

enum class RestoreStatus { Done, Failed, Skipped }

/** One row of the finished restore, shown in place of the checkbox row it came from. */
data class RestoreLine(val label: String, val userId: Int?, val status: RestoreStatus, val reason: UiText? = null)

/**
 * Streams come from the Storage Access Framework, opened by the screen with ContentResolver.
 * The view model never touches a Context or a Uri.
 */
@HiltViewModel
class BackupViewModel @Inject constructor(
    private val repository: BackupRepository
) : ViewModel() {

    private val eventChannel = UiEventChannel()
    val events: Flow<UiEvent> = eventChannel.events

    private val _phase = MutableStateFlow<BackupPhase>(BackupPhase.Idle)
    val phase: StateFlow<BackupPhase> = _phase.asStateFlow()

    private var pendingBytes: ByteArray? = null

    /** Writes the backup to the stream and closes it. A blank password means no encryption. */
    fun export(output: OutputStream, password: CharArray?, parts: BackupParts = BackupParts()) {
        viewModelScope.launch {
            _phase.value = BackupPhase.Working
            try {
                withContext(Dispatchers.IO) { output.use { repository.export(it, password, parts) } }
                _phase.value = BackupPhase.Idle
                eventChannel.send(UiEvent(Messages.backupSaved()))
            } catch (t: Throwable) {
                _phase.value = BackupPhase.Idle
                eventChannel.send(UiEvent(Messages.backupFailed(t.message.orEmpty())))
            }
        }
    }

    /** Reads the picked file. The bytes are kept so a password can be supplied afterwards. */
    fun openBackup(input: InputStream) {
        viewModelScope.launch {
            _phase.value = BackupPhase.Working
            val bytes = try {
                withContext(Dispatchers.IO) { input.use { it.readBytes() } }
            } catch (t: Throwable) {
                _phase.value = BackupPhase.Error(t.message.orEmpty())
                return@launch
            }
            pendingBytes = bytes
            preview(null)
        }
    }

    fun submitPassword(password: CharArray) {
        viewModelScope.launch {
            _phase.value = BackupPhase.Working
            preview(password)
        }
    }

    private suspend fun preview(password: CharArray?) {
        val bytes = pendingBytes ?: return
        _phase.value = when (val r = repository.preview(ByteArrayInputStream(bytes), password)) {
            is PreviewResult.Ready -> BackupPhase.Preview(r.plan)
            PreviewResult.NeedsPassword -> BackupPhase.NeedsPassword
            PreviewResult.WrongPassword -> BackupPhase.WrongPassword
            is PreviewResult.Invalid -> BackupPhase.Error(r.reason)
        }
    }

    fun setCloneIncluded(packageName: String, userId: Int, include: Boolean) = updatePlan { plan ->
        plan.copy(clones = plan.clones.map { if (it.packageName == packageName && it.targetUserId == userId) it.copy(include = include) else it })
    }

    /** Settings checkbox: theme and update preference together. Ignored when the file has no settings part. */
    fun setSettingsIncluded(include: Boolean) = updatePlan {
        it.copy(applyTheme = include && it.theme != null, applyUpdateSettings = include && it.updateSettings != null)
    }

    fun setThemeIncluded(include: Boolean) = setSettingsIncluded(include)

    /** Presets checkbox. Ignored when the file has no presets part. */
    fun setPresetsIncluded(include: Boolean) = updatePlan { plan ->
        plan.copy(presets = plan.presets.map { it.copy(include = include && plan.hasAppOpsPresets) })
    }

    /** Clone list checkbox. Ignored when the file has no clone list part. */
    fun setClonesIncluded(include: Boolean) = updatePlan { plan ->
        plan.copy(clones = plan.clones.map { it.copy(include = include && plan.hasCloneList) })
    }

    private fun updatePlan(block: (RestorePlan) -> RestorePlan) {
        _phase.update { if (it is BackupPhase.Preview) BackupPhase.Preview(block(it.plan)) else it }
    }

    fun applyRestore() {
        val plan = (_phase.value as? BackupPhase.Preview)?.plan ?: return
        viewModelScope.launch {
            _phase.value = BackupPhase.Working
            val report = repository.apply(plan)
            val failed = report.cloneResults.count { it.result !is OpResult.Success } + report.presetsFailed
            val cloned = report.cloneResults.size - report.cloneResults.count { it.result !is OpResult.Success }
            _phase.value = BackupPhase.Done(cloned, failed, plan.skipped.size, resultLines(plan, report))
            pendingBytes = null
            eventChannel.send(UiEvent(Messages.restoreSummary(cloned, failed, plan.skipped.size)))
        }
    }

    private fun resultLines(plan: RestorePlan, report: RestoreReport): List<RestoreLine> {
        val outcomes = report.cloneOutcomes.associateBy { it.userId to it.packageName }
        val lines = ArrayList<RestoreLine>()
        plan.clones.forEach { clone ->
            val outcome = outcomes[clone.targetUserId to clone.packageName]?.result
            lines += when {
                !clone.include -> RestoreLine(clone.packageName, clone.targetUserId, RestoreStatus.Skipped, Messages.restoreNotSelected())
                outcome is OpResult.Success -> RestoreLine(clone.packageName, clone.targetUserId, RestoreStatus.Done)
                outcome is OpResult.Failure -> RestoreLine(clone.packageName, clone.targetUserId, RestoreStatus.Failed, Messages.plain(Messages.describe(outcome.code)))
                outcome == null -> RestoreLine(clone.packageName, clone.targetUserId, RestoreStatus.Failed, Messages.plain("No result was reported."))
                else -> RestoreLine(clone.packageName, clone.targetUserId, RestoreStatus.Failed, Messages.shizukuNotReady())
            }
        }
        plan.skipped.forEach { lines += RestoreLine(it.label, null, RestoreStatus.Skipped, Messages.restoreSkipReason(it.reason)) }
        if (plan.hasAppOpsPresets) {
            val applied = report.presetsApplied
            val failed = report.presetsFailed
            if (applied + failed > 0) {
                lines += RestoreLine(
                    "Permission presets", null,
                    if (failed == 0) RestoreStatus.Done else RestoreStatus.Failed,
                    Messages.plain("$applied applied, $failed failed")
                )
            }
        }
        if (plan.hasSettings) {
            val applied = report.themeApplied || report.updateSettingsApplied
            lines += RestoreLine(
                "Settings", null,
                if (applied) RestoreStatus.Done else RestoreStatus.Skipped,
                if (applied) null else Messages.restoreNotSelected()
            )
        }
        return lines
    }

    fun reset() {
        pendingBytes = null
        _phase.value = BackupPhase.Idle
    }
}
