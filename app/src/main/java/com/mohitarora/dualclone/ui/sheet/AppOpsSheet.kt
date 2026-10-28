package com.mohitarora.dualclone.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import androidx.compose.ui.text.input.KeyboardCapitalization
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTextStyles
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.util.PackageValidator

private class PresetOp(
    val opKey: String,
    @StringRes val title: Int,
    @StringRes val description: Int
)

private val PRESET_OPS = listOf(
    PresetOp("MANAGE_EXTERNAL_STORAGE", R.string.appops_files_title, R.string.appops_files_body),
    PresetOp("REQUEST_INSTALL_PACKAGES", R.string.appops_install_title, R.string.appops_install_body),
    PresetOp("WRITE_SETTINGS", R.string.appops_settings_title, R.string.appops_settings_body),
    PresetOp("SYSTEM_ALERT_WINDOW", R.string.appops_overlay_title, R.string.appops_overlay_body)
)

@Composable
fun AppOpsSheet(
    packageName: String,
    profileLabel: String,
    opsState: Map<String, String>,
    onToggleOp: (String, Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var customOp by rememberSaveable { mutableStateOf("") }
    var customError by rememberSaveable { mutableStateOf(false) }

    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.appops_title, profileLabel)) {
        Text(
            text = packageName,
            style = DcTextStyles.mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DcInfoCard(
            title = stringResource(R.string.appops_info_title),
            body = stringResource(R.string.appops_info_body),
            icon = Icons.Outlined.Info
        )
        PRESET_OPS.forEach { op ->
            AppOpRow(
                title = stringResource(op.title),
                description = stringResource(op.description),
                opKey = op.opKey,
                currentState = opsState[op.opKey] ?: "unknown",
                onToggle = { onToggleOp(op.opKey, it) }
            )
        }

        Text(
            text = stringResource(R.string.appops_custom_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = DcSpacing.sm)
                .semantics { heading() }
        )
        Text(
            text = stringResource(R.string.appops_custom_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = customOp,
            onValueChange = {
                customOp = it.uppercase()
                customError = false
            },
            placeholder = { Text(stringResource(R.string.appops_custom_hint)) },
            singleLine = true,
            isError = customError,
            supportingText = { if (customError) Text(stringResource(R.string.appops_custom_error)) },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            shape = DcShapes.field,
            modifier = Modifier.fillMaxWidth()
        )
        DcButton(
            text = stringResource(R.string.appops_custom_allow),
            onClick = {
                val trimmed = customOp.trim()
                if (PackageValidator.isValidAppOp(trimmed)) {
                    onToggleOp(trimmed, true)
                    customOp = ""
                } else {
                    customError = true
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun AppOpRow(
    title: String,
    description: String,
    opKey: String,
    currentState: String,
    onToggle: (Boolean) -> Unit
) {
    val isAllowed = currentState.contains("allow", ignoreCase = true)
    val isDenied = currentState.contains("deny", ignoreCase = true) || currentState.contains("ignore", ignoreCase = true)
    val (badgeText, tone) = when {
        isAllowed -> R.string.appops_state_allowed to DcTone.Success
        isDenied -> R.string.appops_state_denied to DcTone.Error
        else -> R.string.appops_state_default to DcTone.Neutral
    }
    val badge = stringResource(badgeText)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = isAllowed, role = Role.Switch, onValueChange = onToggle)
            .padding(vertical = DcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DcSpacing.xs)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
                DcBadge(text = badge, tone = tone)
                Text(text = opKey, style = DcTextStyles.monoSmall, color = MaterialTheme.colorScheme.primary)
            }
        }
        Switch(checked = isAllowed, onCheckedChange = null, modifier = Modifier.padding(start = DcSpacing.md))
    }
}
