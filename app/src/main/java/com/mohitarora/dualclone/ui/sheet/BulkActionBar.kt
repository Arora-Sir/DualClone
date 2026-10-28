package com.mohitarora.dualclone.ui.sheet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcCard

/** Bottom bar shown in bulk mode. Both actions have the same height and width. */
@Composable
fun BulkActionBar(
    selectedCount: Int,
    onCloneAll: () -> Unit,
    onRemoveAll: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    DcCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DcSpacing.lg, vertical = DcSpacing.sm)
            .navigationBarsPadding(),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DcBadge(
                text = pluralStringResource(R.plurals.bulk_selected, selectedCount, selectedCount),
                tone = DcTone.Primary
            )
            IconButton(onClick = onCancel) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = stringResource(R.string.bulk_exit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DcButtonRow(modifier = Modifier.padding(top = DcSpacing.sm)) {
            DcButton(
                text = stringResource(R.string.bulk_clone),
                onClick = onCloneAll,
                modifier = Modifier.slot()
            )
            DcButton(
                text = stringResource(R.string.bulk_remove),
                onClick = onRemoveAll,
                style = DcButtonStyle.Destructive,
                modifier = Modifier.slot()
            )
        }
    }
}
