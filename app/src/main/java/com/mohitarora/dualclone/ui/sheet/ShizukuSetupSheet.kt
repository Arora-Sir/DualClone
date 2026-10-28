package com.mohitarora.dualclone.ui.sheet

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.ui.external.ExternalIntents
import com.mohitarora.dualclone.ui.external.ProjectLinks

private enum class StepStatus { Done, Current, Upcoming }

/**
 * First run onboarding and the permanent Shizuku help sheet. Four steps follow the real state:
 * install Shizuku, start it, grant permission and choose profiles. Each step shows its action only while it is current.
 */
@Composable
fun ShizukuSetupSheet(
    state: ShizukuState,
    onRequestPermission: () -> Unit,
    onRefresh: () -> Unit,
    onDismiss: () -> Unit,
    isSamsung: Boolean = Build.MANUFACTURER.equals("samsung", ignoreCase = true)
) {
    val context = LocalContext.current
    val installed = remember(state) { ExternalIntents.isShizukuInstalled(context) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val started = state is ShizukuState.PermissionRequired || state is ShizukuState.User95Missing || state is ShizukuState.Ready
    val granted = state is ShizukuState.User95Missing || state is ShizukuState.Ready
    val finished = state is ShizukuState.Ready
    // NOTE: A running Shizuku is installed by definition, even when its launcher package differs (forks).
    val statuses = firstCurrent(installed || started, started, granted, finished)

    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.setup_title)) {
        Text(
            text = stringResource(if (finished) R.string.setup_intro_ready else R.string.setup_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        SetupStep(
            number = 1,
            title = stringResource(R.string.setup_step_install_title),
            body = stringResource(R.string.setup_step_install_body),
            status = statuses[0]
        ) {
            DcButtonRow {
                DcButton(
                    text = stringResource(R.string.setup_get_shizuku),
                    onClick = { ExternalIntents.openUrl(context, ProjectLinks.SHIZUKU_RELEASES) },
                    modifier = Modifier.slot()
                )
                DcButton(
                    text = stringResource(R.string.setup_get_fork),
                    onClick = { ExternalIntents.openUrl(context, ProjectLinks.SHIZUKU_FORK) },
                    style = DcButtonStyle.Outlined,
                    modifier = Modifier.slot()
                )
            }
        }
        SetupStep(
            number = 2,
            title = stringResource(R.string.setup_step_start_title),
            body = stringResource(R.string.setup_step_start_body),
            status = statuses[1]
        ) {
            DcButtonRow {
                DcButton(
                    text = stringResource(R.string.profiles_shizuku_open),
                    onClick = { ExternalIntents.openShizuku(context) },
                    modifier = Modifier.slot()
                )
                DcButton(
                    text = stringResource(R.string.setup_wireless_debugging),
                    onClick = { ExternalIntents.openDeveloperSettings(context) },
                    style = DcButtonStyle.Outlined,
                    modifier = Modifier.slot()
                )
            }
        }
        SetupStep(
            number = 3,
            title = stringResource(R.string.setup_step_grant_title),
            body = stringResource(R.string.setup_step_grant_body),
            status = statuses[2]
        ) {
            DcButton(
                text = stringResource(R.string.setup_grant),
                onClick = {
                    permissionRequested = true
                    onRequestPermission()
                },
                modifier = Modifier.fillMaxWidth()
            )
            if (permissionRequested && state is ShizukuState.PermissionRequired) {
                DcInfoCard(
                    title = stringResource(R.string.setup_denied_title),
                    body = stringResource(R.string.setup_denied_body),
                    tone = DcTone.Warning
                ) {
                    DcButton(
                        text = stringResource(R.string.profiles_shizuku_open),
                        onClick = { ExternalIntents.openShizuku(context) },
                        style = DcButtonStyle.Tonal
                    )
                }
            }
        }
        SetupStep(
            number = 4,
            title = stringResource(R.string.setup_step_profiles_title),
            body = stringResource(
                when {
                    finished -> R.string.setup_step_profiles_ready
                    state is ShizukuState.User95Missing && isSamsung -> R.string.setup_step_profiles_dual_missing
                    isSamsung -> R.string.setup_step_profiles_body
                    else -> R.string.setup_step_profiles_not_samsung
                }
            ),
            status = statuses[3]
        ) {}

        DcButtonRow {
            DcButton(
                text = stringResource(R.string.action_check_again),
                onClick = onRefresh,
                style = DcButtonStyle.Outlined,
                modifier = Modifier.slot()
            )
            DcButton(
                text = stringResource(R.string.action_done),
                onClick = onDismiss,
                modifier = Modifier.slot()
            )
        }
    }
}

private fun firstCurrent(vararg done: Boolean): List<StepStatus> {
    val firstOpen = done.indexOfFirst { !it }
    return done.mapIndexed { index, isDone ->
        when {
            isDone -> StepStatus.Done
            index == firstOpen -> StepStatus.Current
            else -> StepStatus.Upcoming
        }
    }
}

@Composable
private fun SetupStep(
    number: Int,
    title: String,
    body: String,
    status: StepStatus,
    actions: @Composable ColumnScope.() -> Unit
) {
    val doneText = stringResource(R.string.setup_step_done)
    val currentText = stringResource(R.string.setup_step_current)
    val upcomingText = stringResource(R.string.setup_step_upcoming)
    val statusText = when (status) {
        StepStatus.Done -> doneText
        StepStatus.Current -> currentText
        StepStatus.Upcoming -> upcomingText
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = false) { stateDescription = statusText }
    ) {
        val circleColor = when (status) {
            StepStatus.Done -> MaterialTheme.colorScheme.primary
            StepStatus.Current -> MaterialTheme.colorScheme.primaryContainer
            StepStatus.Upcoming -> MaterialTheme.colorScheme.surfaceContainerHighest
        }
        val onCircle = when (status) {
            StepStatus.Done -> MaterialTheme.colorScheme.onPrimary
            StepStatus.Current -> MaterialTheme.colorScheme.onPrimaryContainer
            StepStatus.Upcoming -> MaterialTheme.colorScheme.onSurfaceVariant
        }
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(circleColor, CircleShape)
                .semantics { contentDescription = "$number, $statusText" },
            contentAlignment = Alignment.Center
        ) {
            if (status == StepStatus.Done) {
                Icon(Icons.Default.Check, contentDescription = null, tint = onCircle, modifier = Modifier.size(18.dp))
            } else {
                Text(text = number.toString(), style = MaterialTheme.typography.labelLarge, color = onCircle)
            }
        }
        Spacer(Modifier.width(DcSpacing.md))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (status == StepStatus.Upcoming) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (status == StepStatus.Current) {
                Column(
                    modifier = Modifier.padding(top = DcSpacing.xs),
                    verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
                ) { actions() }
            }
        }
    }
}
