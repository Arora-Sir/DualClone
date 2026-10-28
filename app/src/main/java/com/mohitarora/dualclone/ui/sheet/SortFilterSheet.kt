package com.mohitarora.dualclone.ui.sheet

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.core.designsystem.components.DcSwitchRow
import com.mohitarora.dualclone.ui.viewmodel.SortOrder

private class SortOption(val order: SortOrder, @StringRes val label: Int)

private val SORT_OPTIONS = listOf(
    SortOption(SortOrder.NAME_ASC, R.string.sort_name_asc),
    SortOption(SortOrder.NAME_DESC, R.string.sort_name_desc),
    SortOption(SortOrder.RECENTLY_UPDATED, R.string.sort_recently_updated),
    SortOption(SortOrder.INSTALL_TIME, R.string.sort_install_time)
)

@Composable
fun SortFilterSheet(
    currentSort: SortOrder,
    onSortSelected: (SortOrder) -> Unit,
    includeSystem: Boolean,
    onToggleIncludeSystem: (Boolean) -> Unit,
    unclonedOnly: Boolean,
    onToggleUnclonedOnly: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.sort_title)) {
        DcSettingsGroup {
            SORT_OPTIONS.forEach { option ->
                SortOptionRow(
                    label = stringResource(option.label),
                    selected = currentSort == option.order,
                    onSelect = { onSortSelected(option.order) }
                )
            }
        }
        Text(
            text = stringResource(R.string.filter_heading),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(top = DcSpacing.sm)
                .semantics { heading() }
        )
        DcSettingsGroup {
            DcSwitchRow(
                title = stringResource(R.string.filter_include_system),
                checked = includeSystem,
                onCheckedChange = onToggleIncludeSystem
            )
            DcSwitchRow(
                title = stringResource(R.string.filter_uncloned_only),
                checked = unclonedOnly,
                onCheckedChange = onToggleUnclonedOnly
            )
        }
    }
}

@Composable
private fun SortOptionRow(label: String, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = DcSpacing.lg),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null)
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = DcSpacing.md)
        )
    }
}
