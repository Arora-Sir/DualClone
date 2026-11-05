package com.mohitarora.dualclone.ui.screen.home

import android.text.format.DateUtils
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.ui.dialog.HomeNotice
import com.mohitarora.dualclone.ui.viewmodel.HomeUiState

/** Situational notices for the selected profile. Each one is shown as a dialog, never as a card that shifts the list. */
@Composable
fun homeNotices(state: HomeUiState, actions: HomeActions, profileLabel: String?, selectedUserId: Int?): List<HomeNotice> {
    val notices = mutableListOf<HomeNotice>()
    if (state.isSelectedProfilePaused && profileLabel != null && selectedUserId != null) {
        notices += HomeNotice(
            key = NoticeDismissals.pausedKey(selectedUserId),
            icon = Icons.Default.PauseCircle,
            title = stringResource(R.string.banner_paused_title),
            body = stringResource(R.string.banner_paused_body, profileLabel)
        )
    }
    state.hostCopyUserIds.forEach { userId ->
        notices += HomeNotice(
            key = "hostcopy:$userId",
            icon = Icons.Default.ErrorOutline,
            title = stringResource(R.string.banner_host_copy_title),
            body = stringResource(R.string.banner_host_copy_body, userId),
            actionLabel = stringResource(R.string.banner_host_copy_action),
            onAction = { actions.onRepairHostCopy(userId) }
        )
    }
    if (state.isCloneStateStale) {
        notices += HomeNotice(
            key = "stale",
            icon = Icons.Default.Info,
            title = stringResource(R.string.banner_stale_title),
            body = stringResource(R.string.banner_stale_body),
            actionLabel = stringResource(R.string.action_retry),
            onAction = actions.onRefresh
        )
    }
    return notices
}

/** "Updated just now" under a minute, otherwise a relative time such as "5 minutes ago". */
@Composable
fun reconcileText(atMillis: Long, ok: Boolean): String {
    val now = System.currentTimeMillis()
    val relative = if (now - atMillis < 60_000L) {
        stringResource(R.string.time_just_now)
    } else {
        DateUtils.getRelativeTimeSpanString(atMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
    return stringResource(if (ok) R.string.reconcile_ok else R.string.reconcile_partial, relative)
}

@Composable
private fun relativeUpdated(atMillis: Long): String {
    val now = System.currentTimeMillis()
    return if (now - atMillis < 60_000L) stringResource(R.string.time_just_now)
    else DateUtils.getRelativeTimeSpanString(atMillis, now, DateUtils.MINUTE_IN_MILLIS).toString()
}

/** One quiet line that says when the profiles were last compared with the system. */
@Composable
fun ReconcileLine(atMillis: Long, ok: Boolean, problems: List<String> = emptyList(), modifier: Modifier = Modifier) {
    if (atMillis <= 0L) return
    val base = reconcileText(atMillis, ok)
    Text(
        text = if (!ok && problems.isNotEmpty()) stringResource(R.string.reconcile_paused, relativeUpdated(atMillis), problems.joinToString(". ")) else base,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier.padding(horizontal = DcSpacing.lg, vertical = DcSpacing.xs)
    )
}
