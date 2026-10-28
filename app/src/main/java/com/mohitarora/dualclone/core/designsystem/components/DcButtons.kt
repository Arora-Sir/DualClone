package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.dcPressScale

enum class DcButtonStyle { Primary, Tonal, Outlined, Destructive, DestructiveOutlined, Text }

/**
 * Single button for the whole app. 48dp minimum height, 24dp shape, one line label,
 * and a fixed 18dp icon slot so mixed rows never differ in height or icon size.
 */
@Composable
fun DcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: DcButtonStyle = DcButtonStyle.Primary,
    enabled: Boolean = true,
    icon: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val colors = MaterialTheme.colorScheme
    val buttonModifier = modifier
        .heightIn(min = DcSizes.minButtonHeight)
        .dcPressScale(interaction)
    val padding = PaddingValues(horizontal = DcSpacing.lg, vertical = DcSpacing.sm)
    val content: @Composable RowScope.() -> Unit = {
        if (icon != null) {
            Box(Modifier.size(DcSizes.buttonIconSlot), contentAlignment = Alignment.Center) { icon() }
            Spacer(Modifier.width(DcSpacing.sm))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
    when (style) {
        DcButtonStyle.Primary -> Button(
            onClick = onClick, modifier = buttonModifier, enabled = enabled, shape = DcShapes.button,
            interactionSource = interaction, contentPadding = padding, content = content
        )
        DcButtonStyle.Tonal -> FilledTonalButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled, shape = DcShapes.button,
            interactionSource = interaction, contentPadding = padding, content = content
        )
        DcButtonStyle.Outlined -> OutlinedButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled, shape = DcShapes.button,
            interactionSource = interaction, contentPadding = padding, content = content
        )
        DcButtonStyle.Destructive -> Button(
            onClick = onClick, modifier = buttonModifier, enabled = enabled, shape = DcShapes.button,
            interactionSource = interaction, contentPadding = padding,
            colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
            content = content
        )
        DcButtonStyle.DestructiveOutlined -> OutlinedButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled, shape = DcShapes.button,
            interactionSource = interaction, contentPadding = padding,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, if (enabled) colors.error else colors.onSurface.copy(alpha = 0.12f)),
            content = content
        )
        DcButtonStyle.Text -> TextButton(
            onClick = onClick, modifier = buttonModifier, enabled = enabled, shape = DcShapes.button,
            interactionSource = interaction, contentPadding = padding, content = content
        )
    }
}

interface DcButtonRowScope {
    /** Gives a child equal width and, in a side by side row, equal height. */
    fun Modifier.slot(): Modifier
}

private class SideBySideScope(private val row: RowScope) : DcButtonRowScope {
    override fun Modifier.slot(): Modifier = with(row) { this@slot.weight(1f).fillMaxHeight() }
}

private object StackedScope : DcButtonRowScope {
    override fun Modifier.slot(): Modifier = this.fillMaxWidth()
}

/**
 * Equal width and equal height buttons. Sits side by side by default and stacks vertically
 * when the container is narrower than 360dp or the font scale is above 1.3.
 */
@Composable
fun DcButtonRow(
    modifier: Modifier = Modifier,
    content: @Composable DcButtonRowScope.() -> Unit
) {
    val fontScale = LocalDensity.current.fontScale
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val stacked = maxWidth < DcSizes.buttonRowStackWidth || fontScale >= 1.3f
        if (stacked) {
            Column(verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)) {
                StackedScope.content()
            }
        } else {
            Row(
                modifier = Modifier.height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SideBySideScope(this).content()
            }
        }
    }
}
