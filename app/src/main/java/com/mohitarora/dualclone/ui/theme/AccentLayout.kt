package com.mohitarora.dualclone.ui.theme

/**
 * Where each accent lives. The row holds five presets and the More colors sheet holds the rest,
 * so no preset is ever listed in both places. Wallpaper only exists on Android 12 and later, and Rose fills its row slot below that.
 */
data class AccentLayout(val quick: List<ThemePreset>, val more: List<ThemePreset>)

fun accentLayout(wallpaperSupported: Boolean): AccentLayout {
    val quick = buildList {
        if (wallpaperSupported) add(ThemePreset.DYNAMIC_MONET)
        add(ThemePreset.ONE_UI_BLUE)
        add(ThemePreset.DUAL_ORANGE)
        add(ThemePreset.EMERALD_GREEN)
        add(ThemePreset.ELECTRIC_PURPLE)
        if (!wallpaperSupported) add(ThemePreset.SUNSET_ROSE)
    }
    val more = listOf(ThemePreset.SUNSET_ROSE, ThemePreset.CRIMSON_RED, ThemePreset.OCEAN_CYAN, ThemePreset.FOREST_GREEN, ThemePreset.MIDNIGHT_MONO).filter { it !in quick }
    return AccentLayout(quick, more)
}
