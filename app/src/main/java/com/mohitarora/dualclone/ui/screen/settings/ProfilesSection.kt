package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsDivider
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import com.mohitarora.dualclone.core.designsystem.components.DcSwitchRow
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.ui.external.ExternalIntents

@Composable
fun ProfilesSection(
    state: SettingsUiState,
    actions: SettingsActions,
    onOpenShizukuSetup: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val visibleProfiles = state.profiles.filter { it.kind != ProfileKind.PRIMARY }
    val shizukuReady = state.shizuku is ShizukuState.Ready
    // NOTE: The Dual Messenger card is Samsung only and only when Shizuku works and no Dual Messenger user exists.
    val dualMissing = state.canSetUpDualMessenger && shizukuReady

    DcSettingsGroup(header = stringResource(R.string.settings_section_profiles), modifier = modifier) {
        DcSettingsRow(
            title = stringResource(R.string.profiles_shizuku_title),
            subtitle = stringResource(shizukuSummary(state.shizuku)),
            icon = Icons.Default.Security,
            onClick = if (shizukuReady) ({ ExternalIntents.openShizuku(context); Unit }) else onOpenShizukuSetup,
            trailing = {
                DcBadge(
                    text = stringResource(shizukuBadge(state.shizuku)),
                    tone = if (shizukuReady) DcTone.Success else DcTone.Warning,
                    showDot = true
                )
            }
        )
        if (visibleProfiles.isEmpty()) {
            DcSettingsDivider()
            DcSettingsRow(
                title = stringResource(R.string.profiles_empty),
                icon = Icons.Default.PersonOutline
            )
        }
        visibleProfiles.forEach { profile ->
            DcSettingsDivider()
            ProfileRow(profile)
        }
        actions.onShowAdvancedUsersChange?.let { change ->
            DcSettingsDivider()
            DcSwitchRow(
                title = stringResource(R.string.profiles_advanced_title),
                subtitle = stringResource(R.string.profiles_advanced_summary),
                icon = Icons.Default.People,
                checked = state.showAdvancedUsers,
                onCheckedChange = change
            )
        }
        if (state.isSamsung) {
            DcSettingsDivider()
            DcSettingsRow(
                title = stringResource(R.string.profiles_knox_title),
                subtitle = stringResource(R.string.profiles_knox_summary),
                icon = Icons.Default.Lock,
                onClick = { ExternalIntents.openSecureFolder(context) },
                trailing = {
                    Icon(
                        Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = stringResource(R.string.action_open),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
        }
    }
    if (dualMissing) {
        DcInfoCard(
            title = stringResource(R.string.profiles_dual_missing_title),
            body = stringResource(R.string.profiles_dual_missing_body),
            tone = DcTone.Warning,
            icon = Icons.Default.Warning,
            modifier = Modifier.padding(top = DcSpacing.md)
        ) {
            DcButton(
                text = stringResource(R.string.profiles_dual_missing_action),
                onClick = actions.onEnableDualMessenger,
                style = DcButtonStyle.Tonal
            )
        }
    }
}

@Composable
private fun ProfileRow(profile: Profile) {
    val (labelRes, icon) = profileKindPresentation(profile.kind)
    DcSettingsRow(
        title = stringResource(labelRes),
        subtitle = stringResource(R.string.profiles_user_id, profile.userId),
        icon = icon,
        trailing = {
            val (text, tone) = when {
                profile.isRestricted -> R.string.profiles_chip_restricted to DcTone.Neutral
                profile.isPaused -> R.string.profiles_chip_paused to DcTone.Warning
                profile.isRunning -> R.string.profiles_chip_running to DcTone.Success
                else -> R.string.profiles_chip_stopped to DcTone.Neutral
            }
            DcBadge(text = stringResource(text), tone = tone, showDot = true)
        }
    )
}

internal fun profileKindPresentation(kind: ProfileKind): Pair<Int, ImageVector> = when (kind) {
    ProfileKind.PRIMARY -> R.string.profile_kind_primary to Icons.Default.Person
    ProfileKind.DUAL_MESSENGER -> R.string.profile_kind_dual to Icons.Default.ContentCopy
    ProfileKind.WORK -> R.string.profile_kind_work to Icons.Default.Business
    ProfileKind.SECURE_FOLDER -> R.string.profile_kind_secure to Icons.Default.Shield
    ProfileKind.OTHER_USER -> R.string.profile_kind_other to Icons.Default.PersonOutline
}

internal fun shizukuSummary(state: ShizukuState): Int = when (state) {
    is ShizukuState.Ready -> R.string.shizuku_summary_ready
    is ShizukuState.Checking -> R.string.shizuku_summary_checking
    is ShizukuState.PermissionRequired -> R.string.shizuku_summary_permission
    is ShizukuState.User95Missing -> R.string.shizuku_summary_dual_missing
    is ShizukuState.Unavailable -> R.string.shizuku_summary_stopped
}

internal fun shizukuBadge(state: ShizukuState): Int = when (state) {
    is ShizukuState.Ready -> R.string.shizuku_badge_ready
    is ShizukuState.Checking -> R.string.shizuku_badge_checking
    is ShizukuState.PermissionRequired -> R.string.shizuku_badge_grant
    is ShizukuState.User95Missing -> R.string.shizuku_badge_setup
    is ShizukuState.Unavailable -> R.string.shizuku_badge_stopped
}
