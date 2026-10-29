package com.mohitarora.dualclone.ui.screen.settings

import kotlin.math.roundToInt
import com.mohitarora.dualclone.ui.theme.DEFAULT_SURFACE_LIFT
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import android.os.Build
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.components.DcAccentRow
import com.mohitarora.dualclone.core.designsystem.components.DcPaletteCell
import com.mohitarora.dualclone.core.designsystem.components.DcSegmentedControl
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsDivider
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsGroup
import com.mohitarora.dualclone.core.designsystem.components.DcDialog
import com.mohitarora.dualclone.core.designsystem.components.DcStepPill
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcSettingsRow
import androidx.compose.material3.Switch
import com.mohitarora.dualclone.ui.sheet.AccentPickerSheet
import com.mohitarora.dualclone.ui.theme.ThemeConfig
import com.mohitarora.dualclone.ui.theme.ThemeMode
import com.mohitarora.dualclone.ui.theme.ThemePreset
import com.mohitarora.dualclone.ui.theme.accentLayout
import com.mohitarora.dualclone.ui.theme.resolveDark

@Composable
fun AppearanceSection(
    theme: ThemeConfig,
    actions: SettingsActions,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = theme.mode.resolveDark()
    var showPicker by rememberSaveable { mutableStateOf(false) }
    var showLift by rememberSaveable { mutableStateOf(false) }

    val wallpaperSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val wallpaperColor = remember(isDark, wallpaperSupported) {
        if (wallpaperSupported) {
            (if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)).primary
        } else {
            ThemePreset.DYNAMIC_MONET.primaryColor
        }
    }
    val layout = remember(wallpaperSupported) { accentLayout(wallpaperSupported) }
    @Composable
    fun cellFor(preset: ThemePreset): DcPaletteCell {
        val label = when {
            preset == ThemePreset.DYNAMIC_MONET && !wallpaperSupported -> R.string.theme_preset_default
            preset == ThemePreset.EMERALD_GREEN -> R.string.accent_teal
            else -> preset.labelRes
        }
        val color = when (preset) {
            ThemePreset.DYNAMIC_MONET -> wallpaperColor
            ThemePreset.CUSTOM -> Color(theme.customColorHex)
            else -> preset.primaryColor
        }
        return DcPaletteCell(id = preset.name, label = stringResource(label), color = color)
    }
    val dots = layout.quick.map { cellFor(it) }
    val extra = if (theme.preset !in layout.quick) cellFor(theme.preset) else null
    val selectedName = (dots + listOfNotNull(extra)).firstOrNull { it.id == theme.preset.name }?.label.orEmpty()

    DcSettingsGroup(header = stringResource(R.string.settings_section_appearance), modifier = modifier) {
        Column(modifier = Modifier.padding(DcSpacing.lg)) {
            Text(
                text = stringResource(R.string.appearance_mode_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .semantics { heading() }
                    .padding(bottom = DcSpacing.sm)
            )
            DcSegmentedControl(
                options = ThemeMode.entries,
                selected = theme.mode,
                label = { stringResource(it.labelRes) },
                onSelect = actions.onModeChange
            )
        }
        DcSettingsDivider()
        Column(modifier = Modifier.padding(DcSpacing.lg)) {
            Text(
                text = stringResource(R.string.appearance_palette_title_selected, selectedName),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier
                    .semantics { heading() }
                    .padding(bottom = DcSpacing.sm)
            )
            DcAccentRow(
                dots = dots,
                selectedId = theme.preset.name,
                extra = extra,
                onSelect = { actions.onPresetChange(ThemePreset.valueOf(it.id)) },
                onMore = { showPicker = true }
            )
        }
        DcSettingsDivider()
        val liftActive = isDark && theme.isPureBlack
        DcSettingsRow(
            title = stringResource(R.string.appearance_amoled_title),
            subtitle = if (liftActive) {
                stringResource(R.string.appearance_lift_summary, liftLevelText(theme.surfaceLift))
            } else {
                stringResource(if (isDark) R.string.appearance_amoled_summary else R.string.appearance_amoled_disabled)
            },
            icon = Icons.Default.DarkMode,
            enabled = isDark,
            onClick = { if (liftActive) showLift = true else actions.onPureBlackChange(!theme.isPureBlack) },
            trailing = {
                Switch(checked = theme.isPureBlack, onCheckedChange = actions.onPureBlackChange, enabled = isDark)
            }
        )
    }

    if (showLift) {
        SurfaceLiftDialog(lift = theme.surfaceLift, onChange = actions.onSurfaceLiftChange, onDismiss = { showLift = false })
    }

    if (showPicker) {
        AccentPickerSheet(
            theme = theme,
            morePresets = layout.more,
            onApplyPreset = {
                actions.onPresetChange(it)
                showPicker = false
            },
            onApplyCustom = {
                actions.onApplyCustomColor(it)
                showPicker = false
            },
            onDismiss = { showPicker = false }
        )
    }
}

/** Ten step pill from flat to lifted cards. Step 5 is the Super-Artemis Pure black look. */
@Composable
private fun SurfaceLiftDialog(lift: Int, onChange: (Int) -> Unit, onDismiss: () -> Unit) {
    DcDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.appearance_lift_title),
        actions = {
            DcButton(text = stringResource(R.string.appearance_lift_reset), onClick = { onChange(DEFAULT_SURFACE_LIFT) }, style = DcButtonStyle.Outlined)
            Spacer(Modifier.weight(1f))
            DcButton(text = stringResource(R.string.action_done), onClick = onDismiss, style = DcButtonStyle.Tonal)
        }
    ) {
        val step = (lift / 10f).roundToInt().coerceIn(0, 10)
        val valueText = liftLevelText(lift)
        Text(text = valueText, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        DcStepPill(
            value = step,
            steps = 10,
            onValueChange = { onChange(it * 10) },
            description = stringResource(R.string.appearance_lift_title),
            valueText = valueText,
            modifier = Modifier.padding(top = DcSpacing.xs)
        )
    }
}

/** "Recommended" at the default step, otherwise "4 of 10". */
@Composable
private fun liftLevelText(lift: Int): String =
    if (lift == DEFAULT_SURFACE_LIFT) stringResource(R.string.appearance_lift_recommended)
    else stringResource(R.string.appearance_lift_step, (lift / 10f).roundToInt().coerceIn(0, 10))
