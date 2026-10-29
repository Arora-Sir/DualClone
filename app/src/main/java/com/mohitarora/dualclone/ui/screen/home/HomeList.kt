package com.mohitarora.dualclone.ui.screen.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.LocalWindowWidthClass
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.ui.components.AlphabetScrollbar
import com.mohitarora.dualclone.ui.components.AppCard
import com.mohitarora.dualclone.ui.viewmodel.HomeUiState
import com.mohitarora.dualclone.ui.viewmodel.SortOrder
import kotlinx.coroutines.launch

/** Everything a row needs besides the app itself. */
class RowConfig(
    val state: HomeUiState,
    val actions: HomeActions,
    val actionsEnabled: Boolean,
    val cloneBadge: String?,
    val onRequestRemove: (AppItem) -> Unit,
    val onDisabledAction: () -> Unit
)

@Composable
private fun AppRow(app: AppItem, config: RowConfig, modifier: Modifier = Modifier) {
    val s = config.state
    val a = config.actions
    AppCard(
        app = app,
        isBulkMode = s.isBulkMode,
        isSelected = s.selectedPackages.contains(app.packageName),
        actionsEnabled = config.actionsEnabled,
        cloneBadge = config.cloneBadge,
        sortOrder = s.sortOrder,
        onToggleSelect = a.onToggleSelection,
        onLongClick = a.onStartBulk,
        onClone = a.onToggleClone,
        onRemove = config.onRequestRemove,
        onDisabledAction = config.onDisabledAction,
        onAppOpsClick = a.onOpenAppOps,
        modifier = modifier
    )
}

@Composable
private fun ListFooter(totalApps: Int, reconcileTime: Long, ok: Boolean) {
    if (totalApps <= 0) return
    val footerText = if (reconcileTime > 0L) {
        val relative = reconcileText(reconcileTime, ok)
        "$totalApps apps · $relative"
    } else {
        "$totalApps apps"
    }
    Text(
        text = footerText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = DcSpacing.md),
        textAlign = androidx.compose.ui.text.style.TextAlign.Center
    )
}

/**
 * Phones and tablets under 840dp get a single column. At 840dp and wider the items flow
 * into an adaptive grid so a 14 inch tablet is not one stretched column.
 */
@Composable
fun AppList(
    groups: List<AppGroup>,
    config: RowConfig,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    gridState: LazyGridState = rememberLazyGridState(),
    contentPadding: PaddingValues = PaddingValues(bottom = 96.dp)
) {
    val sort = config.state.sortOrder
    val scope = rememberCoroutineScope()
    val expanded = LocalWindowWidthClass.current == WindowWidthSizeClass.Expanded
    val showRail = sort.isAlphabetical() && groups.isNotEmpty()

    Box(modifier = modifier.fillMaxSize()) {
        if (expanded) {
            WideGrid(groups, config, gridState, contentPadding)
            RailHost(showRail, sort) { letter ->
                scope.launch {
                    val target = indexForLetter(groups, sort, letter)
                    gridState.scrollToItem(if (target >= 0) target else (gridState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                }
            }
        } else {
            NarrowList(groups, config, listState, contentPadding)
            RailHost(showRail, sort) { letter ->
                scope.launch {
                    val target = indexForLetter(groups, sort, letter)
                    listState.scrollToItem(if (target >= 0) target else (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0))
                }
            }
        }
    }
}

@Composable
private fun NarrowList(
    groups: List<AppGroup>,
    config: RowConfig,
    state: LazyListState,
    contentPadding: PaddingValues
) {
    LazyColumn(
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        groups.forEach { group ->
            items(items = group.apps, key = { it.packageName }) { app ->
                AppRow(app, config, Modifier.animateItem())
            }
        }
        item(key = "narrow_list_footer") {
            ListFooter(
                totalApps = config.state.apps.size,
                reconcileTime = config.state.lastReconcile.atMillis,
                ok = config.state.lastReconcile.ok
            )
        }
    }
}

@Composable
private fun WideGrid(
    groups: List<AppGroup>,
    config: RowConfig,
    state: LazyGridState,
    contentPadding: PaddingValues
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 380.dp),
        state = state,
        modifier = Modifier.fillMaxSize(),
        contentPadding = contentPadding
    ) {
        groups.forEach { group ->
            items(items = group.apps, key = { it.packageName }) { app ->
                AppRow(app, config, Modifier.animateItem())
            }
        }
        item(key = "wide_grid_footer", span = { GridItemSpan(maxLineSpan) }) {
            ListFooter(
                totalApps = config.state.apps.size,
                reconcileTime = config.state.lastReconcile.atMillis,
                ok = config.state.lastReconcile.ok
            )
        }
    }
}

@Composable
private fun BoxScope.RailHost(
    visible: Boolean,
    sort: SortOrder,
    onLetter: (Char) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = Modifier.align(Alignment.CenterEnd)
    ) {
        AlphabetScrollbar(
            sortOrder = sort,
            onLetterSelected = onLetter,
            modifier = Modifier.fillMaxHeight()
        )
    }
}
