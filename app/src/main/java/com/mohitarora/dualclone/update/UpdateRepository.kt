package com.mohitarora.dualclone.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mohitarora.dualclone.BuildConfig
import com.mohitarora.dualclone.R
import com.mohitarora.dualclone.core.common.UiText
import com.mohitarora.dualclone.domain.backup.MiniJson
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/** What the screen must do to start the install. */
sealed interface InstallRequest {
    /** The user has not allowed installs from DualClone yet. Open this settings screen first. */
    data class NeedsPermission(val settingsIntent: Intent) : InstallRequest

    /** The verified APK is ready. Start this intent from an Activity. */
    data class Ready(val installIntent: Intent) : InstallRequest

    /** The file failed verification again or is gone. The state is Failed. */
    data object Unavailable : InstallRequest
}

/**
 * Update entry point for the UI and the optional daily check.
 * Checks run on user action, plus the daily worker when the user switched it on.
 * A download starts only through download(), which the user triggers after seeing the changelog.
 */
@Singleton
class UpdateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    client: UpdateClient
) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    private val updatesDir = File(context.cacheDir, UPDATES_DIR)

    private val engine = UpdateEngine(
        client = client,
        updatesDir = updatesDir,
        currentVersion = BuildConfig.VERSION_NAME,
        toText = ::errorText,
        onReleaseChanged = ::remember,
        initialRelease = recall()
    )

    val state: StateFlow<UpdateState> = engine.state
    val currentVersion: String = BuildConfig.VERSION_NAME

    private val _dailyCheck = MutableStateFlow(prefs.getBoolean(KEY_DAILY, false))

    /** Off by default. When on, a WorkManager job checks once a day and never downloads. */
    val dailyCheckEnabled: StateFlow<Boolean> = _dailyCheck.asStateFlow()

    suspend fun check() = engine.check()

    suspend fun download() = engine.download()

    fun reset() = engine.reset()

    /** Verifies the file once more, then builds the install intent. */
    suspend fun installRequest(): InstallRequest {
        val apk = engine.verifiedApk() ?: return InstallRequest.Unavailable
        if (!context.packageManager.canRequestPackageInstalls()) {
            return InstallRequest.NeedsPermission(
                Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            )
        }
        val uri = FileProvider.getUriForFile(context, authority(context), apk)
        val intent = Intent(Intent.ACTION_VIEW)
            .setDataAndType(uri, APK_MIME)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        return InstallRequest.Ready(intent)
    }

    fun setDailyCheck(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY, enabled).apply()
        _dailyCheck.value = enabled
        val wm = WorkManager.getInstance(context)
        if (enabled) {
            val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(1, TimeUnit.DAYS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .build()
            wm.enqueueUniquePeriodicWork(DAILY_WORK, ExistingPeriodicWorkPolicy.UPDATE, request)
        } else {
            wm.cancelUniqueWork(DAILY_WORK)
        }
    }

    private fun errorText(error: UpdateError): UiText = UiText.Res(
        when (error) {
            UpdateError.NETWORK -> R.string.update_err_network
            UpdateError.TIMEOUT -> R.string.update_err_timeout
            UpdateError.BAD_RESPONSE -> R.string.update_err_bad_response
            UpdateError.RATE_LIMITED -> R.string.update_err_rate_limited
            UpdateError.WRONG_HOST -> R.string.update_err_wrong_host
            UpdateError.NO_APK -> R.string.update_err_no_apk
            UpdateError.NO_CHECKSUM -> R.string.update_err_no_checksum
            UpdateError.CHECKSUM_MISMATCH -> R.string.update_err_checksum
            UpdateError.TOO_LARGE -> R.string.update_err_too_large
            UpdateError.DOWNLOAD_INTERRUPTED -> R.string.update_err_interrupted
            UpdateError.STORAGE -> R.string.update_err_storage
            UpdateError.NOT_AVAILABLE -> R.string.update_err_not_available
        }
    )

    private fun remember(release: ReleaseInfo?) {
        val editor = prefs.edit()
        if (release == null) {
            editor.remove(KEY_RELEASE)
        } else {
            editor.putString(
                KEY_RELEASE,
                MiniJson.write(
                    linkedMapOf(
                        "tag" to release.tag, "changelog" to release.changelog, "apkName" to release.apkName,
                        "apkUrl" to release.apkUrl, "apkSize" to release.apkSizeBytes, "sumsUrl" to release.checksumsUrl
                    )
                )
            )
        }
        editor.apply()
    }

    /** Restores the last found release so an Available state survives a restart. Re-validated before use. */
    private fun recall(): ReleaseInfo? = runCatching {
        val map = MiniJson.parse(prefs.getString(KEY_RELEASE, null) ?: return null) as? Map<*, *> ?: return null
        val tag = map["tag"] as? String ?: return null
        val apkUrl = map["apkUrl"] as? String ?: return null
        val sumsUrl = map["sumsUrl"] as? String ?: return null
        if (!UrlPolicy.isAllowedAssetEntry(apkUrl) || !UrlPolicy.isAllowedAssetEntry(sumsUrl)) return null
        ReleaseInfo(
            version = SemVer.parse(tag) ?: return null,
            tag = tag,
            changelog = map["changelog"] as? String ?: "",
            apkName = map["apkName"] as? String ?: return null,
            apkUrl = apkUrl,
            apkSizeBytes = map["apkSize"] as? Long ?: -1L,
            checksumsUrl = sumsUrl
        )
    }.getOrNull()

    companion object {
        private const val PREFS = "dualclone_update"
        private const val KEY_DAILY = "daily_check"
        private const val KEY_RELEASE = "last_release"
        private const val DAILY_WORK = "update_daily_check"
        private const val UPDATES_DIR = "updates"
        private const val APK_MIME = "application/vnd.android.package-archive"

        /** FileProvider authority declared in the manifest. */
        fun authority(context: Context): String = "${context.packageName}.updates"
    }
}

/** Daily check. Only looks for a newer release and stores the answer, it never downloads. */
@HiltWorker
class UpdateCheckWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted params: WorkerParameters,
    private val repository: UpdateRepository
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        repository.check()
        Timber.tag("UpdateCheck").d("Daily check finished with state %s", repository.state.value)
        return Result.success()
    }
}
