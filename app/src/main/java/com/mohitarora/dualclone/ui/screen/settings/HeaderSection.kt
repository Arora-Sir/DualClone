package com.mohitarora.dualclone.ui.screen.settings

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.clickable
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.BuildConfig
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcCard

const val DIAGNOSTICS_TAPS = 7

/** App identity card. Seven taps unlock diagnostics and the hint counts down from the fourth tap. */
@Composable
fun HeaderSection(
    tapCount: Int,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    val remaining = DIAGNOSTICS_TAPS - tapCount
    DcCard(
        modifier = modifier.fillMaxWidth().clip(DcShapes.card).clickable(role = Role.Button, onClick = onTap)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.app_icon_512),
                contentDescription = stringResource(R.string.settings_header_icon_description),
                modifier = Modifier.size(56.dp).clip(DcShapes.hero)
            )
            Spacer(Modifier.width(DcSpacing.lg))
            Column(verticalArrangement = Arrangement.spacedBy(DcSpacing.xs)) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.settings_header_version,
                        BuildConfig.VERSION_NAME,
                        BuildConfig.VERSION_CODE
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (!BuildConfig.IS_DEBUG) {
                    DcBadge(
                        text = stringResource(R.string.settings_badge_offline),
                        tone = DcTone.Success,
                        icon = Icons.Default.Lock
                    )
                }
            }
        }
        AnimatedVisibility(
            visible = tapCount in 4 until DIAGNOSTICS_TAPS,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = pluralStringResource(R.plurals.settings_tap_hint, remaining.coerceAtLeast(1), remaining.coerceAtLeast(1)),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = DcSpacing.sm)
            )
        }
    }
}
