package com.mohitarora.dualclone.ui.screen.home

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LayersClear
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Apps
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcEmptyState
import com.mohitarora.dualclone.ui.viewmodel.FilterTab
import androidx.compose.foundation.layout.Box

/** Empty states depend on the tab and on whether a search is active, so each one says something true. */
@Composable
fun HomeEmptyState(
    tab: FilterTab,
    searchQuery: String,
    actions: HomeActions,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        when {
            searchQuery.isNotEmpty() -> DcEmptyState(
                icon = Icons.Default.SearchOff,
                title = stringResource(R.string.empty_search_title),
                body = stringResource(R.string.empty_search_body, searchQuery),
                action = {
                    DcButton(
                        text = stringResource(R.string.empty_search_clear),
                        onClick = { actions.onSearchChange("") },
                        style = DcButtonStyle.Outlined
                    )
                }
            )
            tab == FilterTab.CLONED -> DcEmptyState(
                icon = Icons.Default.LayersClear,
                title = stringResource(R.string.empty_cloned_title),
                body = stringResource(R.string.empty_cloned_body),
                action = {
                    DcButton(
                        text = stringResource(R.string.empty_cloned_action),
                        onClick = { actions.onTabSelected(FilterTab.ALL) },
                        style = DcButtonStyle.Tonal
                    )
                }
            )
            tab == FilterTab.INSTALLED -> DcEmptyState(
                icon = Icons.Default.CheckCircle,
                title = stringResource(R.string.empty_installed_title),
                body = stringResource(R.string.empty_installed_body)
            )
            else -> DcEmptyState(
                icon = Icons.Default.Apps,
                title = stringResource(R.string.empty_all_title),
                body = stringResource(R.string.empty_all_body),
                action = {
                    DcButton(
                        text = stringResource(R.string.empty_all_action),
                        onClick = actions.onRefresh,
                        style = DcButtonStyle.Tonal
                    )
                }
            )
        }
    }
}
