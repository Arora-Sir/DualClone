package com.mohitarora.dualclone.ui.components

import androidx.compose.ui.draw.alpha
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.ui.viewmodel.FilterTab

/** Three equal segments with the live count after each label. The count slides when it changes. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SegmentedFilterBar(
    selectedTab: FilterTab,
    onTabSelected: (FilterTab) -> Unit,
    totalCount: Int,
    installedCount: Int,
    clonedCount: Int,
    modifier: Modifier = Modifier,
    countsReady: Boolean = true
) {
    // NOTE: Counts fade in once the first load is done, so a cold start never shows a frame of zeros.
    val countAlpha by animateFloatAsState(if (countsReady) 1f else 0f, label = "countAlpha")
    val tabs = listOf(
        Triple(FilterTab.ALL, R.string.tab_all, totalCount),
        Triple(FilterTab.INSTALLED, R.string.tab_installed, installedCount),
        Triple(FilterTab.CLONED, R.string.tab_cloned, clonedCount)
    )
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = DcSpacing.lg, vertical = DcSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            tabs.forEachIndexed { index, (tab, labelRes, count) ->
                SegmentedButton(
                    selected = selectedTab == tab,
                    onClick = { onTabSelected(tab) },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = tabs.size),
                    icon = {},
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        activeContentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                        inactiveContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    ),
                    modifier = Modifier.heightIn(min = 48.dp),
                    label = {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = stringResource(labelRes),
                                style = MaterialTheme.typography.labelLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            AnimatedContent(
                                targetState = count,
                                transitionSpec = {
                                    (slideInVertically { it / 2 } + fadeIn()) togetherWith
                                        (slideOutVertically { -it / 2 } + fadeOut()) using SizeTransform(clip = false) { _, _ -> snap() }
                                },
                                label = "tabCount"
                            ) { value ->
                                Text(
                                    text = value.toString(),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    modifier = Modifier.alpha(countAlpha)
                                )
                            }
                        }
                    }
                )
            }
        }
    }
}
