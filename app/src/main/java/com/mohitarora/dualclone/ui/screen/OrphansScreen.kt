package com.mohitarora.dualclone.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.Column
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSizes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcCard
import com.mohitarora.dualclone.core.designsystem.components.DcConfirmDialog
import com.mohitarora.dualclone.core.designsystem.components.DcEmptyState
import com.mohitarora.dualclone.core.designsystem.components.DcInfoCard
import com.mohitarora.dualclone.core.designsystem.components.DcSubScreen
import com.mohitarora.dualclone.domain.model.OrphanEntry
import com.mohitarora.dualclone.ui.viewmodel.OrphansUiState
import com.mohitarora.dualclone.ui.viewmodel.key

/** Clones whose host app is gone. Native apps of a profile are labelled and never selected by default. */
@Composable
fun OrphansScreen(
    state: OrphansUiState,
    onScan: () -> Unit,
    onToggle: (OrphanEntry) -> Unit,
    onPurge: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var confirm by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { if (!state.hasScanned) onScan() }

    DcSubScreen(title = stringResource(R.string.orphans_title), onBack = onBack, modifier = modifier) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            when {
                state.isScanning && state.items.isEmpty() -> CircularProgressIndicator(Modifier.padding(DcSpacing.xxl))
                state.hasScanned && state.items.isEmpty() -> DcEmptyState(
                    icon = Icons.Default.CheckCircle,
                    title = stringResource(R.string.orphans_empty_title),
                    body = stringResource(if (state.failedUsers.isEmpty()) R.string.orphans_empty_body else R.string.orphans_failed_body),
                    modifier = Modifier.fillMaxSize(),
                    action = { DcButton(stringResource(R.string.orphans_rescan), onScan, style = DcButtonStyle.Tonal) }
                )
                else -> Column(
                    modifier = Modifier.widthIn(max = DcSizes.settingsMaxWidth).fillMaxWidth().fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(DcSpacing.lg),
                        verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
                    ) {
                        item {
                            DcInfoCard(
                                title = stringResource(R.string.orphans_info_title),
                                body = stringResource(R.string.orphans_info_body),
                                tone = DcTone.Info
                            )
                        }
                        items(state.items, key = { it.key() }) { entry ->
                            val checked = entry.key() in state.selected
                            DcCard(
                                modifier = Modifier.toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle(entry) }),
                                selected = checked
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Checkbox(checked = checked, onCheckedChange = null)
                                    Column(Modifier.padding(start = DcSpacing.md).weight(1f)) {
                                        Text(entry.packageName, style = MaterialTheme.typography.titleSmall)
                                        Text(
                                            stringResource(R.string.profiles_user_id, entry.userId),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (entry.onlyInProfile) {
                                            DcBadge(stringResource(R.string.orphans_only_in_profile), tone = DcTone.Warning, modifier = Modifier.padding(top = DcSpacing.xs))
                                        }
                                    }
                                }
                            }
                        }
                    }
                    DcButton(
                        text = pluralStringResource(R.plurals.orphans_remove, state.selected.size, state.selected.size),
                        onClick = { confirm = true },
                        style = DcButtonStyle.Destructive,
                        enabled = state.selected.isNotEmpty() && !state.isScanning,
                        modifier = Modifier.fillMaxWidth().padding(DcSpacing.lg)
                    )
                }
            }
        }
    }
    if (confirm) {
        DcConfirmDialog(
            title = pluralStringResource(R.plurals.orphans_confirm_title, state.selected.size, state.selected.size),
            message = stringResource(R.string.orphans_confirm_body),
            confirmLabel = stringResource(R.string.action_remove),
            destructive = true,
            onConfirm = {
                confirm = false
                onPurge()
            },
            onDismiss = { confirm = false }
        )
    }
}
