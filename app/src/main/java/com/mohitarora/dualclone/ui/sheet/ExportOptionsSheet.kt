package com.mohitarora.dualclone.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.domain.backup.BackupParts

/**
 * Chooses what goes into an export. Three quick presets fill the checkboxes, and the checkboxes can be tuned by hand.
 * Export stays disabled until at least one section is ticked.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExportOptionsSheet(
    onContinue: (BackupParts) -> Unit,
    onDismiss: () -> Unit,
    initial: BackupParts = BackupParts.EVERYTHING
) {
    var cloneList by rememberSaveable { mutableStateOf(initial.cloneList) }
    var presets by rememberSaveable { mutableStateOf(initial.appOpsPresets) }
    var settings by rememberSaveable { mutableStateOf(initial.settings) }
    val parts = BackupParts(cloneList, presets, settings)

    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.export_options_title)) {
        Text(
            text = stringResource(R.string.export_options_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
            listOf(
                R.string.export_preset_everything to BackupParts.EVERYTHING,
                R.string.export_preset_clones to BackupParts.CLONE_LIST_ONLY,
                R.string.export_preset_settings to BackupParts.SETTINGS_ONLY
            ).forEach { (label, preset) ->
                FilterChip(
                    selected = parts == preset,
                    onClick = {
                        cloneList = preset.cloneList
                        presets = preset.appOpsPresets
                        settings = preset.settings
                    },
                    shape = DcShapes.field,
                    label = { Text(stringResource(label), style = MaterialTheme.typography.labelLarge, maxLines = 1) }
                )
            }
        }
        Text(
            text = stringResource(R.string.export_options_contents),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        Column {
            OptionRow(
                title = stringResource(R.string.export_part_clones),
                body = stringResource(R.string.export_part_clones_body),
                checked = cloneList,
                onChange = { cloneList = it }
            )
            OptionRow(
                title = stringResource(R.string.export_part_presets),
                body = stringResource(R.string.export_part_presets_body),
                checked = presets,
                onChange = { presets = it }
            )
            OptionRow(
                title = stringResource(R.string.export_part_settings),
                body = stringResource(R.string.export_part_settings_body),
                checked = settings,
                onChange = { settings = it }
            )
        }
        if (!parts.isValid) {
            Text(
                text = stringResource(R.string.export_options_none),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error
            )
        }
        DcButtonRow {
            DcButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = DcButtonStyle.Outlined,
                modifier = Modifier.slot()
            )
            DcButton(
                text = stringResource(R.string.action_continue),
                onClick = { onContinue(parts) },
                enabled = parts.isValid,
                modifier = Modifier.slot()
            )
        }
    }
}

@Composable
private fun OptionRow(title: String, body: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = onChange)
            .padding(vertical = DcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(checked = checked, onCheckedChange = null)
        Column(modifier = Modifier.padding(start = DcSpacing.md)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
