package com.mohitarora.dualclone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcStatus
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind

private fun profileLabelRes(kind: ProfileKind): Int = when (kind) {
    ProfileKind.PRIMARY -> R.string.profile_kind_primary
    ProfileKind.DUAL_MESSENGER -> R.string.profile_kind_dual
    ProfileKind.WORK -> R.string.profile_kind_work
    ProfileKind.SECURE_FOLDER -> R.string.profile_kind_secure
    ProfileKind.OTHER_USER -> R.string.profile_kind_other
}

/**
 * Profile chips. Selecting Secure Folder never changes the clone target, it only reports a tap so
 * Home can show the Knox guidance card. No shell command is ever sent to that profile.
 */
@Composable
fun ProfileSelectorRow(
    profiles: List<Profile>,
    selectedUserId: Int?,
    onProfileSelected: (Profile) -> Unit,
    onSecureFolderTapped: () -> Unit,
    secureFolderActive: Boolean,
    modifier: Modifier = Modifier
) {
    if (profiles.isEmpty()) return
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = DcSpacing.xs)
    ) {
        Text(
            text = stringResource(R.string.profile_target_heading),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = DcSpacing.lg, vertical = DcSpacing.xs)
        )
        val listState = rememberLazyListState()
        val selectedIndex = profiles.indexOfFirst {
            if (it.kind == ProfileKind.SECURE_FOLDER) secureFolderActive else selectedUserId == it.userId
        }
        // NOTE: Start at the left padding. Scroll only when the selected chip is not fully visible.
        LaunchedEffect(profiles.size, selectedIndex) {
            snapshotFlow { listState.layoutInfo.visibleItemsInfo.isNotEmpty() }.first { it }
            delay(250)
            listState.scrollToItem(0)
            val info = listState.layoutInfo
            val item = info.visibleItemsInfo.firstOrNull { it.index == selectedIndex }
            if (item != null) {
                val overflow = item.offset + item.size - info.viewportEndOffset
                if (overflow > 0) listState.animateScrollBy(overflow.toFloat() + 48f)
            }
        }
        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = DcSpacing.lg),
            horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm)
        ) {
            items(items = profiles, key = { it.userId }) { profile ->
                val isSecure = profile.kind == ProfileKind.SECURE_FOLDER
                val selected = if (isSecure) secureFolderActive else selectedUserId == profile.userId
                val pausedText = stringResource(R.string.profiles_chip_paused)
                FilterChip(
                    selected = selected,
                    onClick = { if (isSecure) onSecureFolderTapped() else onProfileSelected(profile) },
                    shape = DcShapes.field,
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        selectedLeadingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    label = {
                        Text(
                            text = stringResource(profileLabelRes(profile.kind)),
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1
                        )
                    },
                    leadingIcon = when {
                        isSecure -> ({ Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp)) })
                        selected -> ({ Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) })
                        else -> null
                    },
                    trailingIcon = if (profile.isPaused) {
                        {
                            Icon(
                                imageVector = Icons.Default.PauseCircle,
                                contentDescription = pausedText,
                                tint = DcStatus.colors(DcTone.Warning).accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }
    }
}
