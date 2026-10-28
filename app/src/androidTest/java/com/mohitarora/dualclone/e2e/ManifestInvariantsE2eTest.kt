package com.mohitarora.dualclone.e2e

import android.content.Intent
import android.content.pm.PackageManager
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Static release invariants checked on the installed build. No Hilt needed. */
class ManifestInvariantsE2eTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    // NOTE: INTERNET and REQUEST_INSTALL_PACKAGES exist only for the in-app updater. Any other network related permission fails.
    @Test
    fun no_unexpected_network_or_install_permissions() {
        val info = context.packageManager.getPackageInfo(context.packageName, PackageManager.GET_PERMISSIONS)
        val requested = info.requestedPermissions.orEmpty().toSet()
        val watched = setOf(
            "android.permission.ACCESS_WIFI_STATE", "android.permission.CHANGE_NETWORK_STATE",
            "android.permission.READ_PHONE_STATE", "android.permission.ACCESS_FINE_LOCATION",
            "android.permission.ACCESS_COARSE_LOCATION", "android.permission.READ_CONTACTS",
            "android.permission.GET_ACCOUNTS", "android.permission.MANAGE_EXTERNAL_STORAGE"
        )
        assertFalse("unexpected permissions: ${requested intersect watched}", requested.any { it in watched })
        assertTrue(requested.contains("android.permission.INTERNET"))
    }

    @Test
    fun exactly_one_launcher_activity() {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setPackage(context.packageName)
        assertEquals(1, context.packageManager.queryIntentActivities(intent, 0).size)
    }
}
