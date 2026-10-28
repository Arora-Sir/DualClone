package com.mohitarora.dualclone.ui.screen

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import com.mohitarora.dualclone.core.designsystem.components.DcSnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.mohitarora.dualclone.ui.screen.home.CollapsingHeaderLayout
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.common.UiEvent
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcSkeletonList
import com.mohitarora.dualclone.core.designsystem.rememberDcHaptics
import com.mohitarora.dualclone.data.model.AppItem
import com.mohitarora.dualclone.data.model.ShizukuState
import com.mohitarora.dualclone.domain.model.Profile
import com.mohitarora.dualclone.domain.model.ProfileKind
import com.mohitarora.dualclone.ui.components.EmptyProfileCard
import com.mohitarora.dualclone.ui.components.KnoxGuidanceCard
import com.mohitarora.dualclone.ui.components.ProfileSelectorRow
import com.mohitarora.dualclone.ui.components.SegmentedFilterBar
import com.mohitarora.dualclone.ui.components.ShizukuGuidanceCard
import com.mohitarora.dualclone.ui.dialog.BulkRemoveDialog
import com.mohitarora.dualclone.ui.dialog.BulkSummaryDialog
import com.mohitarora.dualclone.ui.dialog.RemoveCloneDialog
import com.mohitarora.dualclone.ui.external.ExternalIntents
import com.mohitarora.dualclone.ui.screen.home.AppList
import com.mohitarora.dualclone.ui.screen.home.HomeActions
import com.mohitarora.dualclone.ui.screen.home.HomeBanners
import com.mohitarora.dualclone.ui.screen.home.HomeEmptyState
import com.mohitarora.dualclone.ui.screen.home.ReconcileLine
import com.mohitarora.dualclone.ui.screen.home.RowConfig
import com.mohitarora.dualclone.ui.screen.home.groupApps
import com.mohitarora.dualclone.ui.sheet.AppOpsSheet
import com.mohitarora.dualclone.ui.sheet.BulkActionBar
import com.mohitarora.dualclone.ui.sheet.ShizukuSetupSheet
import com.mohitarora.dualclone.ui.sheet.SortFilterSheet
import com.mohitarora.dualclone.ui.viewmodel.HomeUiState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

private val SECURE_FOLDER_PLACEHOLDER = Profile(userId = 150, name = "", kind = ProfileKind.SECURE_FOLDER, isRestricted = true)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    actions: HomeActions,
    events: Flow<UiEvent>,
    showOnboarding: Boolean,
    onOnboardingSeen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptics = rememberDcHaptics()
    val snackbarHostState = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    var showSetupSheet by rememberSaveable { mutableStateOf(showOnboarding) }
    var showSortSheet by rememberSaveable { mutableStateOf(false) }
    var showKnoxCard by rememberSaveable { mutableStateOf(false) }
    var pendingRemove by rememberSaveable { mutableStateOf<String?>(null) }
    var confirmBulkRemove by rememberSaveable { mutableStateOf(false) }

    val isSamsung = remember { Build.MANUFACTURER.equals("samsung", ignoreCase = true) }
    val secureFolderAvailable = remember { isSamsung && ExternalIntents.hasSecureFolder(context) }
    val targets = remember(state.profiles, secureFolderAvailable) {
        val real = state.profiles.filter { it.kind != ProfileKind.PRIMARY }
        if (secureFolderAvailable && real.none { it.kind == ProfileKind.SECURE_FOLDER }) real + SECURE_FOLDER_PLACEHOLDER else real
    }
    val destinations = targets.filter { it.kind != ProfileKind.SECURE_FOLDER }
    val selected = state.selectedProfile
    val actionsEnabled = state.shizukuState is ShizukuState.Ready && selected != null &&
        !state.isSelectedProfilePaused && !selected.isRestricted
    val profileLabel = selected?.let { stringResource(profileNameRes(it.kind)) }

    // NOTE: Shizuku asks for permission on its own the first time the state needs it.
    LaunchedEffect(state.shizukuState) {
        if (state.shizukuState is ShizukuState.PermissionRequired && !showSetupSheet) actions.onRequestShizukuPermission()
    }
    val latestState by rememberUpdatedState(state)
    // NOTE: Refresh in the background when the list is older than five minutes, so the update label never goes stale.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        actions.onResume()
        val at = latestState.lastReconcile.atMillis
        if (latestState.shizukuState is ShizukuState.Ready && System.currentTimeMillis() - at > 5 * 60_000L) actions.onRefresh()
    }
    BackHandler(enabled = state.isBulkMode) { actions.onExitBulk() }

    LaunchedEffect(events) {
        events.collect { event ->
            val result = snackbarHostState.showSnackbar(
                message = event.message.resolve(context),
                actionLabel = event.actionLabel?.resolve(context),
                duration = if (event.actionLabel != null) SnackbarDuration.Long else SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) event.actionId?.let(actions.onEventAction)
        }
    }

    val disabledMessage = stringResource(
        when {
            state.shizukuState !is ShizukuState.Ready -> R.string.disabled_shizuku
            state.isSelectedProfilePaused -> R.string.disabled_paused
            selected?.isRestricted == true -> R.string.disabled_restricted
            else -> R.string.disabled_no_profile
        }
    )
    val setupLabel = stringResource(R.string.profiles_shizuku_setup)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.width(DcSpacing.sm))
                        ShizukuStatusBadge(state = state.shizukuState, onClick = { showSetupSheet = true })
                    }
                },
                actions = {
                    IconButton(onClick = actions.onSettings) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.settings_title))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        snackbarHost = { DcSnackbarHost(snackbarHostState) },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isBulkMode,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                BulkActionBar(
                    selectedCount = state.selectedPackages.size,
                    onCloneAll = actions.onBulkClone,
                    onRemoveAll = { confirmBulkRemove = true },
                    onCancel = actions.onExitBulk
                )
            }
        }
    ) { innerPadding ->
        val compactHeight = LocalConfiguration.current.screenHeightDp < 480
        val header: @Composable () -> Unit = {
            ShizukuGuidanceCard(
                state = state.shizukuState,
                onOpenSetup = { showSetupSheet = true },
                onRequestPermission = actions.onRequestShizukuPermission
            )
            if (destinations.isEmpty() && state.shizukuState is ShizukuState.Ready && !state.isLoading) {
                EmptyProfileCard(isSamsung = isSamsung)
            }
            HomeBanners(state = state, actions = actions, profileLabel = profileLabel)
            ProfileSelectorRow(
                profiles = targets,
                selectedUserId = selected?.userId,
                onProfileSelected = {
                    showKnoxCard = false
                    actions.onProfileSelected(it.userId)
                },
                onSecureFolderTapped = { showKnoxCard = !showKnoxCard },
                secureFolderActive = showKnoxCard
            )
            AnimatedVisibility(visible = showKnoxCard) { KnoxGuidanceCard() }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) { SearchField(query = state.searchQuery, onChange = actions.onSearchChange) }
                IconButton(onClick = { showSortSheet = true }, modifier = Modifier.padding(end = DcSpacing.sm)) {
                    Icon(Icons.Default.FilterList, contentDescription = stringResource(R.string.filter_and_sort), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            SegmentedFilterBar(
                selectedTab = state.filterTab,
                onTabSelected = actions.onTabSelected,
                totalCount = state.totalCount,
                installedCount = state.installedCount,
                clonedCount = state.clonedCount,
                countsReady = !state.isLoading || state.totalCount > 0
            )
            ReconcileLine(
                atMillis = state.lastReconcile.atMillis,
                ok = state.lastReconcile.ok,
                problems = state.profiles.filter { it.kind != ProfileKind.PRIMARY && (it.isPaused || !it.isRunning) }.map {
                    stringResource(if (it.isPaused) R.string.profile_problem_paused else R.string.profile_problem_stopped, stringResource(profileNameRes(it.kind)))
                }
            )

        }
        val body: @Composable () -> Unit = {
            val groups = remember(state.apps, state.sortOrder) { groupApps(state.apps, state.sortOrder) }
            val config = RowConfig(
                state = state,
                actions = actions,
                actionsEnabled = actionsEnabled,
                cloneBadge = selected?.let { stringResource(profileBadgeRes(it.kind)) },
                onRequestRemove = { pendingRemove = it.packageName },
                onDisabledAction = {
                    haptics.error()
                    scope.launch {
                        val res = snackbarHostState.showSnackbar(
                            message = disabledMessage,
                            actionLabel = if (state.shizukuState !is ShizukuState.Ready) setupLabel else null
                        )
                        if (res == SnackbarResult.ActionPerformed) showSetupSheet = true
                    }
                }
            )
            PullToRefreshBox(
                isRefreshing = state.isRefreshing,
                onRefresh = actions.onRefresh,
                modifier = Modifier.fillMaxSize()
            ) {
                when {
                    state.apps.isEmpty() && state.isLoading -> DcSkeletonList(Modifier.padding(top = DcSpacing.sm))
                    state.apps.isEmpty() -> HomeEmptyState(state.filterTab, state.searchQuery, actions)
                    else -> AppList(groups = groups, config = config)
                }
            }
        }
        if (compactHeight) {
            CollapsingHeaderLayout(header = header, modifier = Modifier.padding(innerPadding), body = body)
        } else {
            Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                header()
                body()
            }
        }
    }

    pendingRemove?.let { pkg ->
        val app = state.apps.firstOrNull { it.packageName == pkg }
        if (app == null) {
            pendingRemove = null
        } else {
            RemoveCloneDialog(
                app = app,
                profileLabel = profileLabel.orEmpty(),
                onConfirm = {
                    pendingRemove = null
                    actions.onToggleClone(app)
                },
                onDismiss = { pendingRemove = null }
            )
        }
    }
    if (confirmBulkRemove) {
        BulkRemoveDialog(
            count = state.selectedPackages.size,
            profileLabel = profileLabel.orEmpty(),
            onConfirm = {
                confirmBulkRemove = false
                actions.onBulkRemove()
            },
            onDismiss = { confirmBulkRemove = false }
        )
    }
    state.bulkSummary?.let { summary ->
        BulkSummaryDialog(
            summary = summary,
            appNames = remember(state.apps) { state.apps.associate { it.packageName to it.appName } },
            onRetry = actions.onRetryFailed,
            onDismiss = actions.onDismissBulkSummary
        )
    }
    state.activeAppOpsPackage?.let { pkg ->
        AppOpsSheet(
            packageName = pkg,
            profileLabel = profileLabel.orEmpty(),
            opsState = state.activeAppOpsState,
            onToggleOp = actions.onToggleAppOp,
            onDismiss = actions.onCloseAppOps
        )
    }
    if (showSetupSheet) {
        ShizukuSetupSheet(
            state = state.shizukuState,
            onRequestPermission = actions.onRequestShizukuPermission,
            onRefresh = actions.onRefreshShizuku,
            onDismiss = {
                showSetupSheet = false
                onOnboardingSeen()
            }
        )
    }
    if (showSortSheet) {
        SortFilterSheet(
            currentSort = state.sortOrder,
            onSortSelected = {
                actions.onSortSelected(it)
                showSortSheet = false
            },
            includeSystem = state.includeSystem,
            onToggleIncludeSystem = actions.onIncludeSystemChange,
            unclonedOnly = state.unclonedOnly,
            onToggleUnclonedOnly = actions.onUnclonedOnlyChange,
            onDismiss = { showSortSheet = false }
        )
    }
}

@Composable
private fun SearchField(query: String, onChange: (String) -> Unit) {
    val keyboard = LocalSoftwareKeyboardController.current
    OutlinedTextField(
        value = query,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        trailingIcon = {
            if (query.isNotEmpty()) {
                IconButton(onClick = { onChange("") }) {
                    Icon(Icons.Default.Clear, contentDescription = stringResource(R.string.search_clear))
                }
            }
        },
        singleLine = true,
        shape = DcShapes.field,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = DcSpacing.lg, vertical = DcSpacing.xs)
            .heightIn(min = 56.dp)
    )
}

/** Cold start shows Checking, never a red Stopped, until Shizuku actually answers. */
@Composable
private fun ShizukuStatusBadge(state: ShizukuState, onClick: () -> Unit) {
    val (label, tone) = when (state) {
        is ShizukuState.Ready -> R.string.shizuku_badge_ready to DcTone.Success
        is ShizukuState.Checking -> R.string.shizuku_badge_checking to DcTone.Info
        is ShizukuState.PermissionRequired -> R.string.shizuku_badge_grant to DcTone.Warning
        is ShizukuState.User95Missing -> R.string.shizuku_badge_setup to DcTone.Warning
        is ShizukuState.Unavailable -> R.string.shizuku_badge_stopped to DcTone.Error
    }
    Box(
        modifier = Modifier
            .heightIn(min = 48.dp)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        DcBadge(text = stringResource(label), tone = tone, showDot = true)
    }
}

private fun profileNameRes(kind: ProfileKind): Int = when (kind) {
    ProfileKind.PRIMARY -> R.string.profile_kind_primary
    ProfileKind.DUAL_MESSENGER -> R.string.profile_kind_dual
    ProfileKind.WORK -> R.string.profile_kind_work
    ProfileKind.SECURE_FOLDER -> R.string.profile_kind_secure
    ProfileKind.OTHER_USER -> R.string.profile_kind_other
}

private fun profileBadgeRes(kind: ProfileKind): Int = when (kind) {
    ProfileKind.DUAL_MESSENGER -> R.string.badge_dual
    ProfileKind.WORK -> R.string.badge_work
    else -> R.string.badge_other
}
