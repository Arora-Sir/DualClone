package com.mohitarora.dualclone.ui.screen

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.mohitarora.dualclone.core.common.UiEvent
import kotlinx.coroutines.flow.Flow
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcDialog
import com.mohitarora.dualclone.core.designsystem.components.DcDialogAction
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.core.designsystem.components.DcSectionHeader
import com.mohitarora.dualclone.core.designsystem.components.DcSubScreen
import com.mohitarora.dualclone.domain.backup.BackupParts
import com.mohitarora.dualclone.domain.backup.RestorePlan
import com.mohitarora.dualclone.ui.sheet.ExportOptionsSheet
import com.mohitarora.dualclone.ui.viewmodel.BackupPhase
import com.mohitarora.dualclone.ui.viewmodel.RestoreStatus
import java.io.InputStream
import java.io.OutputStream

class BackupActions(
    val onExport: (OutputStream, CharArray?, BackupParts) -> Unit,
    val onOpenBackup: (InputStream) -> Unit,
    val onSubmitPassword: (CharArray) -> Unit,
    val onCloneIncluded: (String, Int, Boolean) -> Unit,
    val onClonesIncluded: (Boolean) -> Unit,
    val onPresetsIncluded: (Boolean) -> Unit,
    val onSettingsIncluded: (Boolean) -> Unit,
    val onApply: () -> Unit,
    val onReset: () -> Unit,
    val onBack: () -> Unit
)

/** Backup and restore through the Storage Access Framework. A restore always shows a dry run first. */
@Composable
fun BackupScreen(phase: BackupPhase, actions: BackupActions, events: Flow<UiEvent>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(events) { events.collect { snackbarHostState.showSnackbar(it.message.resolve(context)) } }
    var exportPassword by rememberSaveable { mutableStateOf("") }
    var askExportPassword by rememberSaveable { mutableStateOf(false) }
    var showExportOptions by rememberSaveable { mutableStateOf(false) }
    var exportClones by rememberSaveable { mutableStateOf(true) }
    var exportPresets by rememberSaveable { mutableStateOf(true) }
    var exportSettings by rememberSaveable { mutableStateOf(true) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val stream = uri?.let { context.contentResolver.openOutputStream(it) } ?: return@rememberLauncherForActivityResult
        actions.onExport(stream, exportPassword.takeIf { it.isNotBlank() }?.toCharArray(), BackupParts(exportClones, exportPresets, exportSettings))
        exportPassword = ""
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val stream = uri?.let { context.contentResolver.openInputStream(it) } ?: return@rememberLauncherForActivityResult
        actions.onOpenBackup(stream)
    }

    DcSubScreen(title = stringResource(R.string.backup_title), onBack = actions.onBack, snackbarHostState = snackbarHostState, modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (phase is BackupPhase.Preview) {
                PreviewContent(phase.plan, actions, Modifier.widthIn(max = DcSizes.settingsMaxWidth).fillMaxSize())
            } else if (phase is BackupPhase.Done && phase.lines.isNotEmpty()) {
                ResultContent(phase, actions.onReset, Modifier.widthIn(max = DcSizes.settingsMaxWidth).fillMaxSize())
            } else Column(
                modifier = Modifier
                    .widthIn(max = DcSizes.settingsMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(DcSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DcSpacing.lg)
            ) {
                when (phase) {
                    BackupPhase.Working -> Box(Modifier.fillMaxWidth().padding(DcSpacing.xxl), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    is BackupPhase.Done -> DcInfoCard(
                        title = stringResource(R.string.backup_done_title),
                        body = stringResource(R.string.backup_done_body, phase.cloned, phase.failed, phase.skipped),
                        tone = if (phase.failed == 0) DcTone.Success else DcTone.Warning,
                        action = { DcButton(stringResource(R.string.action_done), actions.onReset, style = DcButtonStyle.Tonal) }
                    )
                    is BackupPhase.Error -> DcInfoCard(
                        title = stringResource(R.string.backup_error_title),
                        body = phase.reason.ifBlank { stringResource(R.string.backup_error_body) },
                        tone = DcTone.Error,
                        action = { DcButton(stringResource(R.string.action_retry), actions.onReset, style = DcButtonStyle.Tonal) }
                    )
                    else -> IdleContent(
                        onExport = { showExportOptions = true },
                        onImport = { importLauncher.launch(arrayOf("*/*")) }
                    )
                }
            }
        }
    }

    if (showExportOptions) {
        ExportOptionsSheet(
            onContinue = { parts ->
                exportClones = parts.cloneList
                exportPresets = parts.appOpsPresets
                exportSettings = parts.settings
                showExportOptions = false
                askExportPassword = true
            },
            onDismiss = { showExportOptions = false }
        )
    }
    if (askExportPassword) {
        PasswordDialog(
            title = stringResource(R.string.backup_export_password_title),
            message = stringResource(R.string.backup_export_password_body),
            confirmLabel = stringResource(R.string.backup_export),
            required = false,
            onConfirm = { password ->
                exportPassword = password
                askExportPassword = false
                exportLauncher.launch("dualclone-backup.dcbackup")
            },
            onDismiss = { askExportPassword = false }
        )
    }
    if (phase == BackupPhase.NeedsPassword || phase == BackupPhase.WrongPassword) {
        PasswordDialog(
            title = stringResource(R.string.backup_import_password_title),
            message = stringResource(if (phase == BackupPhase.WrongPassword) R.string.backup_wrong_password else R.string.backup_import_password_body),
            confirmLabel = stringResource(R.string.action_continue),
            required = true,
            isError = phase == BackupPhase.WrongPassword,
            onConfirm = { actions.onSubmitPassword(it.toCharArray()) },
            onDismiss = actions.onReset
        )
    }
}

@Composable
private fun IdleContent(onExport: () -> Unit, onImport: () -> Unit) {
    DcCard {
        Text(stringResource(R.string.backup_export), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.backup_export_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = DcSpacing.sm)
        )
        DcButton(
            text = stringResource(R.string.backup_export),
            onClick = onExport,
            icon = { androidx.compose.material3.Icon(Icons.Default.FileUpload, null) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    DcCard {
        Text(stringResource(R.string.backup_import), style = MaterialTheme.typography.titleMedium)
        Text(
            text = stringResource(R.string.backup_import_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(vertical = DcSpacing.sm)
        )
        DcButton(
            text = stringResource(R.string.backup_import),
            onClick = onImport,
            style = DcButtonStyle.Tonal,
            icon = { androidx.compose.material3.Icon(Icons.Default.FileDownload, null) },
            modifier = Modifier.fillMaxWidth()
        )
    }
    DcInfoCard(
        title = stringResource(R.string.backup_limits_title),
        body = stringResource(R.string.backup_limits_body)
    )
}

@Composable
private fun PreviewContent(plan: RestorePlan, actions: BackupActions, modifier: Modifier = Modifier) {
    // NOTE: A lazy list keeps hundreds of rows cheap and the action buttons stay pinned below it.
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(DcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DcSpacing.xs)
        ) {
            item { DcSectionHeader(stringResource(R.string.backup_preview_heading)) }
            item { Text(text = contentsLine(plan), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            if (plan.clones.isEmpty() && plan.skipped.isEmpty() && plan.presets.isEmpty() && plan.theme == null) {
                item { Text(stringResource(R.string.backup_preview_empty), style = MaterialTheme.typography.bodyMedium) }
            }
            items(plan.clones, key = { "${it.targetUserId}/${it.packageName}" }) { clone ->
                CheckRow(
                    label = clone.packageName,
                    detail = stringResource(R.string.profiles_user_id, clone.targetUserId),
                    checked = clone.include,
                    onChange = { actions.onCloneIncluded(clone.packageName, clone.targetUserId, it) }
                )
            }
            item {
                SectionRow(R.string.export_part_clones, plan.hasCloneList, plan.clones.any { it.include }, actions.onClonesIncluded)
            }
            item {
                SectionRow(R.string.export_part_presets, plan.hasAppOpsPresets, plan.presets.any { it.include }, actions.onPresetsIncluded)
            }
            item {
                SectionRow(R.string.export_part_settings, plan.hasSettings, plan.applyTheme || plan.applyUpdateSettings, actions.onSettingsIncluded)
            }
            if (plan.skipped.isNotEmpty()) {
                item {
                    DcInfoCard(
                        title = pluralStringResource(R.plurals.backup_skipped_title, plan.skipped.size, plan.skipped.size),
                        body = plan.skipped.joinToString(", ") { it.label },
                        tone = DcTone.Warning
                    )
                }
            }
        }
        DcButtonRow(modifier = Modifier.padding(DcSpacing.lg)) {
            DcButton(stringResource(R.string.action_cancel), actions.onReset, style = DcButtonStyle.Outlined, modifier = Modifier.slot())
            DcButton(stringResource(R.string.backup_apply), actions.onApply, modifier = Modifier.slot())
        }
    }
}

/** The finished restore on the same list the preview used, so each row now says what happened to it. */
@Composable
private fun ResultContent(done: BackupPhase.Done, onDone: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(DcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DcSpacing.xs)
        ) {
            item {
                DcInfoCard(
                    title = stringResource(R.string.backup_done_title),
                    body = stringResource(R.string.backup_done_body, done.cloned, done.failed, done.skipped),
                    tone = if (done.failed == 0) DcTone.Success else DcTone.Warning
                )
            }
            items(done.lines, key = { "${it.userId}/${it.label}" }) { line ->
                ResultRow(
                    label = line.label,
                    detail = listOfNotNull(
                        line.userId?.let { context.getString(R.string.profiles_user_id, it) },
                        line.reason?.resolve(context)
                    ).joinToString(". ").ifEmpty { null },
                    status = line.status
                )
            }
        }
        DcButton(stringResource(R.string.action_done), onDone, modifier = Modifier.fillMaxWidth().padding(DcSpacing.lg))
    }
}

@Composable
private fun ResultRow(label: String, detail: String?, status: RestoreStatus) {
    val scheme = MaterialTheme.colorScheme
    val (icon, tint, description) = when (status) {
        RestoreStatus.Done -> Triple(Icons.Default.CheckCircle, scheme.primary, stringResource(R.string.backup_result_done))
        RestoreStatus.Failed -> Triple(Icons.Default.Error, scheme.error, stringResource(R.string.backup_result_failed))
        RestoreStatus.Skipped -> Triple(Icons.Default.RemoveCircleOutline, scheme.onSurfaceVariant, stringResource(R.string.backup_result_skipped))
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .padding(horizontal = DcSpacing.lg, vertical = DcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        androidx.compose.material3.Icon(icon, contentDescription = description, tint = tint)
        Column(Modifier.padding(start = DcSpacing.md)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = if (status == RestoreStatus.Failed) scheme.error else scheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun CheckRow(label: String, detail: String?, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = DcSpacing.lg, vertical = DcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(Modifier.padding(start = DcSpacing.md)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (detail != null) {
                Text(detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PasswordDialog(
    title: String,
    message: String,
    confirmLabel: String,
    required: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isError: Boolean = false
) {
    var value by rememberSaveable { mutableStateOf("") }
    DcDialog(
        onDismiss = onDismiss,
        title = title,
        actions = {
            DcDialogAction(stringResource(R.string.action_cancel), onDismiss)
            DcDialogAction(confirmLabel, { if (!required || value.isNotEmpty()) onConfirm(value) })
        }
    ) {
        Text(message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = value,
            onValueChange = { value = it },
            label = { Text(stringResource(R.string.backup_password_label)) },
            isError = isError,
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            shape = DcShapes.field,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

/** One sentence that says which sections the picked file holds. */
@Composable
private fun contentsLine(plan: RestorePlan): String {
    val p = plan.parts
    return stringResource(
        when {
            p.cloneList && p.appOpsPresets && p.settings -> R.string.backup_has_everything
            p.settings && !p.cloneList && !p.appOpsPresets -> R.string.backup_has_settings_only
            !p.settings && p.cloneList && p.appOpsPresets -> R.string.backup_has_clones_presets
            !p.settings && p.cloneList -> R.string.backup_has_clones_only
            !p.settings -> R.string.backup_has_presets_only
            p.cloneList -> R.string.backup_has_clones_settings
            else -> R.string.backup_has_presets_settings
        }
    )
}

/** A whole section of the restore. Disabled with a note when the file does not hold it. */
@Composable
private fun SectionRow(titleRes: Int, present: Boolean, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked && present, enabled = present, role = Role.Checkbox, onValueChange = onChange)
            .padding(horizontal = DcSpacing.md, vertical = DcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked && present, onCheckedChange = null, enabled = present)
        Column(Modifier.padding(start = DcSpacing.md)) {
            Text(stringResource(titleRes), style = MaterialTheme.typography.bodyLarge)
            if (!present) {
                Text(
                    stringResource(R.string.backup_section_absent),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
