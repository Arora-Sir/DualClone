package com.mohitarora.dualclone.update

import java.io.Closeable
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/** One HTTP response. The caller owns the body stream and must close the response. */
class HttpResponse(
    val code: Int,
    val location: String?,
    /** Bytes the server says it will send for this response, or -1 when unknown. */
    val contentLength: Long,
    val body: InputStream,
    private val onClose: () -> Unit = {}
) : Closeable {
    override fun close() {
        try {
            body.close()
        } finally {
            onClose()
        }
    }
}

/** Raw HTTP access. Redirects are never followed here, so the caller can check every hop. */
interface HttpTransport {
    @Throws(IOException::class)
    fun open(url: String, rangeStart: Long = 0L): HttpResponse
}

/** Plain HttpURLConnection implementation, no third party HTTP library. */
@Singleton
class UrlConnectionTransport @Inject constructor() : HttpTransport {

    override fun open(url: String, rangeStart: Long): HttpResponse {
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = false
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.useCaches = false
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "DualClone-Updater")
            connection.setRequestProperty("Accept", "application/vnd.github+json, application/octet-stream, text/plain")
            connection.setRequestProperty("Accept-Encoding", "identity")
            if (rangeStart > 0) connection.setRequestProperty("Range", "bytes=$rangeStart-")
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else (connection.errorStream ?: emptyStream())
            return HttpResponse(
                code = code,
                location = connection.getHeaderField("Location"),
                contentLength = connection.getHeaderField("Content-Length")?.toLongOrNull() ?: -1L,
                body = stream,
                onClose = { connection.disconnect() }
            )
        } catch (t: Throwable) {
            connection.disconnect()
            throw t
        }
    }

    private fun emptyStream(): InputStream = java.io.ByteArrayInputStream(ByteArray(0))

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}
