package com.mohitarora.dualclone.update

import com.mohitarora.dualclone.domain.backup.MiniJson
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.net.SocketTimeoutException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Talks to GitHub Releases through an HttpTransport. Blocking, so callers run it off the main thread.
 * Every hop of every request is checked against UrlPolicy, and size limits apply to every body.
 */
@Singleton
class UpdateClient @Inject constructor(private val transport: HttpTransport) {

    /** Reads the latest stable release and compares it with the running version. */
    fun checkLatest(currentVersion: String): CheckResult {
        val current = SemVer.parse(currentVersion) ?: throw UpdateException(UpdateError.BAD_RESPONSE, "Unreadable app version")
        val root = try {
            MiniJson.parse(String(getBytes(UrlPolicy.LATEST_RELEASE_URL, MAX_JSON_BYTES), Charsets.UTF_8)) as? Map<*, *>
        } catch (e: MiniJson.JsonException) {
            null
        } ?: throw UpdateException(UpdateError.BAD_RESPONSE, "Release answer is not valid JSON")

        // NOTE: The latest endpoint already hides drafts and prereleases. The flags and tag are checked again so a mistake there never offers one.
        if (root["draft"] == true || root["prerelease"] == true) return CheckResult.UpToDate
        val tag = root["tag_name"] as? String ?: throw UpdateException(UpdateError.BAD_RESPONSE, "Missing tag_name")
        val version = SemVer.parse(tag) ?: throw UpdateException(UpdateError.BAD_RESPONSE, "Unreadable tag $tag")
        if (version.isPrerelease) return CheckResult.UpToDate
        if (version <= current) return CheckResult.UpToDate

        val assets = (root["assets"] as? List<*>).orEmpty().mapNotNull { it as? Map<*, *> }
        val apkName = "DualClone-v$version.apk"
        val apk = assets.firstOrNull { it["name"] == apkName } ?: throw UpdateException(UpdateError.NO_APK, "No asset named $apkName")
        val sums = assets.firstOrNull { it["name"] == CHECKSUMS_NAME } ?: throw UpdateException(UpdateError.NO_CHECKSUM, "No $CHECKSUMS_NAME asset")
        val apkUrl = apk["browser_download_url"] as? String ?: throw UpdateException(UpdateError.BAD_RESPONSE, "APK asset has no URL")
        val sumsUrl = sums["browser_download_url"] as? String ?: throw UpdateException(UpdateError.BAD_RESPONSE, "Checksum asset has no URL")
        if (!UrlPolicy.isAllowedAssetEntry(apkUrl) || !UrlPolicy.isAllowedAssetEntry(sumsUrl)) {
            throw UpdateException(UpdateError.WRONG_HOST, "Asset URL is outside the allowed hosts")
        }
        val size = (apk["size"] as? Long) ?: -1L
        if (size > MAX_APK_BYTES) throw UpdateException(UpdateError.TOO_LARGE, "APK is $size bytes")

        return CheckResult.Newer(
            ReleaseInfo(
                version = version,
                tag = tag,
                changelog = (root["body"] as? String).orEmpty().trim().take(MAX_CHANGELOG_CHARS),
                apkName = apkName,
                apkUrl = apkUrl,
                apkSizeBytes = size,
                checksumsUrl = sumsUrl
            )
        )
    }

    /** Returns the expected SHA-256 of the release APK from SHA256SUMS.txt, as lowercase hex. */
    fun fetchExpectedSha256(release: ReleaseInfo): String {
        val text = String(getBytes(release.checksumsUrl, MAX_SUMS_BYTES), Charsets.UTF_8)
        return Checksums.find(text, release.apkName) ?: throw UpdateException(UpdateError.NO_CHECKSUM, "No checksum line for ${release.apkName}")
    }

    /**
     * Downloads the APK to [dest]. A partial file is resumed with a Range request when the server supports it,
     * otherwise the download restarts. Progress is reported as bytes done and total (or -1).
     */
    fun downloadApk(release: ReleaseInfo, dest: File, onProgress: (done: Long, total: Long) -> Unit) {
        var offset = if (dest.exists()) dest.length() else 0L
        if (release.apkSizeBytes > 0 && offset > release.apkSizeBytes) {
            dest.delete()
            offset = 0L
        }
        if (release.apkSizeBytes > 0 && offset == release.apkSizeBytes) {
            onProgress(offset, release.apkSizeBytes)
            return
        }
        open(release.apkUrl, offset).use { resp ->
            val append: Boolean
            when (resp.code) {
                206 -> append = true
                200 -> {
                    append = false
                    offset = 0L
                }
                416 -> {
                    dest.delete()
                    throw UpdateException(UpdateError.DOWNLOAD_INTERRUPTED, "Server rejected the resume range")
                }
                else -> throw httpError(resp.code)
            }
            val total = when {
                resp.contentLength >= 0 -> offset + resp.contentLength
                release.apkSizeBytes > 0 -> release.apkSizeBytes
                else -> -1L
            }
            if (total > MAX_APK_BYTES) throw UpdateException(UpdateError.TOO_LARGE, "APK is $total bytes")
            dest.parentFile?.mkdirs()
            try {
                FileOutputStream(dest, append).use { out ->
                    var done = offset
                    val buffer = ByteArray(BUFFER_BYTES)
                    onProgress(done, total)
                    while (true) {
                        if (Thread.currentThread().isInterrupted) throw UpdateException(UpdateError.DOWNLOAD_INTERRUPTED, "Cancelled")
                        val n = resp.body.read(buffer)
                        if (n < 0) break
                        done += n
                        if (done > MAX_APK_BYTES) throw UpdateException(UpdateError.TOO_LARGE, "APK exceeds the size limit")
                        out.write(buffer, 0, n)
                        onProgress(done, total)
                    }
                }
            } catch (e: UpdateException) {
                throw e
            } catch (e: SocketTimeoutException) {
                throw UpdateException(UpdateError.TIMEOUT, "Timed out during download", e)
            } catch (e: IOException) {
                throw UpdateException(UpdateError.DOWNLOAD_INTERRUPTED, "Download interrupted", e)
            }
        }
        if (release.apkSizeBytes > 0 && dest.length() != release.apkSizeBytes) {
            throw UpdateException(UpdateError.DOWNLOAD_INTERRUPTED, "Received ${dest.length()} of ${release.apkSizeBytes} bytes")
        }
    }

    private fun getBytes(url: String, maxBytes: Int): ByteArray = open(url, 0L).use { resp ->
        if (resp.code != 200) throw httpError(resp.code)
        if (resp.contentLength > maxBytes) throw UpdateException(UpdateError.TOO_LARGE, "Body is ${resp.contentLength} bytes")
        try {
            readBounded(resp.body, maxBytes)
        } catch (e: SocketTimeoutException) {
            throw UpdateException(UpdateError.TIMEOUT, "Timed out reading the response", e)
        } catch (e: IOException) {
            throw UpdateException(UpdateError.NETWORK, "Read failed", e)
        }
    }

    private fun readBounded(input: InputStream, maxBytes: Int): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8 * 1024)
        while (true) {
            val n = input.read(buffer)
            if (n < 0) break
            out.write(buffer, 0, n)
            if (out.size() > maxBytes) throw UpdateException(UpdateError.TOO_LARGE, "Body exceeds $maxBytes bytes")
        }
        return out.toByteArray()
    }

    /** Opens a URL and follows redirects by hand so every hop passes UrlPolicy. */
    private fun open(startUrl: String, rangeStart: Long): HttpResponse {
        var url = startUrl
        repeat(MAX_REDIRECTS + 1) {
            if (!UrlPolicy.isAllowedHop(url)) throw UpdateException(UpdateError.WRONG_HOST, "Blocked host in $url")
            val resp = try {
                transport.open(url, rangeStart)
            } catch (e: SocketTimeoutException) {
                throw UpdateException(UpdateError.TIMEOUT, "Timed out", e)
            } catch (e: IOException) {
                throw UpdateException(UpdateError.NETWORK, "Connection failed", e)
            }
            if (resp.code in REDIRECT_CODES) {
                val next = resp.location?.let { UrlPolicy.resolve(url, it) }
                resp.close()
                url = next ?: throw UpdateException(UpdateError.BAD_RESPONSE, "Redirect without a usable Location")
            } else {
                return resp
            }
        }
        throw UpdateException(UpdateError.BAD_RESPONSE, "Too many redirects")
    }

    private fun httpError(code: Int) = when (code) {
        403, 429 -> UpdateException(UpdateError.RATE_LIMITED, "HTTP $code")
        else -> UpdateException(UpdateError.BAD_RESPONSE, "HTTP $code")
    }

    companion object {
        const val CHECKSUMS_NAME = "SHA256SUMS.txt"
        const val MAX_JSON_BYTES = 1024 * 1024
        const val MAX_SUMS_BYTES = 64 * 1024
        const val MAX_APK_BYTES = 200L * 1024 * 1024
        private const val MAX_CHANGELOG_CHARS = 4_000
        private const val MAX_REDIRECTS = 5
        private const val BUFFER_BYTES = 64 * 1024
        private val REDIRECT_CODES = setOf(301, 302, 303, 307, 308)

        fun sha256(file: File): String {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            return digest.digest().joinToString("") { "%02x".format(it) }
        }
    }
}

/** Parser for the `sha256sum` output format: 64 hex digits, whitespace, optional `*`, file name. */
object Checksums {
    private val LINE = Regex("""^([0-9a-fA-F]{64})\s+\*?(.+?)\s*$""")

    fun find(text: String, fileName: String): String? = text.lineSequence()
        .mapNotNull { LINE.matchEntire(it.trim()) }
        .firstOrNull { it.groupValues[2].substringAfterLast('/') == fileName }
        ?.groupValues?.get(1)?.lowercase()
}
