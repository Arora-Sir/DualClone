package com.mohitarora.dualclone.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.materialkolor.hct.Hct
import com.materialkolor.scheme.DynamicScheme
import com.materialkolor.scheme.SchemeMonochrome
import com.materialkolor.scheme.SchemeTonalSpot

/**
 * The one colour a preset is generated from. The swatch the user taps in the palette is this same colour,
 * so what you pick is the hue you get. Wallpaper falls back to the brand purple below Android 12.
 */
internal fun seedFor(preset: ThemePreset, customHex: Long): Color =
    if (preset == ThemePreset.CUSTOM) Color(customHex) else preset.primaryColor

/**
 * Builds the full scheme for a preset the way Material You does. The seed goes through HCT tonal palettes,
 * so primary, containers, surfaces and outlines all come from one hue with fixed tone steps and guaranteed contrast.
 * It is a pure function, so unit tests can check every preset in light, dark and AMOLED without a device.
 */
fun buildColorScheme(
    preset: ThemePreset,
    dark: Boolean,
    amoled: Boolean,
    customHex: Long = DEFAULT_CUSTOM_HEX,
    surfaceLift: Int = DEFAULT_SURFACE_LIFT
): ColorScheme {
    val source = Hct.fromInt(seedFor(preset, customHex).toArgb())
    // NOTE: Mono uses the monochrome scheme, because tonal spot on a grey seed would still pick a faint hue.
    val scheme = if (preset == ThemePreset.MIDNIGHT_MONO) {
        SchemeMonochrome(source, dark, CONTRAST_STANDARD)
    } else {
        SchemeTonalSpot(source, dark, CONTRAST_STANDARD)
    }
    val colors = scheme.toColorScheme()
    return if (dark && amoled) colors.toAmoled(surfaceLift) else colors
}

private const val CONTRAST_STANDARD = 0.0

private fun DynamicScheme.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = Color(primary),
        onPrimary = Color(onPrimary),
        primaryContainer = Color(primaryContainer),
        onPrimaryContainer = Color(onPrimaryContainer),
        inversePrimary = Color(inversePrimary),
        secondary = Color(secondary),
        onSecondary = Color(onSecondary),
        secondaryContainer = Color(secondaryContainer),
        onSecondaryContainer = Color(onSecondaryContainer),
        tertiary = Color(tertiary),
        onTertiary = Color(onTertiary),
        tertiaryContainer = Color(tertiaryContainer),
        onTertiaryContainer = Color(onTertiaryContainer),
        background = Color(background),
        onBackground = Color(onBackground),
        surface = Color(surface),
        onSurface = Color(onSurface),
        surfaceVariant = Color(surfaceVariant),
        onSurfaceVariant = Color(onSurfaceVariant),
        surfaceTint = Color(surfaceTint),
        inverseSurface = Color(inverseSurface),
        inverseOnSurface = Color(inverseOnSurface),
        error = Color(error),
        onError = Color(onError),
        errorContainer = Color(errorContainer),
        onErrorContainer = Color(onErrorContainer),
        outline = Color(outline),
        outlineVariant = Color(outlineVariant),
        scrim = Color(scrim),
        surfaceBright = Color(surfaceBright),
        surfaceDim = Color(surfaceDim),
        surfaceContainerLowest = Color(surfaceContainerLowest),
        surfaceContainerLow = Color(surfaceContainerLow),
        surfaceContainer = Color(surfaceContainer),
        surfaceContainerHigh = Color(surfaceContainerHigh),
        surfaceContainerHighest = Color(surfaceContainerHighest)
    )
}

/**
 * Pure black windows with barely lifted cards, so an OLED panel switches its pixels off and the accent still carries the screen.
 * Only the surface steps change. Every accent role keeps the tonal values, so contrast guarantees survive.
 * Also applied to the wallpaper scheme, which the system generates.
 */
fun ColorScheme.toAmoled(surfaceLift: Int = DEFAULT_SURFACE_LIFT): ColorScheme {
    val ladder = amoledLadder(surfaceLift)
    return copy(
        background = ladder.black,
        surface = ladder.black,
        surfaceDim = ladder.black,
        surfaceContainerLowest = ladder.black,
        surfaceContainerLow = ladder.black,
        surfaceContainer = ladder.container,
        surfaceContainerHigh = ladder.high,
        surfaceContainerHighest = ladder.highest,
        surfaceBright = ladder.highest,
        surfaceVariant = ladder.high,
        outlineVariant = ladder.outlineVariant
    )
}
