package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcContrast
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing

/**
 * One row of accent dots with a plus button at the end. The plus opens the full picker and
 * wears the current colour when the active accent is not one of the dots, so the selection is never hidden.
 */
@Composable
fun DcAccentRow(
    dots: List<DcPaletteCell>,
    selectedId: String,
    extra: DcPaletteCell?,
    onSelect: (DcPaletteCell) -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        dots.forEach { cell ->
            AccentDot(
                color = cell.color,
                label = cell.label,
                selected = cell.id == selectedId,
                onClick = { onSelect(cell) }
            )
        }
        // NOTE: An active accent that is not in the row appears as a sixth dot, so the current choice is always visible.
        if (extra != null) {
            AccentDot(color = extra.color, label = extra.label, selected = extra.id == selectedId, onClick = { onSelect(extra) })
        }
        AccentDot(
            color = MaterialTheme.colorScheme.surfaceContainerHighest,
            label = stringResource(R.string.accent_more),
            selected = false,
            plus = true,
            onClick = onMore
        )
    }
}

@Composable
private fun AccentDot(
    color: Color,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    plus: Boolean = false
) {
    val state = stringResource(if (selected) R.string.palette_selected else R.string.palette_not_selected)
    val ring = if (selected) {
        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
    } else {
        Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
    }
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .size(48.dp)
            .clip(DcShapes.chip)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = label
                stateDescription = state
            }
            ,
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color)
                .then(ring),
            contentAlignment = Alignment.Center
        ) {
            val glyph = DcContrast.onColor(color)
            when {
                plus -> Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface, modifier = Modifier.size(20.dp))
                selected -> Icon(Icons.Default.Check, contentDescription = null, tint = glyph, modifier = Modifier.size(20.dp))
            }
        }
    }
}

