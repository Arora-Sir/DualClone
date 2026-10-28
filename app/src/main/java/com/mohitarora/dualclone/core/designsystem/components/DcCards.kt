package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcStatus
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.dcPressScale

/** Tonal container used by every grouped surface in the app. */
@Composable
fun DcCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    selected: Boolean = false,
    shape: Shape = DcShapes.card,
    containerColor: Color? = null,
    contentPadding: PaddingValues = PaddingValues(DcSpacing.lg),
    content: @Composable ColumnScope.() -> Unit
) {
    val color = containerColor ?: if (selected) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        // NOTE: Cards sit on the container step, one below fields and raised parts, which is how Material 3 and Super-Artemis layer surfaces.
        MaterialTheme.colorScheme.surfaceContainer
    }
    if (onClick != null) {
        val interaction = remember { MutableInteractionSource() }
        Surface(
            onClick = onClick,
            modifier = modifier.dcPressScale(interaction),
            shape = shape,
            color = color,
            interactionSource = interaction
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(modifier = modifier, shape = shape, color = color) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}

@Composable
fun DcSectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .semantics { heading() }
            .padding(start = DcSpacing.xs, top = DcSpacing.sm, bottom = DcSpacing.sm)
    )
}

/** A section header above a single card that holds [DcSettingsRow] children. */
@Composable
fun DcSettingsGroup(
    modifier: Modifier = Modifier,
    header: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth()) {
        if (header != null) DcSectionHeader(header)
        DcCard(shape = DcShapes.group, contentPadding = PaddingValues(0.dp), content = content)
    }
}

/** The 2dp gap between rows of a group. It shows the screen background, so each row reads as its own tile. */
@Composable
fun DcSettingsDivider(modifier: Modifier = Modifier, tonal: Boolean = false) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(2.dp)
            .background(MaterialTheme.colorScheme.background)
    )
}

@Composable
fun DcIconCircle(
    icon: ImageVector,
    modifier: Modifier = Modifier,
    container: Color = MaterialTheme.colorScheme.secondaryContainer,
    tint: Color = MaterialTheme.colorScheme.onSecondaryContainer
) {
    Box(
        modifier = modifier
            .size(DcSizes.settingsIconCircle)
            .background(container, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

/**
 * One row of a settings group. A 40dp tonal circle holds the leading glyph, text takes the remaining width,
 * and the whole row is at least 56dp so the 48dp touch target rule always holds.
 */
@Composable
fun DcSettingsRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector? = null,
    leading: (@Composable () -> Unit)? = null,
    iconStyle: DcIconStyle = DcIconStyle.Bare,
    iconTint: Color = Color.Unspecified,
    iconContainer: Color = Color.Unspecified,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    titleColor: Color = MaterialTheme.colorScheme.onSurface
) {
    val clickable = if (onClick != null) {
        Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }
    val alpha = if (enabled) 1f else 0.38f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(clickable)
            .heightIn(min = 56.dp)
            .padding(horizontal = DcSpacing.lg, vertical = DcSpacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when {
            leading != null -> leading()
            icon != null && iconStyle == DcIconStyle.Tonal -> DcIconCircle(
                icon,
                container = iconContainer.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.secondaryContainer,
                tint = iconTint.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSecondaryContainer
            )
            icon != null -> Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint.takeIf { it != Color.Unspecified } ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(DcSizes.settingsIconGlyph)
            )
        }
        if (leading != null || icon != null) Spacer(Modifier.width(DcSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor.copy(alpha = titleColor.alpha * alpha)
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = alpha)
                )
            }
        }
        if (trailing != null) {
            Spacer(Modifier.width(DcSpacing.md))
            trailing()
        }
    }
}

/** How a settings row draws its leading icon. Bare is the default, Tonal is for the header card and the Support row only. */
enum class DcIconStyle { Bare, Tonal }

/** Tinted banner for guidance and warnings. Accent and text colours come from [DcStatus] so contrast holds. */
@Composable
fun DcInfoCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    tone: DcTone = DcTone.Info,
    icon: ImageVector? = null,
    action: (@Composable ColumnScope.() -> Unit)? = null
) {
    val colors = DcStatus.colors(tone)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(),
        shape = DcShapes.card,
        color = colors.container,
        border = BorderStroke(1.dp, colors.border)
    ) {
        Row(modifier = Modifier.padding(DcSpacing.lg)) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.accent,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(DcSpacing.md))
            }
            Column(verticalArrangement = Arrangement.spacedBy(DcSpacing.xs), modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (action != null) {
                    Spacer(Modifier.padding(top = DcSpacing.xs))
                    action()
                }
            }
        }
    }
}

@Composable
fun DcEmptyState(
    icon: ImageVector,
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DcSpacing.xxl, vertical = DcSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DcSpacing.sm, Alignment.CenterVertically)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(56.dp)
        )
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Clip
        )
        if (action != null) {
            Spacer(Modifier.padding(top = DcSpacing.sm))
            action()
        }
    }
}
