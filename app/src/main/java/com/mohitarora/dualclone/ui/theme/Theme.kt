package com.mohitarora.dualclone.ui.theme

import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.mohitarora.dualclone.core.designsystem.DcMotion
import com.mohitarora.dualclone.core.designsystem.DcShapes
import com.mohitarora.dualclone.core.designsystem.DcTypography
import com.mohitarora.dualclone.core.designsystem.rememberReduceMotion

val DcMaterialShapes = androidx.compose.material3.Shapes(
    extraSmall = DcShapes.chip,
    small = DcShapes.chip,
    medium = DcShapes.field,
    large = DcShapes.card,
    extraLarge = DcShapes.dialog
)

/** Resolves System, Light or Dark against the device setting. */
@Composable
fun ThemeMode.resolveDark(): Boolean = when (this) {
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}

@Composable
fun DualCloneTheme(
    themeConfig: ThemeConfig = ThemeConfig(),
    darkTheme: Boolean = themeConfig.mode.resolveDark(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val amoled = darkTheme && themeConfig.isPureBlack
    val target: ColorScheme =
        if (themeConfig.preset == ThemePreset.DYNAMIC_MONET && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // NOTE: The wallpaper scheme is the system's own tonal palette, left untouched except for the pure black surfaces.
            if (darkTheme) dynamicDarkColorScheme(context).let { if (amoled) it.toAmoled(themeConfig.surfaceLift) else it } else dynamicLightColorScheme(context)
        } else {
            buildColorScheme(themeConfig.preset, darkTheme, amoled, themeConfig.customColorHex, themeConfig.surfaceLift)
        }

    val reduceMotion = rememberReduceMotion()
    val animated = target.animated(if (reduceMotion) 0 else DcMotion.themeCrossfadeMillis)

    MaterialTheme(
        colorScheme = animated,
        typography = DcTypography,
        shapes = DcMaterialShapes,
        content = content
    )
}

@Composable
private fun ColorScheme.animated(durationMillis: Int): ColorScheme {
    @Composable
    fun Color.a(): Color {
        val state: State<Color> = animateColorAsState(this, tween(durationMillis), label = "themeColor")
        return state.value
    }
    return copy(
        primary = primary.a(),
        onPrimary = onPrimary.a(),
        primaryContainer = primaryContainer.a(),
        onPrimaryContainer = onPrimaryContainer.a(),
        inversePrimary = inversePrimary.a(),
        secondary = secondary.a(),
        onSecondary = onSecondary.a(),
        secondaryContainer = secondaryContainer.a(),
        onSecondaryContainer = onSecondaryContainer.a(),
        tertiary = tertiary.a(),
        onTertiary = onTertiary.a(),
        tertiaryContainer = tertiaryContainer.a(),
        onTertiaryContainer = onTertiaryContainer.a(),
        background = background.a(),
        onBackground = onBackground.a(),
        surface = surface.a(),
        onSurface = onSurface.a(),
        surfaceVariant = surfaceVariant.a(),
        onSurfaceVariant = onSurfaceVariant.a(),
        surfaceTint = surfaceTint.a(),
        inverseSurface = inverseSurface.a(),
        inverseOnSurface = inverseOnSurface.a(),
        error = error.a(),
        onError = onError.a(),
        errorContainer = errorContainer.a(),
        onErrorContainer = onErrorContainer.a(),
        outline = outline.a(),
        outlineVariant = outlineVariant.a(),
        scrim = scrim.a(),
        surfaceBright = surfaceBright.a(),
        surfaceDim = surfaceDim.a(),
        surfaceContainer = surfaceContainer.a(),
        surfaceContainerHigh = surfaceContainerHigh.a(),
        surfaceContainerHighest = surfaceContainerHighest.a(),
        surfaceContainerLow = surfaceContainerLow.a(),
        surfaceContainerLowest = surfaceContainerLowest.a()
    )
}
