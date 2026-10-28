package com.mohitarora.dualclone.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/** Semantic colour roles for badges, banners and status text. Neutral and Primary follow the active scheme. */
enum class DcTone { Success, Error, Warning, Info, Primary, Neutral }

@Immutable
data class DcStatusPalette(val success: Color, val error: Color, val warning: Color, val info: Color)

// NOTE: Values are Blueprint Pillar 10.3. Container alpha is 0.12 and border alpha is 0.28.
val DcStatusLight = DcStatusPalette(
    success = Color(0xFF059669),
    error = Color(0xFFDC2626),
    warning = Color(0xFFD97706),
    info = Color(0xFF2563EB)
)

val DcStatusDark = DcStatusPalette(
    success = Color(0xFF10B981),
    error = Color(0xFFEF4444),
    warning = Color(0xFFF59E0B),
    info = Color(0xFF3B82F6)
)

/** Resolved colours for one badge or banner on a given surface. */
@Immutable
data class DcToneColors(val container: Color, val border: Color, val content: Color, val accent: Color)

object DcStatus {
    const val CONTAINER_ALPHA = 0.12f
    const val BORDER_ALPHA = 0.28f

    fun palette(darkTheme: Boolean): DcStatusPalette = if (darkTheme) DcStatusDark else DcStatusLight

    /**
     * Computes badge colours. The accent (dot, icon, border) keeps 3 to 1 against [surface],
     * and the text keeps 4.5 to 1 against the tinted container.
     */
    fun toneColors(
        tone: DcTone,
        surface: Color,
        primary: Color,
        onSurfaceVariant: Color,
        darkTheme: Boolean
    ): DcToneColors {
        val p = palette(darkTheme)
        val seed = when (tone) {
            DcTone.Success -> p.success
            DcTone.Error -> p.error
            DcTone.Warning -> p.warning
            DcTone.Info -> p.info
            DcTone.Primary -> primary
            DcTone.Neutral -> onSurfaceVariant
        }
        val accent = DcContrast.ensure(seed, surface, DcContrast.BADGE_CONTAINER_MIN)
        val container = DcContrast.over(accent.copy(alpha = CONTAINER_ALPHA), surface)
        val content = DcContrast.ensure(accent, container, DcContrast.TEXT_MIN)
        return DcToneColors(
            container = container,
            border = accent.copy(alpha = BORDER_ALPHA),
            content = content,
            accent = accent
        )
    }

    @Composable
    @ReadOnlyComposable
    fun colors(tone: DcTone): DcToneColors {
        val scheme = MaterialTheme.colorScheme
        return toneColors(
            tone = tone,
            surface = scheme.surfaceContainer,
            primary = scheme.primary,
            onSurfaceVariant = scheme.onSurfaceVariant,
            darkTheme = scheme.surface.luminance() < 0.4f
        )
    }
}
