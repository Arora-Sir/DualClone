package com.mohitarora.dualclone.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.mohitarora.dualclone.core.common.UiEvent
import kotlinx.coroutines.flow.Flow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcSubScreen
import com.mohitarora.dualclone.core.designsystem.rememberDcHaptics
import com.mohitarora.dualclone.ui.screen.settings.AppearanceSection
import com.mohitarora.dualclone.ui.screen.settings.DIAGNOSTICS_TAPS
import com.mohitarora.dualclone.ui.screen.settings.DataSection
import com.mohitarora.dualclone.ui.screen.settings.DeveloperSection
import com.mohitarora.dualclone.ui.screen.settings.HeaderSection
import com.mohitarora.dualclone.ui.screen.settings.ProfilesSection
import com.mohitarora.dualclone.ui.screen.settings.SettingsActions
import com.mohitarora.dualclone.ui.screen.settings.SettingsUiState
import com.mohitarora.dualclone.ui.screen.settings.SupportSection
import com.mohitarora.dualclone.ui.screen.settings.UpdatesSection
import com.mohitarora.dualclone.ui.screen.settings.DiagnosticsGate
import com.mohitarora.dualclone.ui.sheet.AttributionBottomSheet
import com.mohitarora.dualclone.ui.sheet.ShizukuSetupSheet
import com.mohitarora.dualclone.ui.sheet.SupportBottomSheet
import kotlinx.coroutines.launch

/**
 * One grouped page. Section order is fixed by the release plan and the content is capped at 640dp
 * so it stays readable on tablets.
 */
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    actions: SettingsActions,
    events: Flow<UiEvent>,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(events) {
        events.collect { snackbarHostState.showSnackbar(it.message.resolve(context)) }
    }
    val scope = rememberCoroutineScope()
    val haptics = rememberDcHaptics()

    var tapCount by rememberSaveable { mutableIntStateOf(0) }
    var showSupport by rememberSaveable { mutableStateOf(false) }
    var showAttribution by rememberSaveable { mutableStateOf(false) }
    var showShizukuSetup by rememberSaveable { mutableStateOf(false) }

    val unlockedMessage = stringResource(R.string.settings_diagnostics_unlocked)

    DcSubScreen(
        title = stringResource(R.string.settings_title),
        onBack = actions.onBack,
        snackbarHostState = snackbarHostState,
        modifier = modifier
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = DcSizes.settingsMaxWidth)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DcSpacing.lg)
                    .padding(top = DcSpacing.sm, bottom = DcSpacing.xl)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(DcSpacing.lg)
            ) {
                HeaderSection(
                    tapCount = tapCount,
                    onTap = {
                        if (DiagnosticsGate.unlocked) return@HeaderSection
                        tapCount++
                        if (tapCount >= DIAGNOSTICS_TAPS) {
                            DiagnosticsGate.unlocked = true
                            haptics.success()
                            scope.launch { snackbarHostState.showSnackbar(unlockedMessage) }
                        }
                    }
                )
                AppearanceSection(theme = state.theme, actions = actions)
                ProfilesSection(
                    state = state,
                    actions = actions,
                    onOpenShizukuSetup = { showShizukuSetup = true }
                )
                DataSection(state = state, actions = actions)
                UpdatesSection(state = state, actions = actions)
                SupportSection(
                    onOpenSupport = { showSupport = true },
                    onOpenAttribution = { showAttribution = true }
                )
                DeveloperSection()
                Text(
                    text = stringResource(R.string.settings_footer),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = DcSpacing.xs, vertical = DcSpacing.sm)
                )
            }
        }
    }

    if (showSupport) {
        SupportBottomSheet(onDismiss = { showSupport = false })
    }
    if (showAttribution) {
        AttributionBottomSheet(onDismiss = { showAttribution = false })
    }
    if (showShizukuSetup) {
        ShizukuSetupSheet(
            state = state.shizuku,
            onRequestPermission = actions.onRequestShizukuPermission,
            onRefresh = actions.onRefreshShizuku,
            onDismiss = { showShizukuSetup = false }
        )
    }
}
