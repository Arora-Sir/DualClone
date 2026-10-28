package com.mohitarora.dualclone.ui.sheet

import android.text.format.Formatter
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.update.UpdateState

/** Changelog, size and progress for one release. Download and Install are separate steps, so nothing installs unseen. */
@Composable
fun UpdateSheet(
    state: UpdateState,
    currentVersion: String,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.update_sheet_title)) {
        when (state) {
            is UpdateState.Available -> {
                VersionLine(currentVersion, state.version)
                Text(
                    text = stringResource(R.string.update_sheet_size, Formatter.formatShortFileSize(context, state.sizeBytes)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Changelog(state.changelog)
                DcButton(
                    text = stringResource(R.string.update_sheet_download),
                    onClick = onDownload,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            is UpdateState.Downloading -> {
                Text(stringResource(R.string.updates_state_downloading), style = MaterialTheme.typography.titleMedium)
                val progress = state.progress
                if (progress != null) {
                    LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium)
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
            }
            is UpdateState.Downloaded -> {
                VersionLine(currentVersion, state.version)
                Text(
                    text = stringResource(R.string.update_sheet_verified),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                DcButtonRow {
                    DcButton(stringResource(R.string.action_cancel), onDismiss, style = DcButtonStyle.Outlined, modifier = Modifier.slot())
                    DcButton(stringResource(R.string.update_sheet_install), onInstall, modifier = Modifier.slot())
                }
            }
            is UpdateState.Failed -> {
                DcInfoCard(
                    title = stringResource(R.string.update_sheet_failed),
                    body = state.reason.resolve(context),
                    tone = DcTone.Error
                ) {
                    DcButton(stringResource(R.string.action_retry), onRetry, style = DcButtonStyle.Tonal)
                }
            }
            else -> Unit
        }
    }
}

@Composable
private fun VersionLine(current: String, target: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
        DcBadge(text = current, tone = DcTone.Neutral)
        Text(stringResource(R.string.update_sheet_to), style = MaterialTheme.typography.labelMedium)
        DcBadge(text = target, tone = DcTone.Primary)
    }
}

@Composable
private fun Changelog(text: String) {
    DcCard(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh) {
        Column(modifier = Modifier.heightIn(max = 240.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = text.ifBlank { stringResource(R.string.update_sheet_no_changelog) },
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
