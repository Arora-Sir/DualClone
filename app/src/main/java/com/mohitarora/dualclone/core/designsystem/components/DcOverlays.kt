package com.mohitarora.dualclone.core.designsystem.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing

/**
 * The one bottom sheet. 24dp top corners, one container colour, scrollable body that respects
 * the navigation bar and the keyboard, and a width cap for tablets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DcSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    skipPartiallyExpanded: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        sheetMaxWidth = DcSizes.sheetMaxWidth,
        shape = DcShapes.sheet,
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = { WindowInsets.safeDrawing.only(WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DcSpacing.xl)
                .padding(bottom = DcSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(DcSpacing.md)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
            }
            content()
        }
    }
}

/** Text action for a dialog. Always at least 48dp tall. */
@Composable
fun DcDialogAction(
    text: String,
    onClick: () -> Unit,
    destructive: Boolean = false
) {
    DcButton(
        text = text,
        onClick = onClick,
        style = if (destructive) DcButtonStyle.Destructive else DcButtonStyle.Text
    )
}

/** The one dialog shell. 28dp shape and surfaceContainerHigh for every dialog in the app. */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DcDialog(
    onDismiss: () -> Unit,
    title: String,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    actions: @Composable RowScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    BasicAlertDialog(onDismissRequest = onDismiss, properties = properties, modifier = modifier) {
        Surface(
            shape = DcShapes.dialog,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 8.dp,
            modifier = Modifier.widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(DcSpacing.xl)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(DcSpacing.md)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics { heading() }
                )
                content()
                Spacer(Modifier.height(DcSpacing.xs))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions
                )
            }
        }
    }
}

/** Confirm or cancel dialog. [destructive] colours the confirm action with the error role. */
@Composable
fun DcConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    dismissLabel: String = stringResource(R.string.action_cancel),
    destructive: Boolean = false
) {
    DcDialog(
        onDismiss = onDismiss,
        title = title,
        modifier = modifier,
        actions = {
            DcDialogAction(text = dismissLabel, onClick = onDismiss)
            DcDialogAction(text = confirmLabel, onClick = onConfirm, destructive = destructive)
        }
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * The one snackbar host of the app. The Material default uses the inverse surface, which is light in dark mode and looked like
 * a white sticker over a dark screen. This one follows the theme in every mode, including Pure black.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DcSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = DcShapes.card,
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            actionColor = MaterialTheme.colorScheme.primary,
            dismissActionContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** Scaffold for pushed screens. Large title collapses on scroll and the back arrow always has a label. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DcSubScreen(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState? = null,
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = title,
                        modifier = Modifier.semantics { heading() }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                },
                actions = actions,
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { if (snackbarHostState != null) DcSnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        content = content
    )
}
