package com.mohitarora.dualclone

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import com.mohitarora.dualclone.core.designsystem.DcContrast
import com.mohitarora.dualclone.core.designsystem.DcStatus
import com.mohitarora.dualclone.core.designsystem.DcTone
import com.mohitarora.dualclone.ui.theme.ThemePreset
import com.mohitarora.dualclone.ui.theme.buildColorScheme
import org.junit.Assert.assertTrue
import org.junit.Test

/** Every preset in light, dark and AMOLED keeps the Blueprint contrast floors. */
class ThemeContrastTest {

    private data class Variant(val name: String, val dark: Boolean, val amoled: Boolean)

    private val variants = listOf(
        Variant("light", dark = false, amoled = false),
        Variant("dark", dark = true, amoled = false),
        Variant("amoled", dark = true, amoled = true)
    )

    private fun check(label: String, fg: Color, bg: Color, min: Float) {
        val ratio = DcContrast.ratio(fg, bg)
        assertTrue("$label contrast %.2f is below $min".format(ratio), ratio >= min)
    }

    @Test
    fun presetSchemesKeepContrast() {
        for (preset in ThemePreset.entries) for (v in variants) {
            val s = buildColorScheme(preset, v.dark, v.amoled)
            val tag = "${preset.name}/${v.name}"
            check("$tag primary on container", s.primary, s.surfaceContainer, 3f)
            check("$tag onPrimary on primary", s.onPrimary, s.primary, 3f)
            check("$tag onPrimaryContainer", s.onPrimaryContainer, s.primaryContainer, 4.5f)
            check("$tag onSecondaryContainer", s.onSecondaryContainer, s.secondaryContainer, 4.5f)
            check("$tag onSurface", s.onSurface, s.surface, 4.5f)
            check("$tag onSurfaceVariant", s.onSurfaceVariant, s.surfaceContainerHighest, 4.5f)
        }
    }

    @Test
    fun statusBadgesKeepContrast() {
        for (preset in ThemePreset.entries) for (v in variants) {
            val s = buildColorScheme(preset, v.dark, v.amoled)
            for (tone in DcTone.entries) {
                val c = DcStatus.toneColors(tone, s.surfaceContainer, s.primary, s.onSurfaceVariant, v.dark)
                val tag = "${preset.name}/${v.name}/${tone.name}"
                check("$tag accent on card", c.accent, s.surfaceContainer, DcContrast.BADGE_CONTAINER_MIN)
                check("$tag text on badge", c.content, c.container, DcContrast.TEXT_MIN)
            }
        }
    }


    /** Each step of the container ladder must be visibly different from its neighbour, so cards never merge into the page. */
    @Test
    fun containerLadderStepsAreDistinct() {
        val minDelta = 0.0015f
        for (preset in ThemePreset.entries) for (v in variants) {
            val s = buildColorScheme(preset, v.dark, v.amoled)
            val steps = listOf(
                "lowest" to s.surfaceContainerLowest,
                "low" to s.surfaceContainerLow,
                "container" to s.surfaceContainer,
                "high" to s.surfaceContainerHigh,
                "highest" to s.surfaceContainerHighest
            )
            steps.zipWithNext().forEach { (a, b) ->
                // NOTE: Pure black deliberately keeps the page and the two lowest steps at black, like the Super-Artemis overlay.
                if (v.amoled && a.second == Color.Black && b.second == Color.Black) return@forEach
                val delta = kotlin.math.abs(a.second.luminance() - b.second.luminance())
                assertTrue("${preset.name}/${v.name} ${a.first} to ${b.first} luminance delta %.4f is below $minDelta".format(delta), delta >= minDelta)
            }
            val pageToCard = kotlin.math.abs(s.surface.luminance() - s.surfaceContainer.luminance())
            assertTrue("${preset.name}/${v.name} card does not separate from the page (%.4f)".format(pageToCard), pageToCard >= 0.003f)
        }
    }

    /** Accents are generated, never hand mixed. Primary and its container sit on the fixed Material tones of the seed hue. */
    @Test
    fun accentsFollowMaterialTones() {
        val generated = ThemePreset.entries.filter { it != ThemePreset.MIDNIGHT_MONO }
        for (preset in generated) {
            val dark = buildColorScheme(preset, dark = true, amoled = false)
            val light = buildColorScheme(preset, dark = false, amoled = false)
            fun tone(c: Color) = com.materialkolor.hct.Hct.fromInt(c.toArgb()).tone
            assertTrue("${preset.name} dark primary tone ${tone(dark.primary)}", tone(dark.primary) in 78.0..82.0)
            assertTrue("${preset.name} dark container tone ${tone(dark.primaryContainer)}", tone(dark.primaryContainer) in 28.0..32.0)
            assertTrue("${preset.name} light primary tone ${tone(light.primary)}", tone(light.primary) in 38.0..42.0)
            assertTrue("${preset.name} light container tone ${tone(light.primaryContainer)}", tone(light.primaryContainer) in 88.0..92.0)
        }
    }

    /** Pure black only changes surfaces. Every accent role is identical to plain dark. */
    @Test
    fun amoledKeepsAccentRolesOfDark() {
        for (preset in ThemePreset.entries) {
            val dark = buildColorScheme(preset, dark = true, amoled = false)
            val amoled = buildColorScheme(preset, dark = true, amoled = true)
            assertTrue("${preset.name} primary changed", dark.primary == amoled.primary)
            assertTrue("${preset.name} primaryContainer changed", dark.primaryContainer == amoled.primaryContainer)
            assertTrue("${preset.name} onSurface changed", dark.onSurface == amoled.onSurface)
        }
    }

    @Test
    fun amoledUsesPureBlackBackground() {
        for (preset in ThemePreset.entries) {
            val s = buildColorScheme(preset, dark = true, amoled = true)
            assertTrue(s.background == Color.Black && s.surface == Color.Black && s.surfaceContainerLowest == Color.Black)
        }
    }
}
