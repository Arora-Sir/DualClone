package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Update
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material.icons.filled.Link
import com.mohitarora.dualclone.ui.external.ExternalIntents
import com.mohitarora.dualclone.ui.external.ProjectLinks
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsDivider
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import com.mohitarora.dualclone.core.designsystem.components.DcSwitchRow
import com.mohitarora.dualclone.ui.sheet.UpdateSheet
import com.mohitarora.dualclone.update.UpdateState

/** Updates and About. Checking is always user initiated and the privacy note says what uses the internet. */
@Composable
fun UpdatesSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val update = state.update
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var awaiting by rememberSaveable { mutableStateOf(false) }

    // NOTE: The sheet opens by itself only when the user's own check finds a release.
    LaunchedEffect(update) {
        if (awaiting) {
            when (update) {
                is UpdateState.Available -> { showSheet = true; awaiting = false }
                UpdateState.UpToDate, is UpdateState.Failed -> awaiting = false
                else -> Unit
            }
        }
    }

    DcSettingsGroup(header = stringResource(R.string.settings_section_updates), modifier = modifier) {
        DcSettingsRow(
            title = stringResource(R.string.updates_version_title),
            subtitle = stringResource(R.string.updates_version_summary, state.currentVersion, com.mohitarora.dualclone.BuildConfig.VERSION_CODE),
            icon = Icons.Default.Info
        )
        DcSettingsDivider()
        DcSettingsRow(
            title = stringResource(R.string.updates_check_title),
            subtitle = updateSubtitle(update, context),
            icon = if (update is UpdateState.Available || update is UpdateState.Downloaded) Icons.Default.SystemUpdate else Icons.Default.Update,
            enabled = update !is UpdateState.Checking,
            onClick = {
                when (update) {
                    is UpdateState.Available, is UpdateState.Downloading, is UpdateState.Downloaded -> showSheet = true
                    is UpdateState.Failed -> { actions.onDismissUpdateFailure(); awaiting = true; actions.onCheckUpdate() }
                    else -> { awaiting = true; actions.onCheckUpdate() }
                }
            },
            trailing = {
                when (update) {
                    UpdateState.Checking -> CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    is UpdateState.Available, is UpdateState.Downloaded -> DcBadge(stringResource(R.string.updates_badge_new), tone = DcTone.Primary)
                    is UpdateState.Failed -> DcBadge(stringResource(R.string.audit_failed), tone = DcTone.Error)
                    else -> Unit
                }
            }
        )
        DcSettingsDivider()
        DcSettingsRow(
            title = stringResource(R.string.updates_obtainium_title),
            subtitle = stringResource(R.string.updates_obtainium_summary),
            icon = Icons.Default.Link,
            onClick = { ExternalIntents.openUrl(context, ProjectLinks.obtainium) },
            trailing = { OpenGlyph() }
        )
        DcSettingsDivider()
        DcSwitchRow(
            title = stringResource(R.string.updates_daily_title),
            subtitle = stringResource(R.string.updates_daily_summary),
            icon = Icons.Default.Schedule,
            checked = state.dailyUpdateCheck,
            onCheckedChange = actions.onDailyUpdateCheckChange
        )
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DcSpacing.lg, vertical = DcSpacing.md)) {
            Text(
                text = stringResource(R.string.updates_privacy_note),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }

    if (showSheet && (update is UpdateState.Available || update is UpdateState.Downloading || update is UpdateState.Downloaded || update is UpdateState.Failed)) {
        UpdateSheet(
            state = update,
            currentVersion = state.currentVersion,
            onDownload = actions.onDownloadUpdate,
            onInstall = actions.onInstallUpdate,
            onRetry = { actions.onDismissUpdateFailure(); actions.onCheckUpdate() },
            onDismiss = { showSheet = false }
        )
    }
}

@Composable
private fun updateSubtitle(update: UpdateState, context: android.content.Context): String = when (update) {
    UpdateState.Idle -> stringResource(R.string.updates_check_summary)
    UpdateState.Checking -> stringResource(R.string.updates_state_checking)
    UpdateState.UpToDate -> stringResource(R.string.updates_state_up_to_date)
    is UpdateState.Available -> stringResource(R.string.updates_state_available, update.version)
    is UpdateState.Downloading -> update.progress?.let { stringResource(R.string.updates_state_downloading_percent, (it * 100).toInt()) }
        ?: stringResource(R.string.updates_state_downloading)
    is UpdateState.Downloaded -> stringResource(R.string.updates_state_downloaded, update.version)
    is UpdateState.Failed -> update.reason.resolve(context)
}
