package com.mohitarora.dualclone.ui.dialog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcConfirmDialog
import com.mohitarora.dualclone.core.designsystem.components.DcDialog
import com.mohitarora.dualclone.core.designsystem.components.DcDialogAction
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.domain.model.OpResult
import com.mohitarora.dualclone.domain.model.PmErrorCode
import com.mohitarora.dualclone.ui.viewmodel.BulkKind
import com.mohitarora.dualclone.ui.viewmodel.BulkSummary

/** States plainly that removing a clone deletes the data stored inside it. */
@Composable
fun RemoveCloneDialog(
    app: AppItem,
    profileLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DcConfirmDialog(
        title = stringResource(R.string.remove_dialog_title, app.appName),
        message = stringResource(R.string.remove_dialog_body, profileLabel),
        confirmLabel = stringResource(R.string.action_remove),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

@Composable
fun BulkRemoveDialog(
    count: Int,
    profileLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    DcConfirmDialog(
        title = pluralStringResource(R.plurals.bulk_remove_dialog_title, count, count),
        message = stringResource(R.string.bulk_remove_dialog_body, profileLabel),
        confirmLabel = stringResource(R.string.action_remove),
        destructive = true,
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

private fun reasonRes(result: OpResult): Int = when (result) {
    is OpResult.Success -> R.string.pm_error_unknown
    OpResult.ShizukuUnavailable -> R.string.pm_error_shizuku
    is OpResult.Failure -> when (result.code) {
        PmErrorCode.NOT_INSTALLED_FOR_USER -> R.string.pm_error_not_installed
        PmErrorCode.DEVICE_POLICY_BLOCKED -> R.string.pm_error_policy
        PmErrorCode.OWNER_BLOCKED -> R.string.pm_error_owner
        PmErrorCode.USER_RESTRICTED -> R.string.pm_error_restricted
        PmErrorCode.SECURITY_EXCEPTION -> R.string.pm_error_security
        PmErrorCode.TIMEOUT -> R.string.pm_error_timeout
        PmErrorCode.UNKNOWN -> R.string.pm_error_unknown
    }
}

/** End of a bulk run. Lists every failure with its reason and offers a retry for those packages only. */
@Composable
fun BulkSummaryDialog(
    summary: BulkSummary,
    appNames: Map<String, String>,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    val total = summary.items.size
    val failed = summary.failed
    val ok = total - failed.size
    val titleRes = if (summary.kind == BulkKind.CLONE) R.string.bulk_summary_cloned else R.string.bulk_summary_removed
    DcDialog(
        onDismiss = onDismiss,
        title = stringResource(titleRes, ok, total),
        actions = {
            if (failed.isNotEmpty()) {
                DcDialogAction(text = stringResource(R.string.bulk_summary_retry), onClick = onRetry)
            }
            DcDialogAction(text = stringResource(R.string.action_done), onClick = onDismiss)
        }
    ) {
        if (failed.isEmpty()) {
            Text(
                text = stringResource(R.string.bulk_summary_all_ok),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            Column(
                modifier = Modifier
                    .heightIn(max = 280.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
            ) {
                failed.forEach { item ->
                    Column {
                        Text(
                            text = appNames[item.packageName] ?: item.packageName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(reasonRes(item.result)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
