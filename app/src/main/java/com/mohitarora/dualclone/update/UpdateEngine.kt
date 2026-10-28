package com.mohitarora.dualclone.update

import com.mohitarora.dualclone.core.common.UiText
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import java.io.File

/**
 * The update state machine without Android types. Checking and downloading run only when called,
 * which the caller does on an explicit user action (or the optional daily check, which never downloads).
 */
class UpdateEngine(
    private val client: UpdateClient,
    private val updatesDir: File,
    private val currentVersion: String,
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val toText: (UpdateError) -> UiText = { UiText.Plain(it.name) },
    /** Called when a newer release was found or cleared, so it can be remembered across restarts. */
    private val onReleaseChanged: (ReleaseInfo?) -> Unit = {},
    initialRelease: ReleaseInfo? = null
) {
    private val busy = Mutex()
    private var release: ReleaseInfo? = initialRelease?.takeIf { newer(it) }
    private var verifiedFile: File? = null
    private var verifiedSha: String? = null

    private val _state = MutableStateFlow<UpdateState>(release?.let(::availableOf) ?: UpdateState.Idle)
    val state: StateFlow<UpdateState> = _state.asStateFlow()

    init {
        cleanupStale(keep = release?.apkName)
    }

    private fun newer(r: ReleaseInfo): Boolean {
        val current = SemVer.parse(currentVersion) ?: return false
        return r.version > current
    }

    private fun availableOf(r: ReleaseInfo) = UpdateState.Available(r.version.toString(), r.changelog, r.apkSizeBytes)

    /** Asks GitHub for the latest release. Ignored while a download is running. */
    suspend fun check() {
        if (!busy.tryLock()) return
        try {
            if (_state.value is UpdateState.Downloading) return
            _state.value = UpdateState.Checking
            _state.value = try {
                when (val r = withContext(io) { client.checkLatest(currentVersion) }) {
                    CheckResult.UpToDate -> {
                        release = null
                        onReleaseChanged(null)
                        cleanupStale(keep = null)
                        UpdateState.UpToDate
                    }
                    is CheckResult.Newer -> {
                        release = r.release
                        onReleaseChanged(r.release)
                        cleanupStale(keep = r.release.apkName)
                        availableOf(r.release)
                    }
                }
            } catch (e: UpdateException) {
                failed(e.error)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                failed(UpdateError.NETWORK)
            }
        } finally {
            busy.unlock()
        }
    }

    /** Downloads the available release and verifies its SHA-256. Needs a prior successful check. */
    suspend fun download() {
        if (!busy.tryLock()) return
        try {
            val r = release
            if (r == null || _state.value !is UpdateState.Available && _state.value !is UpdateState.Failed) {
                if (r == null) _state.value = failed(UpdateError.NOT_AVAILABLE)
                return
            }
            _state.value = UpdateState.Downloading(0f)
            verifiedFile = null
            verifiedSha = null
            _state.value = try {
                withContext(io) {
                    cleanupStale(keep = r.apkName)
                    val expected = client.fetchExpectedSha256(r)
                    val part = File(updatesDir, r.apkName + ".part")
                    var lastPercent = -1
                    client.downloadApk(r, part) { done, total ->
                        val percent = if (total > 0) (done * 100 / total).toInt() else -1
                        if (percent != lastPercent) {
                            lastPercent = percent
                            _state.value = UpdateState.Downloading(if (total > 0) done.toFloat() / total else null)
                        }
                    }
                    val actual = UpdateClient.sha256(part)
                    if (actual != expected) {
                        part.delete()
                        throw UpdateException(UpdateError.CHECKSUM_MISMATCH, "Expected $expected but got $actual")
                    }
                    val apk = File(updatesDir, r.apkName)
                    apk.delete()
                    if (!part.renameTo(apk)) throw UpdateException(UpdateError.STORAGE, "Could not finalize the download")
                    verifiedFile = apk
                    verifiedSha = expected
                }
                UpdateState.Downloaded(r.version.toString())
            } catch (e: UpdateException) {
                failed(e.error)
            } catch (e: CancellationException) {
                throw e
            } catch (e: java.io.IOException) {
                failed(UpdateError.STORAGE)
            }
        } finally {
            busy.unlock()
        }
    }

    /**
     * Re-hashes the downloaded file and returns it only if it still matches the checksum from SHA256SUMS.txt.
     * Call this immediately before building the install intent.
     */
    suspend fun verifiedApk(): File? {
        val file = verifiedFile
        val expected = verifiedSha
        if (file == null || expected == null || _state.value !is UpdateState.Downloaded) return null
        val ok = withContext(io) { file.exists() && UpdateClient.sha256(file) == expected }
        if (!ok) {
            file.delete()
            verifiedFile = null
            verifiedSha = null
            _state.value = failed(UpdateError.CHECKSUM_MISMATCH)
            return null
        }
        return file
    }

    fun reset() {
        if (_state.value is UpdateState.Downloading || _state.value is UpdateState.Checking) return
        _state.value = release?.let(::availableOf) ?: UpdateState.Idle
    }

    private fun failed(error: UpdateError) = UpdateState.Failed(error, toText(error))

    /** Deletes every file in the updates folder except the current target and its partial download. */
    private fun cleanupStale(keep: String?) {
        updatesDir.listFiles()?.forEach { f ->
            if (keep == null || (f.name != keep && f.name != "$keep.part")) f.delete()
        }
    }
}
