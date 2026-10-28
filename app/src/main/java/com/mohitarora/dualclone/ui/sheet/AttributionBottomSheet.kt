package com.mohitarora.dualclone.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.ui.external.ExternalIntents

private data class AttributionEntry(
    @StringRes val title: Int,
    @StringRes val description: Int,
    val url: String
)

private val ATTRIBUTIONS = listOf(
    AttributionEntry(R.string.attr_darq_title, R.string.attr_darq_body, "https://github.com/Arora-Sir/DarQ-Reborn"),
    AttributionEntry(R.string.attr_thedjchi_title, R.string.attr_thedjchi_body, "https://github.com/thedjchi/Shizuku"),
    AttributionEntry(R.string.attr_shizuku_title, R.string.attr_shizuku_body, "https://github.com/RikkaApps/Shizuku"),
    AttributionEntry(R.string.attr_island_title, R.string.attr_island_body, "https://github.com/oasisfeng/island"),
    AttributionEntry(R.string.attr_dual_manager_title, R.string.attr_dual_manager_body, "https://github.com/trunone/dual-manager"),
    AttributionEntry(R.string.attr_artemis_title, R.string.attr_artemis_body, "https://github.com/Arora-Sir/Super-Artemis"),
    AttributionEntry(R.string.attr_compose_title, R.string.attr_compose_body, "https://developer.android.com/jetpack/compose"),
    AttributionEntry(R.string.attr_mcu_title, R.string.attr_mcu_body, "https://github.com/jordond/materialkolor")
)

@Composable
fun AttributionBottomSheet(onDismiss: () -> Unit) {
    val context = LocalContext.current
    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.attribution_title)) {
        Text(
            text = stringResource(R.string.attribution_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Column(verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
            ATTRIBUTIONS.forEach { entry ->
                val title = stringResource(entry.title)
                DcCard(
                    onClick = { ExternalIntents.openUrl(context, entry.url) },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.semantics { role = Role.Button }
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(entry.description),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(Modifier.width(DcSpacing.md))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = stringResource(R.string.attribution_open_link, title),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
