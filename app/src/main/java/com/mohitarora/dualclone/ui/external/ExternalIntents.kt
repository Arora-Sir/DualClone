package com.mohitarora.dualclone.ui.external

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import com.mohitarora.dualclone.R

/** Every outbound intent in the app goes through here so failures never crash and never show a raw Toast. */
object ExternalIntents {

    private const val SHIZUKU_PACKAGE = "moe.shizuku.privileged.api"
    private const val DUAL_APP_AGENT = "com.samsung.android.da.daagent"
    private val SECURE_FOLDER_PACKAGES = listOf("com.samsung.knox.securefolder", "com.sec.knox.switcher")

    /** Opens [url] in the default browser and bypasses app link hijacking. Returns false when nothing can open it. */
    fun openUrl(context: Context, url: String): Boolean {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            val probe = Intent(Intent.ACTION_VIEW, Uri.parse("https://")).addCategory(Intent.CATEGORY_BROWSABLE)
            val browser = context.packageManager
                .resolveActivity(probe, PackageManager.MATCH_DEFAULT_ONLY)
                ?.activityInfo?.packageName
            if (browser != null && browser != "android") view.setPackage(browser)
            context.startActivity(view)
            true
        } catch (_: Exception) {
            try {
                view.setPackage(null)
                context.startActivity(view)
                true
            } catch (_: Exception) {
                AppToast.show(context, context.getString(R.string.error_no_app_for_link))
                false
            }
        }
    }

    /** Launches Samsung Secure Folder. Never sends a shell command to the Knox profile. */
    fun openSecureFolder(context: Context): Boolean {
        val intent = SECURE_FOLDER_PACKAGES.firstNotNullOfOrNull { context.packageManager.getLaunchIntentForPackage(it) }
        if (intent == null) {
            AppToast.show(context, context.getString(R.string.error_secure_folder_missing))
            return false
        }
        return start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun hasSecureFolder(context: Context): Boolean =
        SECURE_FOLDER_PACKAGES.any { context.packageManager.getLaunchIntentForPackage(it) != null }

    fun openShizuku(context: Context): Boolean {
        val intent = context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE)
        if (intent == null) {
            AppToast.show(context, context.getString(R.string.error_shizuku_not_installed))
            return false
        }
        return start(context, intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun isShizukuInstalled(context: Context): Boolean =
        context.packageManager.getLaunchIntentForPackage(SHIZUKU_PACKAGE) != null

    /** Developer options hold the Wireless debugging switch that Shizuku needs. */
    fun openDeveloperSettings(context: Context): Boolean =
        start(context, Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

    /** Opens the Samsung Dual Messenger settings, falling back to the general settings screen. */
    fun openDualMessengerSettings(context: Context): Boolean {
        val candidates = listOf(
            Intent("com.samsung.android.da.daagent.DUAL_APP_SETTINGS"),
            Intent().setClassName(DUAL_APP_AGENT, "$DUAL_APP_AGENT.DualAppSettingActivity"),
            Intent(Settings.ACTION_SETTINGS)
        )
        return candidates.any { start(context, it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
    }

    fun hasUpiApp(context: Context): Boolean =
        context.packageManager
            .queryIntentActivities(Intent(Intent.ACTION_VIEW, Uri.parse(ProjectLinks.upiPayment)), PackageManager.MATCH_DEFAULT_ONLY)
            .isNotEmpty()

    /** Starts the UPI chooser. Returns false when no UPI app exists so the caller can copy the ID instead. */
    fun payWithUpi(context: Context): Boolean {
        if (!hasUpiApp(context)) return false
        val chooser = Intent.createChooser(
            Intent(Intent.ACTION_VIEW, Uri.parse(ProjectLinks.upiPayment)),
            context.getString(R.string.support_upi_chooser)
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return start(context, chooser)
    }

    fun copyUpiId(context: Context) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("UPI ID", ProjectLinks.UPI_ID))
    }

    private fun start(context: Context, intent: Intent): Boolean = try {
        context.startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    } catch (_: SecurityException) {
        false
    }
}
