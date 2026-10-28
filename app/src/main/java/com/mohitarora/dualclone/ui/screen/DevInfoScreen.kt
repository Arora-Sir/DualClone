package com.mohitarora.dualclone.ui.screen

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTextStyles
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import com.mohitarora.dualclone.core.designsystem.components.DcSubScreen
import com.mohitarora.dualclone.shizuku.ShizukuDiagnostics
import kotlinx.coroutines.launch

/** Developer diagnostics. Shows live Shizuku facts, so nothing here is hardcoded. */
@Composable
fun DevInfoScreen(
    totalApps: Int,
    cloneCount: Int,
    logCount: Int,
    diagnostics: ShizukuDiagnostics,
    onOpenAuditLog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val unknown = stringResource(R.string.diag_unknown)
    val uid = diagnostics.uid?.toString() ?: unknown
    val selinux = diagnostics.selinuxContext ?: unknown
    val api = diagnostics.apiVersion?.toString() ?: unknown
    val helper = stringResource(if (diagnostics.processHelperOk) R.string.diag_helper_ok else R.string.diag_helper_missing)
    val report = stringResource(R.string.diag_report, totalApps, cloneCount, logCount, uid, selinux, api)
    val copied = stringResource(R.string.diag_copied)

    DcSubScreen(
        title = stringResource(R.string.data_diagnostics_title),
        onBack = onBack,
        snackbarHostState = snackbar,
        modifier = modifier,
        actions = {
            IconButton(onClick = {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("DualClone", report))
                scope.launch { snackbar.showSnackbar(copied) }
            }) {
                Icon(Icons.Default.ContentCopy, contentDescription = stringResource(R.string.diag_copy))
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = DcSizes.settingsMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(DcSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(DcSpacing.lg)
            ) {
                DcCard {
                    Text(
                        text = stringResource(R.string.diag_shizuku_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Metric(stringResource(R.string.diag_uid), uid)
                    Metric(stringResource(R.string.diag_selinux), selinux)
                    Metric(stringResource(R.string.diag_api), api)
                    Metric(stringResource(R.string.diag_helper), helper)
                }
                DcCard {
                    Text(
                        text = stringResource(R.string.diag_storage_heading),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.semantics { heading() }
                    )
                    Metric(stringResource(R.string.diag_catalog), stringResource(R.string.diag_indexed, totalApps))
                    Metric(stringResource(R.string.diag_clones), stringResource(R.string.diag_tracked, cloneCount))
                }
                DcSettingsGroup {
                    DcSettingsRow(
                        title = stringResource(R.string.data_audit_title),
                        subtitle = stringResource(R.string.diag_audit_count, logCount),
                        icon = Icons.Default.History,
                        onClick = onOpenAuditLog
                    )
                }
            }
        }
    }
}

@Composable
private fun Metric(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = DcSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            style = DcTextStyles.mono,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = DcSpacing.md)
        )
    }
}
