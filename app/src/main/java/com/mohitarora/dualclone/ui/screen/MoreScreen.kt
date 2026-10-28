package com.mohitarora.dualclone.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcConfirmDialog
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsDivider
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import com.mohitarora.dualclone.core.designsystem.components.DcSubScreen
import com.mohitarora.dualclone.core.designsystem.rememberDcHaptics
import com.mohitarora.dualclone.ui.screen.settings.Chevron
import com.mohitarora.dualclone.ui.screen.settings.DiagnosticsGate
import kotlinx.coroutines.flow.Flow

class MoreActions(
    val onBack: () -> Unit,
    val onOpenAuditLog: () -> Unit,
    val onClearAuditLog: () -> Unit,
    val onOpenDiagnostics: () -> Unit,
    val onOpenLicenses: (() -> Unit)? = null
)

/** The rarely used rows. Diagnostics appear only after the seven tap unlock in Settings. */
@Composable
fun MoreScreen(actions: MoreActions, events: Flow<UiEvent>, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val haptics = rememberDcHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    var showClear by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(events) { events.collect { snackbarHostState.showSnackbar(it.message.resolve(context)) } }

    DcSubScreen(
        title = stringResource(R.string.more_title),
        onBack = actions.onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = DcSizes.settingsMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(DcSpacing.lg)
            ) {
                DcSettingsGroup {
                    DcSettingsRow(
                        title = stringResource(R.string.data_audit_title),
                        subtitle = stringResource(R.string.data_audit_summary),
                        icon = Icons.Default.History,
                        onClick = actions.onOpenAuditLog,
                        trailing = { Chevron() }
                    )
                    DcSettingsDivider()
                    DcSettingsRow(
                        title = stringResource(R.string.data_audit_clear_title),
                        subtitle = stringResource(R.string.data_audit_clear_summary),
                        icon = Icons.Default.DeleteSweep,
                        onClick = { showClear = true }
                    )
                    if (DiagnosticsGate.unlocked) {
                        DcSettingsDivider()
                        DcSettingsRow(
                            title = stringResource(R.string.data_diagnostics_title),
                            subtitle = stringResource(R.string.data_diagnostics_summary),
                            icon = Icons.Default.BugReport,
                            onClick = actions.onOpenDiagnostics,
                            trailing = { Chevron() }
                        )
                    }
                    actions.onOpenLicenses?.let { open ->
                        DcSettingsDivider()
                        DcSettingsRow(
                            title = stringResource(R.string.licenses_row_title),
                            subtitle = stringResource(R.string.licenses_row_summary),
                            icon = Icons.Default.Verified,
                            onClick = open,
                            trailing = { Chevron() }
                        )
                    }
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
                haptics.success()
                actions.onClearAuditLog()
            },
            onDismiss = { showClear = false }
        )
    }
}
