package com.mohitarora.dualclone.update

import com.mohitarora.dualclone.core.common.UiText
import com.mohitarora.dualclone.domain.backup.MiniJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.file.Files
import java.security.MessageDigest

class UpdateTest {

    private class FakeTransport : HttpTransport {
        val routes = mutableMapOf<String, (Long) -> HttpResponse>()
        val requests = mutableListOf<Pair<String, Long>>()
        override fun open(url: String, rangeStart: Long): HttpResponse {
            requests.add(url to rangeStart)
            val route = routes[url] ?: throw IOException("no route for $url")
            return route(rangeStart)
        }
    }

    private val base = "https://github.com/Arora-Sir/DualClone/releases/download"
    private val apkBytes = ByteArray(300_000) { (it * 31).toByte() }
    private val apkSha = sha(apkBytes)
    private lateinit var dir: File
    private val transport = FakeTransport()

    @Before
    fun setUp() {
        dir = Files.createTempDirectory("updates").toFile()
    }

    @After
    fun tearDown() {
        dir.deleteRecursively()
    }

    private fun sha(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

    private fun resp(code: Int, body: ByteArray = ByteArray(0), location: String? = null, length: Long = body.size.toLong()) =
        HttpResponse(code, location, length, ByteArrayInputStream(body))

    private fun asset(name: String, url: String = "$base/v1.1.0/$name", size: Long = apkBytes.size.toLong()) =
        mapOf("name" to name, "browser_download_url" to url, "size" to size)

    private fun releaseJson(
        tag: String = "v1.1.0",
        prerelease: Boolean = false,
        assets: List<Map<String, Any>> = listOf(asset("DualClone-v1.1.0.apk"), asset("SHA256SUMS.txt", size = 100))
    ) = MiniJson.write(linkedMapOf("tag_name" to tag, "prerelease" to prerelease, "draft" to false, "body" to "Fixes and polish", "assets" to assets))

    private fun serveRelease(json: String) {
        transport.routes[UrlPolicy.LATEST_RELEASE_URL] = { resp(200, json.toByteArray()) }
    }

    private fun serveDownloads(sums: String = "$apkSha  DualClone-v1.1.0.apk\n", apk: (Long) -> HttpResponse = { resp(200, apkBytes) }) {
        transport.routes["$base/v1.1.0/SHA256SUMS.txt"] = { resp(200, sums.toByteArray()) }
        transport.routes["$base/v1.1.0/DualClone-v1.1.0.apk"] = apk
    }

    private fun engine(current: String = "1.0.0") =
        UpdateEngine(UpdateClient(transport), dir, current, Dispatchers.IO, toText = { UiText.Plain(it.name) })

    private fun UpdateEngine.failure(): UpdateError = (state.value as UpdateState.Failed).error

    // region check

    @Test
    fun newerReleaseBecomesAvailableWithChangelogAndSize() = runBlocking {
        serveRelease(releaseJson())
        val e = engine()
        e.check()
        assertEquals(UpdateState.Available("1.1.0", "Fixes and polish", apkBytes.size.toLong()), e.state.value)
    }

    @Test
    fun sameVersionIsUpToDate() = runBlocking {
        serveRelease(releaseJson(tag = "v1.0.0"))
        val e = engine()
        e.check()
        assertEquals(UpdateState.UpToDate, e.state.value)
    }

    @Test
    fun olderVersionIsUpToDate() = runBlocking {
        serveRelease(releaseJson(tag = "v0.9.0"))
        val e = engine()
        e.check()
        assertEquals(UpdateState.UpToDate, e.state.value)
    }

    @Test
    fun prereleaseIsIgnoredByFlagAndByTag() = runBlocking {
        serveRelease(releaseJson(prerelease = true))
        val flagged = engine().also { it.check() }
        assertEquals(UpdateState.UpToDate, flagged.state.value)

        serveRelease(releaseJson(tag = "v2.0.0-rc1"))
        val tagged = engine().also { it.check() }
        assertEquals(UpdateState.UpToDate, tagged.state.value)
    }

    @Test
    fun releaseWithoutChecksumAssetFails() = runBlocking {
        serveRelease(releaseJson(assets = listOf(asset("DualClone-v1.1.0.apk"))))
        val e = engine()
        e.check()
        assertEquals(UpdateError.NO_CHECKSUM, e.failure())
    }

    @Test
    fun releaseWithoutApkAssetFails() = runBlocking {
        serveRelease(releaseJson(assets = listOf(asset("SHA256SUMS.txt"), asset("other.zip"))))
        val e = engine()
        e.check()
        assertEquals(UpdateError.NO_APK, e.failure())
    }

    @Test
    fun malformedJsonFails() = runBlocking {
        serveRelease("{ not json")
        val e = engine()
        e.check()
        assertEquals(UpdateError.BAD_RESPONSE, e.failure())

        serveRelease("[1,2,3]")
        e.check()
        assertEquals(UpdateError.BAD_RESPONSE, e.failure())
    }

    @Test
    fun assetOnAnotherHostIsRejected() = runBlocking {
        serveRelease(releaseJson(assets = listOf(
            asset("DualClone-v1.1.0.apk", url = "https://evil.example.com/Arora-Sir/DualClone/releases/download/v1.1.0/DualClone-v1.1.0.apk"),
            asset("SHA256SUMS.txt")
        )))
        val e = engine()
        e.check()
        assertEquals(UpdateError.WRONG_HOST, e.failure())
    }

    @Test
    fun assetFromAnotherRepositoryOrPlainHttpIsRejected() = runBlocking {
        serveRelease(releaseJson(assets = listOf(
            asset("DualClone-v1.1.0.apk", url = "https://github.com/someone/else/releases/download/v1.1.0/DualClone-v1.1.0.apk"),
            asset("SHA256SUMS.txt")
        )))
        assertEquals(UpdateError.WRONG_HOST, engine().also { it.check() }.failure())

        serveRelease(releaseJson(assets = listOf(
            asset("DualClone-v1.1.0.apk", url = "http://github.com/Arora-Sir/DualClone/releases/download/v1.1.0/DualClone-v1.1.0.apk"),
            asset("SHA256SUMS.txt")
        )))
        assertEquals(UpdateError.WRONG_HOST, engine().also { it.check() }.failure())
    }

    @Test
    fun httpErrorsAndNetworkFailuresMapToErrors() = runBlocking {
        transport.routes[UrlPolicy.LATEST_RELEASE_URL] = { resp(403) }
        assertEquals(UpdateError.RATE_LIMITED, engine().also { it.check() }.failure())
        transport.routes[UrlPolicy.LATEST_RELEASE_URL] = { resp(500) }
        assertEquals(UpdateError.BAD_RESPONSE, engine().also { it.check() }.failure())
        transport.routes.clear()
        assertEquals(UpdateError.NETWORK, engine().also { it.check() }.failure())
    }

    @Test
    fun onlyTheGitHubApiIsContactedForTheCheck() = runBlocking {
        serveRelease(releaseJson())
        engine().check()
        assertEquals(listOf(UrlPolicy.LATEST_RELEASE_URL), transport.requests.map { it.first })
    }

    // endregion

    // region download

    private fun availableEngine(): UpdateEngine = runBlocking {
        serveRelease(releaseJson())
        engine().also { it.check() }
    }

    @Test
    fun downloadVerifiesChecksumAndExposesTheFile() = runBlocking {
        serveDownloads()
        val e = availableEngine()
        e.download()
        assertEquals(UpdateState.Downloaded("1.1.0"), e.state.value)
        val file = e.verifiedApk()
        assertNotNull(file)
        assertEquals(apkSha, sha(file!!.readBytes()))
        assertEquals("DualClone-v1.1.0.apk", file.name)
        assertFalse(File(dir, "DualClone-v1.1.0.apk.part").exists())
    }

    @Test
    fun badChecksumDeletesTheFileAndNeverExposesIt() = runBlocking {
        serveDownloads(sums = "${"0".repeat(64)}  DualClone-v1.1.0.apk\n")
        val e = availableEngine()
        e.download()
        assertEquals(UpdateError.CHECKSUM_MISMATCH, e.failure())
        assertNull(e.verifiedApk())
        assertTrue(dir.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun checksumFileWithoutALineForTheApkFails() = runBlocking {
        serveDownloads(sums = "$apkSha  something-else.apk\n")
        val e = availableEngine()
        e.download()
        assertEquals(UpdateError.NO_CHECKSUM, e.failure())
        assertEquals(0, transport.requests.count { it.first.endsWith(".apk") })
    }

    @Test
    fun tamperingAfterDownloadIsCaughtBeforeInstall() = runBlocking {
        serveDownloads()
        val e = availableEngine()
        e.download()
        File(dir, "DualClone-v1.1.0.apk").appendBytes(byteArrayOf(1))
        assertNull(e.verifiedApk())
        assertEquals(UpdateError.CHECKSUM_MISMATCH, e.failure())
    }

    @Test
    fun interruptedDownloadKeepsPartialFileAndResumesWithRange() = runBlocking {
        val cut = 100_000
        serveDownloads(apk = { start ->
            if (start == 0L) {
                val failing = object : InputStream() {
                    private var pos = 0
                    override fun read(): Int = if (pos < cut) apkBytes[pos++].toInt() and 0xff else throw IOException("connection reset")
                }
                HttpResponse(200, null, apkBytes.size.toLong(), failing)
            } else {
                HttpResponse(206, null, apkBytes.size - start, ByteArrayInputStream(apkBytes.copyOfRange(start.toInt(), apkBytes.size)))
            }
        })
        val e = availableEngine()
        e.download()
        assertEquals(UpdateError.DOWNLOAD_INTERRUPTED, e.failure())
        assertEquals(cut.toLong(), File(dir, "DualClone-v1.1.0.apk.part").length())

        e.download()
        assertEquals(UpdateState.Downloaded("1.1.0"), e.state.value)
        assertTrue(transport.requests.any { it.first.endsWith(".apk") && it.second == cut.toLong() })
        assertEquals(apkSha, sha(e.verifiedApk()!!.readBytes()))
    }

    @Test
    fun serverThatIgnoresRangeRestartsTheDownload() = runBlocking {
        serveDownloads()
        val e = availableEngine()
        // NOTE: A fresh engine would clean unknown files, so the partial file is written after the check as a prior interrupted run would leave it.
        File(dir, "DualClone-v1.1.0.apk.part").writeBytes(ByteArray(1_000) { 9 })
        e.download()
        assertEquals(UpdateState.Downloaded("1.1.0"), e.state.value)
    }

    @Test
    fun redirectToAnotherHostIsBlocked() = runBlocking {
        serveDownloads(apk = { resp(302, location = "https://evil.example.com/payload.apk") })
        val e = availableEngine()
        e.download()
        assertEquals(UpdateError.WRONG_HOST, e.failure())
    }

    @Test
    fun redirectToGitHubAssetHostIsFollowed() = runBlocking {
        val asset = "https://release-assets.githubusercontent.com/github-production-release-asset/abc/file"
        serveDownloads(apk = { resp(302, location = asset) })
        transport.routes[asset] = { resp(200, apkBytes) }
        val e = availableEngine()
        e.download()
        assertEquals(UpdateState.Downloaded("1.1.0"), e.state.value)
    }

    @Test
    fun oversizedDownloadIsRefused() = runBlocking {
        serveDownloads(apk = { resp(200, ByteArray(10), length = UpdateClient.MAX_APK_BYTES + 1) })
        val e = availableEngine()
        e.download()
        assertEquals(UpdateError.TOO_LARGE, e.failure())
    }

    @Test
    fun downloadWithoutACheckIsRefused() = runBlocking {
        val e = engine()
        e.download()
        assertEquals(UpdateError.NOT_AVAILABLE, e.failure())
        assertTrue(transport.requests.isEmpty())
    }

    @Test
    fun staleFilesAreCleanedWhenTheEngineStartsAndAfterACheck() = runBlocking {
        File(dir, "DualClone-v0.9.0.apk").writeBytes(ByteArray(10))
        File(dir, "junk.part").writeBytes(ByteArray(10))
        serveRelease(releaseJson(tag = "v1.0.0"))
        val e = engine()
        assertTrue("engine start removes files with no pending release", dir.listFiles().orEmpty().isEmpty())
        File(dir, "DualClone-v0.8.0.apk").writeBytes(ByteArray(10))
        e.check()
        assertTrue("an up to date check removes leftovers", dir.listFiles().orEmpty().isEmpty())
    }

    // endregion

    @Test
    fun semanticVersionsCompareCorrectly() {
        fun v(s: String) = SemVer.parse(s)!!
        assertTrue(v("1.10.0") > v("1.9.9"))
        assertTrue(v("v2.0.0") > v("1.99.99"))
        assertTrue(v("1.0.0") > v("1.0.0-rc1"))
        assertTrue(v("1.0.0-rc2") > v("1.0.0-rc1"))
        assertTrue(v("1.0.0-rc.10") > v("1.0.0-rc.9"))
        assertEquals(0, v("1.2.3").compareTo(v("v1.2.3+build5")))
        assertNull(SemVer.parse("1.2"))
        assertNull(SemVer.parse("latest"))
        assertTrue(v("1.0.0-beta").isPrerelease)
    }

    @Test
    fun checksumFileParsing() {
        val text = "${"A".repeat(64)}  DualClone-v1.1.0.apk\n${"b".repeat(64)} *other.zip\n"
        assertEquals("a".repeat(64), Checksums.find(text, "DualClone-v1.1.0.apk"))
        assertEquals("b".repeat(64), Checksums.find(text, "other.zip"))
        assertNull(Checksums.find(text, "missing.apk"))
        assertNull(Checksums.find("not a checksum line", "x"))
    }

    @Test
    fun urlPolicyAllowsOnlyTheKnownHosts() {
        assertTrue(UrlPolicy.isAllowedHop(UrlPolicy.LATEST_RELEASE_URL))
        assertTrue(UrlPolicy.isAllowedHop("https://objects.githubusercontent.com/x"))
        assertFalse(UrlPolicy.isAllowedHop("https://api.github.com.evil.com/x"))
        assertFalse(UrlPolicy.isAllowedHop("https://user:pw@github.com/x"))
        assertFalse(UrlPolicy.isAllowedHop("https://github.com:8443/x"))
        assertFalse(UrlPolicy.isAllowedHop("http://github.com/x"))
        assertFalse(UrlPolicy.isAllowedHop("ftp://github.com/x"))
        assertFalse(UrlPolicy.isAllowedHop("not a url"))
    }
}
