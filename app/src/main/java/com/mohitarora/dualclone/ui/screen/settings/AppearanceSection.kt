package com.mohitarora.dualclone.ui.screen.settings

import kotlin.math.roundToInt
import com.mohitarora.dualclone.ui.theme.DEFAULT_SURFACE_LIFT
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Slider
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
import com.mohitarora.dualclone.core.designsystem.components.DcSwitchRow
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
        DcSwitchRow(
            title = stringResource(R.string.appearance_amoled_title),
            subtitle = stringResource(if (isDark) R.string.appearance_amoled_summary else R.string.appearance_amoled_disabled),
            icon = Icons.Default.DarkMode,
            checked = theme.isPureBlack,
            enabled = isDark,
            onCheckedChange = actions.onPureBlackChange
        )
        // NOTE: The lift only matters for Pure black, so the row appears when that switch is on and the screen is dark.
        if (isDark && theme.isPureBlack) {
            DcSettingsDivider()
            SurfaceLiftRow(lift = theme.surfaceLift, onChange = actions.onSurfaceLiftChange)
        }
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

/** A slider from flat to lifted cards. The recommended position is the Super-Artemis Pure black look. */
@Composable
private fun SurfaceLiftRow(lift: Int, onChange: (Int) -> Unit) {
    Column(modifier = Modifier.padding(horizontal = DcSpacing.lg, vertical = DcSpacing.md)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.appearance_lift_title),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = if (lift == DEFAULT_SURFACE_LIFT) stringResource(R.string.appearance_lift_recommended) else lift.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        val label = stringResource(R.string.appearance_lift_title)
        Slider(
            value = lift.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = 0f..100f,
            modifier = Modifier.semantics { contentDescription = label }
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.appearance_lift_flat),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = stringResource(R.string.appearance_lift_lifted),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        DcButton(
            text = stringResource(R.string.appearance_lift_reset),
            onClick = { onChange(DEFAULT_SURFACE_LIFT) },
            style = DcButtonStyle.Text,
            enabled = lift != DEFAULT_SURFACE_LIFT
        )
    }
}
