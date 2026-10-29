package com.mohitarora.dualclone

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.mohitarora.dualclone.core.designsystem.DcContrast
import com.mohitarora.dualclone.ui.theme.DEFAULT_SURFACE_LIFT
import com.mohitarora.dualclone.ui.theme.ThemePreset
import com.mohitarora.dualclone.ui.theme.amoledLadder
import com.mohitarora.dualclone.ui.theme.buildColorScheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SurfaceLiftTest {

    private val levels = listOf(0, 10, 25, 50, 75, 90, 100)

    private fun grey(c: Color) = Math.round(c.red * 255)

    /** The recommended position is exactly the Super-Artemis look, cards on 0D0D0D and raised on 161616. */
    @Test
    fun theRecommendedLiftMatchesTheSuperArtemisLook() {
        val l = amoledLadder(DEFAULT_SURFACE_LIFT)
        assertEquals(DEFAULT_SURFACE_LIFT, 50)
        assertEquals(0x0D, grey(l.container))
        assertEquals(0x16, grey(l.high))
        assertEquals(0x1F, grey(l.highest))
        assertEquals(Color.Black, l.black)
    }

    @Test
    fun surfacesGetLighterAsTheLiftGrows() {
        val ladders = levels.map { amoledLadder(it) }
        ladders.zipWithNext().forEach { (a, b) ->
            assertTrue(grey(b.container) >= grey(a.container))
            assertTrue(grey(b.high) >= grey(a.high))
            assertTrue(grey(b.highest) >= grey(a.highest))
        }
    }

    /** Every step keeps a visible gap from its neighbour, even at the flat end, so cards never merge into the page. */
    @Test
    fun everyLevelKeepsTheStepsApart() {
        for (level in 0..100 step 5) {
            val l = amoledLadder(level)
            val steps = listOf(l.black, l.container, l.high, l.highest)
            steps.zipWithNext().forEach { (a, b) ->
                val delta = kotlin.math.abs(b.luminance() - a.luminance())
                assertTrue("level $level step delta %.4f".format(delta), delta >= 0.0015f)
            }
            val pageToCard = l.container.luminance() - l.black.luminance()
            assertTrue("level $level page to card %.4f".format(pageToCard), pageToCard >= 0.003f)
        }
    }

    @Test
    fun textStaysReadableOnEveryPresetAtBothEnds() {
        for (preset in ThemePreset.entries) for (level in listOf(0, 50, 100)) {
            val s = buildColorScheme(preset, dark = true, amoled = true, surfaceLift = level)
            val tag = "${preset.name}/lift $level"
            assertTrue("$tag onSurface", DcContrast.ratio(s.onSurface, s.surface) >= 4.5f)
            assertTrue("$tag onSurfaceVariant on highest", DcContrast.ratio(s.onSurfaceVariant, s.surfaceContainerHighest) >= 4.5f)
            assertTrue("$tag primary on card", DcContrast.ratio(s.primary, s.surfaceContainer) >= 3f)
        }
    }

    @Test
    fun outOfRangeLiftIsClamped() {
        assertEquals(amoledLadder(0), amoledLadder(-40))
        assertEquals(amoledLadder(100), amoledLadder(400))
    }

    /** The lift moves surfaces only. Accent roles are identical at every level. */
    @Test
    fun liftNeverChangesAccentRoles() {
        for (preset in ThemePreset.entries) {
            val a = buildColorScheme(preset, dark = true, amoled = true, surfaceLift = 0)
            val b = buildColorScheme(preset, dark = true, amoled = true, surfaceLift = 100)
            assertTrue(a.primary == b.primary && a.primaryContainer == b.primaryContainer && a.onSurface == b.onSurface)
        }
    }
}
