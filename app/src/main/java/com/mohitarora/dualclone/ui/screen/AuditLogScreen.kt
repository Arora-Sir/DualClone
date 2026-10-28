package com.mohitarora.dualclone.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTextStyles
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcConfirmDialog
import com.mohitarora.dualclone.core.designsystem.components.DcEmptyState
import com.mohitarora.dualclone.core.designsystem.components.DcSubScreen
import com.mohitarora.dualclone.data.db.CommandLogEntry
import java.text.DateFormat
import java.util.Date

/** Command audit log. Clear asks for confirmation here and in Settings through the same dialog. */
@Composable
fun AuditLogScreen(
    logs: List<CommandLogEntry>,
    onClear: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showClear by rememberSaveable { mutableStateOf(false) }
    DcSubScreen(
        title = stringResource(R.string.audit_title),
        onBack = onBack,
        modifier = modifier,
        actions = {
            IconButton(onClick = { showClear = true }, enabled = logs.isNotEmpty()) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.data_audit_clear_title))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            if (logs.isEmpty()) {
                DcEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.audit_empty_title),
                    body = stringResource(R.string.audit_empty_body),
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                LazyColumn(
                    modifier = Modifier.widthIn(max = DcSizes.settingsMaxWidth).fillMaxWidth(),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(DcSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
                ) {
                    item {
                        Text(
                            text = pluralStringResource(R.plurals.audit_entries, logs.size, logs.size),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    items(logs, key = { it.id }) { LogCard(it) }
                }
            }
        }
    }
    if (showClear) {
        DcConfirmDialog(
            title = stringResource(R.string.data_audit_clear_dialog_title),
            message = stringResource(R.string.data_audit_clear_dialog_body),
            confirmLabel = stringResource(R.string.action_clear),
            destructive = true,
            onConfirm = {
                showClear = false
                onClear()
            },
            onDismiss = { showClear = false }
        )
    }
}

@Composable
private fun LogCard(log: CommandLogEntry) {
    val time = remember(log.timestampMs) {
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(log.timestampMs))
    }
    DcCard {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = log.packageName,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.weight(1f)
            )
            DcBadge(
                text = stringResource(if (log.success) R.string.audit_success else R.string.audit_failed),
                tone = if (log.success) DcTone.Success else DcTone.Error,
                modifier = Modifier.padding(start = DcSpacing.sm)
            )
        }
        Text(
            text = log.command,
            style = DcTextStyles.mono,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = DcSpacing.sm)
        )
        if (log.result.isNotEmpty()) {
            Text(
                text = stringResource(R.string.audit_result, log.result),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = stringResource(R.string.audit_meta, time, log.userId, log.exitCode),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.outline
        )
    }
}
