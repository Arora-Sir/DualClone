package com.mohitarora.dualclone.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.LauncherApps
import android.os.UserHandle
import com.mohitarora.dualclone.data.repository.Reconciler
import com.mohitarora.dualclone.domain.repo.CatalogRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Listens for package changes for as long as the process lives.
 *
 * Android 8 and later do not deliver implicit PACKAGE_* broadcasts to manifest receivers, so the receiver
 * is registered at runtime. Changes inside managed profiles arrive through LauncherApps.Callback.
 * Bursts are debounced into one reconcile.
 */
@Singleton
class PackageEventMonitor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val catalog: CatalogRepository,
    private val reconciler: Reconciler
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val started = AtomicBoolean(false)
    private val _reconcileRequests = MutableSharedFlow<Unit>(extraBufferCapacity = 8)

    /** Emits after the debounced reconcile finished, so screens can refresh. */
    private val _completed = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val completed: SharedFlow<Unit> = _completed.asSharedFlow()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        registerPackageReceiver()
        registerLauncherCallback()
        scope.launch {
            @OptIn(kotlinx.coroutines.FlowPreview::class)
            _reconcileRequests.debounce(DEBOUNCE_MILLIS).collect {
                runCatching { reconciler.run() }.onFailure { Timber.tag("PackageEvents").w(it, "Reconcile failed") }
                _completed.tryEmit(Unit)
            }
        }
    }

    private fun registerPackageReceiver() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                val packageName = intent.data?.schemeSpecificPart ?: return
                val pending = goAsync()
                scope.launch {
                    try {
                        handle(intent.action, packageName, intent.getBooleanExtra(Intent.EXTRA_REPLACING, false))
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
        // NOTE: Package broadcasts are system broadcasts, so the receiver does not need to be exported.
        context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
    }

    private suspend fun handle(action: String?, packageName: String, replacing: Boolean) {
        Timber.tag("PackageEvents").d("action=%s package=%s", action, packageName)
        when (action) {
            Intent.ACTION_PACKAGE_ADDED, Intent.ACTION_PACKAGE_REPLACED -> catalog.onPackageAdded(packageName)
            Intent.ACTION_PACKAGE_REMOVED -> if (!replacing) catalog.onPackageRemoved(packageName)
        }
        _reconcileRequests.tryEmit(Unit)
    }

    private fun registerLauncherCallback() {
        try {
            val launcherApps = context.getSystemService(LauncherApps::class.java) ?: return
            launcherApps.registerCallback(object : LauncherApps.Callback() {
                override fun onPackageAdded(packageName: String, user: UserHandle) = changed(packageName, user)
                override fun onPackageRemoved(packageName: String, user: UserHandle) = changed(packageName, user)
                override fun onPackageChanged(packageName: String, user: UserHandle) = changed(packageName, user)
                override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                    changed(packageNames.firstOrNull().orEmpty(), user)
                override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
                    changed(packageNames.firstOrNull().orEmpty(), user)
            })
        } catch (t: Throwable) {
            Timber.tag("PackageEvents").w(t, "LauncherApps callback unavailable")
        }
    }

    private fun changed(packageName: String, user: UserHandle) {
        Timber.tag("PackageEvents").d("profile change package=%s user=%s", packageName, user)
        _reconcileRequests.tryEmit(Unit)
    }

    private companion object {
        const val DEBOUNCE_MILLIS = 1_500L
    }
}
