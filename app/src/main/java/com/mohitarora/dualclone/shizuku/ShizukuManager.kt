package com.mohitarora.dualclone.shizuku

import android.content.pm.PackageManager
import com.mohitarora.dualclone.data.model.ShizukuState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import timber.log.Timber
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject
import javax.inject.Singleton

/** Live facts about the privileged backend, shown in Developer Diagnostics. Null means the value could not be read. */
data class ShizukuDiagnostics(
    val uid: Int? = null,
    val selinuxContext: String? = null,
    val apiVersion: Int? = null,
    val processHelperOk: Boolean = true
)

@Singleton
class ShizukuManager @Inject constructor() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private val _shizukuState = MutableStateFlow<ShizukuState>(ShizukuState.Checking)
    val shizukuState: StateFlow<ShizukuState> = _shizukuState.asStateFlow()

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        Timber.tag("ShizukuManager").d("Binder received")
        refreshStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        Timber.tag("ShizukuManager").w("Binder dead")
        _shizukuState.value = ShizukuState.Unavailable
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            Timber.tag("ShizukuManager").d("Permission result code=$requestCode grantResult=$grantResult")
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                refreshStatus()
            } else {
                _shizukuState.value = ShizukuState.PermissionRequired
            }
        }

    init {
        Shizuku.addBinderReceivedListenerSticky(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)
        Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
        refreshStatus()
    }

    fun refreshStatus() {
        scope.launch {
            _shizukuState.value = checkShizukuInternal()
        }
    }

    fun requestPermission(requestCode: Int = 1001) {
        try {
            if (Shizuku.getVersion() < 11) {
                Timber.tag("ShizukuManager").w("Pre-v11 Shizuku not supported")
                return
            }
            if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(requestCode)
            } else {
                refreshStatus()
            }
        } catch (t: Throwable) {
            Timber.tag("ShizukuManager").e(t, "Error requesting Shizuku permission")
        }
    }

    private suspend fun checkShizukuInternal(): ShizukuState = withContext(Dispatchers.IO) {
        try {
            var binderReady = try {
                Shizuku.pingBinder()
            } catch (t: Throwable) {
                false
            }

            if (!binderReady) {
                for (i in 1..8) {
                    kotlinx.coroutines.delay(150)
                    binderReady = try {
                        Shizuku.pingBinder()
                    } catch (t: Throwable) {
                        false
                    }
                    if (binderReady) break
                }
            }

            if (!binderReady) {
                return@withContext ShizukuState.Unavailable
            }

            val hasPermission = try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (t: Throwable) {
                Timber.tag("ShizukuManager").w(t, "checkSelfPermission failed")
                false
            }

            if (!hasPermission) {
                return@withContext ShizukuState.PermissionRequired
            }

            ShizukuState.Ready
        } catch (t: Throwable) {
            Timber.tag("ShizukuManager").e(t, "Shizuku status check failed")
            ShizukuState.Unavailable
        }
    }

    /** Reads the real UID and SELinux context of the Shizuku service, for example 2000 (shell) or 0 (root or Sui). */
    fun diagnostics(): ShizukuDiagnostics = ShizukuDiagnostics(
        uid = runCatching { Shizuku.getUid() }.getOrNull(),
        selinuxContext = runCatching { Shizuku.getSELinuxContext() }.getOrNull(),
        apiVersion = runCatching { Shizuku.getVersion() }.getOrNull(),
        processHelperOk = ShizukuProcessHelper.selfTest().isSuccess
    )
}
