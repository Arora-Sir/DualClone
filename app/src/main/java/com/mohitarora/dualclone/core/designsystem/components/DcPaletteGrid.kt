package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcContrast
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing

data class DcPaletteCell(
    val id: String,
    val label: String,
    val color: Color,
    val isCustom: Boolean = false
)

const val DC_PALETTE_MAX_COLUMNS = 5

/**
 * Columns for [count] cells with rows as even as possible. Up to five cells share one row, so a single leftover cell
 * never sits alone. Above that the cells split evenly, for example six become 3 and 3, and nine become 5 and 4.
 * Sets of 21 or more can leave one cell alone in the last row, which is out of scope for a palette of about ten.
 */
fun paletteColumns(count: Int, maxColumns: Int = DC_PALETTE_MAX_COLUMNS): Int {
    if (count <= 0) return 1
    val rows = (count + maxColumns - 1) / maxColumns
    return (count + rows - 1) / rows
}

/**
 * Grid of equal cells. Every cell has the same width and, per row, the same height, whatever the label length.
 * Rows are balanced and the last row is padded with empty cells so widths never change.
 */
@Composable
fun DcPaletteGrid(
    cells: List<DcPaletteCell>,
    selectedId: String?,
    onSelect: (DcPaletteCell) -> Unit,
    modifier: Modifier = Modifier
) {
    val columns = paletteColumns(cells.size)
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(DcSpacing.xs)) {
        cells.chunked(columns).forEach { rowCells ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(DcSpacing.xs)
            ) {
                rowCells.forEach { cell ->
                    PaletteCell(
                        cell = cell,
                        selected = cell.id == selectedId,
                        onClick = { onSelect(cell) },
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                repeat(columns - rowCells.size) {
                    Box(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun PaletteCell(
    cell: DcPaletteCell,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val selectedText = stringResource(if (selected) R.string.palette_selected else R.string.palette_not_selected)
    val ring = if (selected) {
        BorderStroke(3.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    }
    Column(
        modifier = modifier
            .heightIn(min = DcSizes.paletteCell)
            .clip(DcShapes.field)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = cell.label
                stateDescription = selectedText
            }
            .padding(vertical = DcSpacing.sm, horizontal = DcSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(DcSpacing.xs, Alignment.Top)
    ) {
        Box(
            modifier = Modifier
                .size(DcSizes.paletteCircle)
                .clip(CircleShape)
                .background(cell.color)
                .border(ring, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            val glyphTint = DcContrast.onColor(cell.color)
            when {
                selected -> Icon(Icons.Default.Check, contentDescription = null, tint = glyphTint, modifier = Modifier.size(22.dp))
                cell.isCustom -> Icon(Icons.Default.Add, contentDescription = null, tint = glyphTint, modifier = Modifier.size(20.dp))
            }
        }
        Text(
            text = cell.label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
