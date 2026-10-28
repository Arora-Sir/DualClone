package com.mohitarora.dualclone

import com.mohitarora.dualclone.ui.theme.ThemePreset
import com.mohitarora.dualclone.ui.theme.accentLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentLayoutTest {

    @Test
    fun rowHasFiveAndSheetNeverRepeatsThem() {
        for (wallpaper in listOf(true, false)) {
            val layout = accentLayout(wallpaper)
            assertEquals("row size, wallpaper=$wallpaper", 5, layout.quick.size)
            assertTrue("sheet repeats a row preset", layout.more.none { it in layout.quick })
            assertFalse(ThemePreset.CUSTOM in layout.quick)
            assertFalse(ThemePreset.CUSTOM in layout.more)
        }
    }

    @Test
    fun withWallpaperTheSheetHoldsRoseAndMono() {
        val layout = accentLayout(true)
        assertEquals(
            listOf(ThemePreset.DYNAMIC_MONET, ThemePreset.ONE_UI_BLUE, ThemePreset.DUAL_ORANGE, ThemePreset.EMERALD_GREEN, ThemePreset.ELECTRIC_PURPLE),
            layout.quick
        )
        assertEquals(listOf(ThemePreset.SUNSET_ROSE, ThemePreset.CRIMSON_RED, ThemePreset.OCEAN_CYAN, ThemePreset.FOREST_GREEN, ThemePreset.MIDNIGHT_MONO), layout.more)
    }

    @Test
    fun withoutWallpaperRoseMovesIntoTheRow() {
        val layout = accentLayout(false)
        assertFalse(ThemePreset.DYNAMIC_MONET in layout.quick)
        assertTrue(ThemePreset.SUNSET_ROSE in layout.quick)
        assertEquals(listOf(ThemePreset.CRIMSON_RED, ThemePreset.OCEAN_CYAN, ThemePreset.FOREST_GREEN, ThemePreset.MIDNIGHT_MONO), layout.more)
    }
}
