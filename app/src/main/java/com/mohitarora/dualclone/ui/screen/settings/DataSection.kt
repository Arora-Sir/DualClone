package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsDivider
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import com.mohitarora.dualclone.core.designsystem.components.DcSwitchRow

@Composable
internal fun Chevron() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

/** Maintenance rows. Rows whose callback is null are hidden. Audit log, diagnostics and licenses sit behind More. */
@Composable
fun DataSection(
    state: SettingsUiState,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val rows = buildList<@Composable () -> Unit> {
        state.reconcileSummary?.let { summary ->
            add {
                DcSettingsRow(
                    title = stringResource(R.string.data_reconcile_title),
                    subtitle = summary,
                    icon = Icons.Default.Sync
                )
            }
        }
        actions.onOpenBackup?.let { open ->
            add {
                DcSettingsRow(
                    title = stringResource(R.string.data_backup_title),
                    subtitle = stringResource(R.string.data_backup_summary),
                    icon = Icons.Default.SaveAlt,
                    onClick = open,
                    trailing = { Chevron() }
                )
            }
        }
        actions.onOpenOrphans?.let { open ->
            add {
                DcSettingsRow(
                    title = stringResource(R.string.data_orphans_title),
                    subtitle = stringResource(R.string.data_orphans_summary),
                    icon = Icons.Default.LayersClear,
                    onClick = open,
                    trailing = { Chevron() }
                )
            }
        }
        actions.onRefreshLauncher?.let { refresh ->
            add {
                DcSettingsRow(
                    title = stringResource(R.string.data_refresh_launcher_title),
                    subtitle = stringResource(R.string.data_refresh_launcher_summary),
                    icon = Icons.Default.Home,
                    onClick = refresh
                )
            }
        }
        actions.onAutoRefreshLauncherChange?.let { change ->
            add {
                DcSwitchRow(
                    title = stringResource(R.string.data_auto_refresh_title),
                    subtitle = stringResource(R.string.data_auto_refresh_summary),
                    icon = Icons.Default.Refresh,
                    checked = state.autoRefreshLauncher,
                    onCheckedChange = change
                )
            }
        }
        actions.onOpenMore?.let { open ->
            add {
                DcSettingsRow(
                    title = stringResource(R.string.more_row_title),
                    subtitle = stringResource(R.string.more_row_summary),
                    icon = Icons.Default.MoreHoriz,
                    onClick = open,
                    trailing = { Chevron() }
                )
            }
        }
    }

    DcSettingsGroup(header = stringResource(R.string.settings_section_data), modifier = modifier) {
        rows.forEachIndexed { index, row ->
            if (index > 0) DcSettingsDivider()
            row()
        }
    }
}
