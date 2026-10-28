package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcStatus
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.rememberReduceMotion

/** Small status label. One component replaces the many near identical chips. */
@Composable
fun DcBadge(
    text: String,
    modifier: Modifier = Modifier,
    tone: DcTone = DcTone.Neutral,
    icon: ImageVector? = null,
    showDot: Boolean = false
) {
    val colors = DcStatus.colors(tone)
    Surface(
        modifier = modifier,
        shape = DcShapes.chip,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = DcSpacing.sm, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showDot) {
                Box(Modifier.size(6.dp).background(colors.accent, CircleShape))
                Spacer(Modifier.width(6.dp))
            }
            if (icon != null) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = colors.content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** A settings row with a trailing switch. The whole row toggles and TalkBack reads it as one switch. */
@Composable
fun DcSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    DcSettingsRow(
        title = title,
        subtitle = subtitle,
        icon = icon,
        enabled = enabled,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        trailing = {
            Switch(checked = checked, onCheckedChange = null, enabled = enabled)
        }
    )
}

/** Equal width segments with a 48dp minimum height. Labels stay on one line. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> DcSegmentedControl(
    options: List<T>,
    selected: T,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    SingleChoiceSegmentedButtonRow(modifier = modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                ),
                modifier = Modifier.heightIn(min = 48.dp),
                label = {
                    Text(
                        text = label(option),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }
    }
}

/** Placeholder rows that match the app list layout while data loads. Static when animations are off. */
@Composable
fun DcSkeletonList(
    modifier: Modifier = Modifier,
    rows: Int = 8
) {
    val reduce = rememberReduceMotion()
    val alpha = if (reduce) {
        0.6f
    } else {
        val transition = rememberInfiniteTransition(label = "skeleton")
        val a by transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
            label = "skeletonAlpha"
        )
        a
    }
    val loading = stringResource(R.string.dc_loading)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DcSpacing.md)
            .clearAndSetSemantics { contentDescription = loading },
        verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
    ) {
        repeat(rows) {
            Surface(shape = DcShapes.card, color = MaterialTheme.colorScheme.surfaceContainer) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(DcSpacing.md),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val bar = MaterialTheme.colorScheme.onSurface.copy(alpha = alpha * 0.18f)
                    Box(Modifier.size(44.dp).clip(DcShapes.field).background(bar))
                    Spacer(Modifier.width(DcSpacing.md))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
                        Box(Modifier.fillMaxWidth(0.55f).height(14.dp).clip(DcShapes.chip).background(bar))
                        Box(Modifier.fillMaxWidth(0.8f).height(12.dp).clip(DcShapes.chip).background(bar))
                    }
                    Spacer(Modifier.width(DcSpacing.md))
                    Box(Modifier.width(72.dp).height(36.dp).clip(DcShapes.button).background(bar))
                }
            }
        }
    }
}
