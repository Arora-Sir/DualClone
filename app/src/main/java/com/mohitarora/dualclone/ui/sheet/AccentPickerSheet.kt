package com.mohitarora.dualclone.ui.sheet

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.designsystem.DcColorMath
import com.mohitarora.dualclone.core.designsystem.DcContrast
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcSpacing
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.core.designsystem.components.DcBadge
import com.mohitarora.dualclone.core.designsystem.components.DcButton
import com.mohitarora.dualclone.core.designsystem.components.DcButtonRow
import com.mohitarora.dualclone.core.designsystem.components.DcButtonStyle
import com.mohitarora.dualclone.core.designsystem.components.DcColorPicker
import com.mohitarora.dualclone.core.designsystem.components.DcPaletteCell
import com.mohitarora.dualclone.core.designsystem.components.DcPaletteGrid
import com.mohitarora.dualclone.core.designsystem.components.DcSheet
import com.mohitarora.dualclone.ui.theme.DEFAULT_CUSTOM_HEX
import com.mohitarora.dualclone.ui.theme.ThemeConfig
import com.mohitarora.dualclone.ui.theme.ThemePreset
import com.mohitarora.dualclone.ui.theme.buildColorScheme

/**
 * The More colors sheet. It lists only the presets that are not in the Settings row, plus the custom colour.
 * A preset applies and closes at once. Custom edits a draft and only applies on Apply.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccentPickerSheet(
    theme: ThemeConfig,
    morePresets: List<ThemePreset>,
    onApplyPreset: (ThemePreset) -> Unit,
    onApplyCustom: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    val initial = remember(theme.customColorHex) { DcColorMath.argbToHsv(theme.customColorHex.toInt()) }
    var hue by rememberSaveable { mutableFloatStateOf(initial[0]) }
    var saturation by rememberSaveable { mutableFloatStateOf(initial[1]) }
    var value by rememberSaveable { mutableFloatStateOf(initial[2]) }
    var hexText by rememberSaveable { mutableStateOf(DcColorMath.toHex(theme.customColorHex.toInt())) }

    // NOTE: The hex text is the source of truth. The picker writes it, and a typed code is used exactly as typed.
    val parsed = DcColorMath.parseHex(hexText)
    val customHex = (parsed ?: DcColorMath.hsvToArgb(hue, saturation, value)).toLong() and 0xFFFFFFFFL
    val cells = morePresets.map { DcPaletteCell(id = it.name, label = stringResource(it.labelRes), color = it.primaryColor) }

    fun setFromHsv(h: Float, s: Float, v: Float) {
        hue = h; saturation = s; value = v
        hexText = DcColorMath.toHex(DcColorMath.hsvToArgb(h, s, v))
    }

    DcSheet(onDismiss = onDismiss, title = stringResource(R.string.accent_more)) {
        if (cells.isNotEmpty()) {
            DcPaletteGrid(
                cells = cells,
                selectedId = theme.preset.name,
                onSelect = { onApplyPreset(ThemePreset.valueOf(it.id)) }
            )
        }
        Text(
            text = stringResource(R.string.theme_preset_custom),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.custom_color_help),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Preview(customHex)
        DcColorPicker(hue = hue, saturation = saturation, value = value, onChange = ::setFromHsv)
        Row(horizontalArrangement = Arrangement.spacedBy(DcSpacing.sm), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = hexText,
                onValueChange = { raw ->
                    val cleaned = raw.removePrefix("#").filter { it.isLetterOrDigit() }.take(6).uppercase()
                    hexText = cleaned
                    DcColorMath.parseHex(cleaned)?.let { argb ->
                        val hsv = DcColorMath.argbToHsv(argb)
                        hue = hsv[0]; saturation = hsv[1]; value = hsv[2]
                    }
                },
                label = { Text(stringResource(R.string.custom_color_hex_label)) },
                prefix = { Text("#") },
                isError = parsed == null,
                supportingText = { if (parsed == null) Text(stringResource(R.string.custom_color_hex_error)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                shape = DcShapes.field,
                modifier = Modifier.weight(1f)
            )
            DcButton(
                text = stringResource(R.string.custom_color_reset),
                onClick = {
                    val hsv = DcColorMath.argbToHsv(DEFAULT_CUSTOM_HEX.toInt())
                    setFromHsv(hsv[0], hsv[1], hsv[2])
                },
                style = DcButtonStyle.Text
            )
        }
        DcButtonRow {
            DcButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDismiss,
                style = DcButtonStyle.Outlined,
                modifier = Modifier.slot()
            )
            DcButton(
                text = stringResource(R.string.action_apply),
                onClick = { onApplyCustom(0xFF000000L or (customHex and 0xFFFFFFL)) },
                enabled = parsed != null,
                modifier = Modifier.slot()
            )
        }
    }
}

/**
 * Shows what Apply will produce, not the raw seed. The seed goes through the same tonal generator as the app theme,
 * in the mode that is active now, so the button, badge and contrast line are the real result.
 */
@Composable
private fun Preview(customHex: Long) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val scheme = remember(customHex, dark) { buildColorScheme(ThemePreset.CUSTOM, dark, amoled = false, customHex = customHex) }
    val ratio = DcContrast.ratio(scheme.primary, scheme.surfaceContainerHigh)
    MaterialTheme(colorScheme = scheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(DcShapes.card)
                .background(scheme.surfaceContainerHigh)
                .padding(DcSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(DcSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.custom_color_preview_title),
                style = MaterialTheme.typography.titleMedium,
                color = scheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(DcSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                // NOTE: A plain box, not a button, so the preview is never focusable or clickable.
                Box(
                    modifier = Modifier
                        .clip(DcShapes.button)
                        .background(scheme.primary)
                        .padding(horizontal = DcSpacing.xl, vertical = DcSpacing.md)
                ) {
                    Text(
                        text = stringResource(R.string.custom_color_preview_button),
                        style = MaterialTheme.typography.labelLarge,
                        color = scheme.onPrimary
                    )
                }
                DcBadge(text = stringResource(R.string.custom_color_preview_badge), tone = DcTone.Primary)
            }
            Text(
                text = stringResource(
                    if (dark) R.string.custom_color_contrast_dark else R.string.custom_color_contrast_light,
                    ratio
                ),
                style = MaterialTheme.typography.bodySmall,
                color = scheme.onSurfaceVariant
            )
        }
    }
}
