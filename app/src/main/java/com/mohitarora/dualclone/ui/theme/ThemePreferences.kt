package com.mohitarora.dualclone.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import com.mohitarora.dualclone.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

const val DEFAULT_CUSTOM_HEX = 0xFF00BCD4L

enum class ThemePreset(
    @StringRes val labelRes: Int,
    val primaryColor: Color
) {
    DYNAMIC_MONET(R.string.theme_preset_wallpaper, Color(0xFF8C52FF)),
    ONE_UI_BLUE(R.string.theme_preset_one_ui, Color(0xFF2196F3)),
    DUAL_ORANGE(R.string.theme_preset_orange, Color(0xFFFF6D00)),
    EMERALD_GREEN(R.string.theme_preset_emerald, Color(0xFF00897B)),
    ELECTRIC_PURPLE(R.string.theme_preset_purple, Color(0xFF7E57C2)),
    SUNSET_ROSE(R.string.theme_preset_rose, Color(0xFFD81B60)),
    MIDNIGHT_MONO(R.string.theme_preset_mono, Color(0xFF808080)),
    CRIMSON_RED(R.string.theme_preset_red, Color(0xFFE53935)),
    OCEAN_CYAN(R.string.theme_preset_cyan, Color(0xFF00ACC1)),
    FOREST_GREEN(R.string.theme_preset_green, Color(0xFF388E3C)),
    CUSTOM(R.string.theme_preset_custom, Color(DEFAULT_CUSTOM_HEX));

    companion object {
        // NOTE: Blue first, because a wallpaper accent can be a pale or muddy color. Wallpaper stays one tap away in the row.
        val DEFAULT = ONE_UI_BLUE
    }
}

enum class ThemeMode(@StringRes val labelRes: Int) {
    SYSTEM(R.string.theme_mode_system),
    LIGHT(R.string.theme_mode_light),
    DARK(R.string.theme_mode_dark)
}

data class ThemeConfig(
    val preset: ThemePreset = ThemePreset.DEFAULT,
    val isPureBlack: Boolean = false,
    val customColorHex: Long = DEFAULT_CUSTOM_HEX,
    val mode: ThemeMode = ThemeMode.SYSTEM,
    val surfaceLift: Int = DEFAULT_SURFACE_LIFT
)

// NOTE: Track A replaces this class with a Hilt ThemeRepository that exposes the same ThemeConfig StateFlow.
class ThemePreferences(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    private val _themeConfig = MutableStateFlow(loadConfig())
    val themeConfig: StateFlow<ThemeConfig> = _themeConfig.asStateFlow()

    private fun loadConfig(): ThemeConfig {
        val preset = runCatching {
            ThemePreset.valueOf(prefs.getString(KEY_PRESET, null) ?: ThemePreset.DEFAULT.name)
        }.getOrDefault(ThemePreset.DEFAULT)
        val mode = runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_MODE, null) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
        return ThemeConfig(
            preset = preset,
            isPureBlack = prefs.getBoolean(KEY_PURE_BLACK, false),
            customColorHex = prefs.getLong(KEY_CUSTOM_HEX, DEFAULT_CUSTOM_HEX),
            mode = mode,
            surfaceLift = prefs.getInt(KEY_SURFACE_LIFT, DEFAULT_SURFACE_LIFT).coerceIn(0, 100)
        )
    }

    fun setPreset(preset: ThemePreset) {
        prefs.edit().putString(KEY_PRESET, preset.name).apply()
        _themeConfig.value = _themeConfig.value.copy(preset = preset)
    }

    /** Stores the custom colour and selects the Custom preset. Call only after the user presses Apply. */
    fun setCustomColor(colorHex: Long) {
        prefs.edit()
            .putString(KEY_PRESET, ThemePreset.CUSTOM.name)
            .putLong(KEY_CUSTOM_HEX, colorHex)
            .apply()
        _themeConfig.value = _themeConfig.value.copy(
            preset = ThemePreset.CUSTOM,
            customColorHex = colorHex
        )
    }

    fun setPureBlack(isPureBlack: Boolean) {
        prefs.edit().putBoolean(KEY_PURE_BLACK, isPureBlack).apply()
        _themeConfig.value = _themeConfig.value.copy(isPureBlack = isPureBlack)
    }

    fun setSurfaceLift(lift: Int) {
        val value = lift.coerceIn(0, 100)
        prefs.edit().putInt(KEY_SURFACE_LIFT, value).apply()
        _themeConfig.value = _themeConfig.value.copy(surfaceLift = value)
    }

    fun setMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_MODE, mode.name).apply()
        _themeConfig.value = _themeConfig.value.copy(mode = mode)
    }

    companion object {
        private const val PREFS_NAME = "dualclone_theme_prefs"
        private const val KEY_PRESET = "theme_preset"
        private const val KEY_PURE_BLACK = "pure_black"
        private const val KEY_CUSTOM_HEX = "custom_color_hex"
        private const val KEY_MODE = "theme_mode"
        private const val KEY_SURFACE_LIFT = "surface_lift"

        @Volatile
        private var instance: ThemePreferences? = null

        fun getInstance(context: Context): ThemePreferences {
            return instance ?: synchronized(this) {
                instance ?: ThemePreferences(context.applicationContext).also { instance = it }
            }
        }
    }
}
