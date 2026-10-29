package com.mohitarora.dualclone.data.repository

import android.content.Context
import com.mohitarora.dualclone.ui.theme.ThemeConfig
import com.mohitarora.dualclone.ui.theme.ThemeMode
import com.mohitarora.dualclone.ui.theme.ThemePreferences
import com.mohitarora.dualclone.ui.theme.ThemePreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Injectable theme state.
 * Delegates to the process wide ThemePreferences instance so there is exactly one in memory copy
 * while screens still read it directly. Screens should depend on this class, then the delegate can move inside.
 */
@Singleton
class ThemeRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = ThemePreferences.getInstance(context)

    val config: StateFlow<ThemeConfig> get() = prefs.themeConfig

    fun setPreset(preset: ThemePreset) = prefs.setPreset(preset)
    fun setCustomColor(colorHex: Long) = prefs.setCustomColor(colorHex)
    fun setPureBlack(isPureBlack: Boolean) = prefs.setPureBlack(isPureBlack)
    fun setSurfaceLift(lift: Int) = prefs.setSurfaceLift(lift)
    fun setMode(mode: ThemeMode) = prefs.setMode(mode)
}
