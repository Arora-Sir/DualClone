package com.mohitarora.dualclone.ui.components

import android.content.Context

/** Remembers whether the first run setup sheet was shown. A single boolean, so plain SharedPreferences is enough. */
class FirstRunPrefs(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("dualclone_ui_prefs", Context.MODE_PRIVATE)

    var onboardingSeen: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_SEEN, false)
        set(value) {
            prefs.edit().putBoolean(KEY_ONBOARDING_SEEN, value).apply()
        }

    private companion object {
        const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    }
}
