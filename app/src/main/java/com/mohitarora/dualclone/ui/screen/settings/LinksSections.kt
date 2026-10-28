package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Code
import com.mohitarora.dualclone.core.designsystem.components.DcHeartbeatIcon
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PrivacyTip
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.BuildConfig
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsDivider
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import com.mohitarora.dualclone.ui.external.ExternalIntents
import com.mohitarora.dualclone.ui.external.ProjectLinks

@Composable
private fun OpenGlyph() {
    Icon(
        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
        contentDescription = stringResource(R.string.action_open),
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    )
}

@Composable
fun SupportSection(
    onOpenSupport: () -> Unit,
    onOpenAttribution: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    DcSettingsGroup(header = stringResource(R.string.settings_section_support), modifier = modifier) {
        DcSettingsRow(
            title = stringResource(R.string.support_row_title),
            subtitle = stringResource(R.string.support_row_summary),
            leading = { DcHeartbeatIcon() },
            onClick = onOpenSupport,
            trailing = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
        DcSettingsDivider(tonal = true)
        DcSettingsRow(
            title = stringResource(R.string.attribution_row_title),
            subtitle = stringResource(R.string.attribution_row_summary),
            icon = Icons.Default.Code,
            onClick = onOpenAttribution,
            trailing = { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
        )
        DcSettingsDivider()
        DcSettingsRow(
            title = stringResource(R.string.issue_row_title),
            subtitle = stringResource(R.string.issue_row_summary),
            icon = Icons.Default.BugReport,
            onClick = { ExternalIntents.openUrl(context, ProjectLinks.NEW_ISSUE) },
            trailing = { OpenGlyph() }
        )
        DcSettingsDivider()
        DcSettingsRow(
            title = stringResource(R.string.privacy_row_title),
            subtitle = stringResource(R.string.privacy_row_summary),
            icon = Icons.Default.PrivacyTip,
            onClick = { ExternalIntents.openUrl(context, ProjectLinks.PRIVACY) },
            trailing = { OpenGlyph() }
        )
    }
}

@Composable
fun DeveloperSection(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth().padding(top = DcSpacing.md)) {
        DcCard {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { ExternalIntents.openUrl(context, ProjectLinks.GITHUB_PROFILE) }
                    .heightIn(min = 56.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.dev_avatar_mohit),
                    contentDescription = stringResource(R.string.developer_avatar_description),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                )
                Spacer(Modifier.width(DcSpacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.developer_name),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.developer_role),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                modifier = Modifier.padding(top = DcSpacing.md),
                verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
            ) {
                DcButtonRow {
                    DcButton(
                        text = stringResource(R.string.developer_link_github),
                        onClick = { ExternalIntents.openUrl(context, ProjectLinks.GITHUB_PROFILE) },
                        style = DcButtonStyle.Tonal,
                        icon = { Icon(painterResource(R.drawable.ic_brand_github), null) },
                        modifier = Modifier.slot()
                    )
                    DcButton(
                        text = stringResource(R.string.developer_link_linkedin),
                        onClick = { ExternalIntents.openUrl(context, ProjectLinks.LINKEDIN) },
                        style = DcButtonStyle.Tonal,
                        icon = { Icon(painterResource(R.drawable.ic_brand_linkedin), null) },
                        modifier = Modifier.slot()
                    )
                }
                DcButtonRow {
                    DcButton(
                        text = stringResource(R.string.developer_link_website),
                        onClick = { ExternalIntents.openUrl(context, ProjectLinks.WEBSITE) },
                        style = DcButtonStyle.Tonal,
                        icon = { Icon(painterResource(R.drawable.ic_brand_website), null) },
                        modifier = Modifier.slot()
                    )
                    DcButton(
                        text = stringResource(R.string.developer_link_x),
                        onClick = { ExternalIntents.openUrl(context, ProjectLinks.X) },
                        style = DcButtonStyle.Tonal,
                        icon = { Icon(painterResource(R.drawable.ic_brand_x), null) },
                        modifier = Modifier.slot()
                    )
                }
            }
        }
    }
}
