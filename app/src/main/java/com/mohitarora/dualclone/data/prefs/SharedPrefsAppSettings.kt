package com.mohitarora.dualclone.data.prefs

import android.content.Context
import android.content.SharedPreferences
import com.mohitarora.dualclone.domain.repo.AppSettings
import com.mohitarora.dualclone.domain.repo.ReconcileStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedPrefsAppSettings @Inject constructor(@ApplicationContext context: Context) : AppSettings {

    private val prefs: SharedPreferences = context.getSharedPreferences("dualclone_settings", Context.MODE_PRIVATE)

    private val _advanced = MutableStateFlow(prefs.getBoolean(KEY_ADVANCED, false))
    private val _autoRefresh = MutableStateFlow(prefs.getBoolean(KEY_AUTO_REFRESH, false))
    private val _selected = MutableStateFlow(if (prefs.contains(KEY_SELECTED)) prefs.getInt(KEY_SELECTED, 0) else null)
    private val _reconcile = MutableStateFlow(
        ReconcileStatus(
            atMillis = prefs.getLong(KEY_RECONCILE_AT, 0L),
            ok = prefs.getBoolean(KEY_RECONCILE_OK, true),
            detail = prefs.getString(KEY_RECONCILE_DETAIL, "").orEmpty()
        )
    )

    override val showAdvancedUsers: StateFlow<Boolean> = _advanced.asStateFlow()
    override val autoRefreshLauncher: StateFlow<Boolean> = _autoRefresh.asStateFlow()
    override val selectedProfileId: StateFlow<Int?> = _selected.asStateFlow()
    override val lastReconcile: StateFlow<ReconcileStatus> = _reconcile.asStateFlow()

    override fun setShowAdvancedUsers(value: Boolean) {
        prefs.edit().putBoolean(KEY_ADVANCED, value).apply()
        _advanced.value = value
    }

    override fun setAutoRefreshLauncher(value: Boolean) {
        prefs.edit().putBoolean(KEY_AUTO_REFRESH, value).apply()
        _autoRefresh.value = value
    }

    override fun setSelectedProfileId(userId: Int?) {
        val editor = prefs.edit()
        if (userId == null) editor.remove(KEY_SELECTED) else editor.putInt(KEY_SELECTED, userId)
        editor.apply()
        _selected.value = userId
    }

    override fun setLastReconcile(status: ReconcileStatus) {
        prefs.edit()
            .putLong(KEY_RECONCILE_AT, status.atMillis)
            .putBoolean(KEY_RECONCILE_OK, status.ok)
            .putString(KEY_RECONCILE_DETAIL, status.detail)
            .apply()
        _reconcile.value = status
    }

    private companion object {
        const val KEY_ADVANCED = "show_advanced_users"
        const val KEY_AUTO_REFRESH = "auto_refresh_launcher"
        const val KEY_SELECTED = "selected_profile_id"
        const val KEY_RECONCILE_AT = "reconcile_at"
        const val KEY_RECONCILE_OK = "reconcile_ok"
        const val KEY_RECONCILE_DETAIL = "reconcile_detail"
    }
}
