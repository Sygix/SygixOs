/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

data class HttpTimeouts(val connectMs: Int, val readMs: Int, val totalMs: Long)

data class HttpResponse(
    val status: Int,
    val headers: Map<String, String>,
    val finalUrl: String,
    val bodyBytes: Long,
    val exceeded: Boolean,
)

interface HttpTransport {
    @Throws(IOException::class)
    fun get(
        url: String,
        headers: Map<String, String>,
        maxBytes: Long,
        timeouts: HttpTimeouts,
        sink: OutputStream,
        onBytes: (Long) -> Unit = {},
    ): HttpResponse
}

class HttpsUrlTransport : HttpTransport {

    override fun get(
        url: String,
        headers: Map<String, String>,
        maxBytes: Long,
        timeouts: HttpTimeouts,
        sink: OutputStream,
        onBytes: (Long) -> Unit,
    ): HttpResponse {
        val deadline = System.nanoTime() + timeouts.totalMs * 1_000_000
        val connection = URL(url).openConnection() as HttpURLConnection
        try {
            connection.instanceFollowRedirects = true
            connection.useCaches = false
            connection.connectTimeout = timeouts.connectMs
            connection.readTimeout = timeouts.readMs
            connection.requestMethod = "GET"
            headers.forEach { (name, value) -> connection.setRequestProperty(name, value) }
            val status = connection.responseCode
            val useful = UsefulHeaders.mapNotNull { name -> connection.getHeaderField(name)?.let { name to it } }.toMap()
            val finalUrl = connection.url.toString()
            if (status !in 200..299) {
                return HttpResponse(status, useful, finalUrl, bodyBytes = 0, exceeded = false)
            }
            val (written, exceeded) = connection.inputStream.use { input -> copy(input, sink, maxBytes, deadline, onBytes) }
            return HttpResponse(status, useful, finalUrl, written, exceeded)
        } finally {
            connection.disconnect()
        }
    }

    private fun copy(input: InputStream, sink: OutputStream, maxBytes: Long, deadline: Long, onBytes: (Long) -> Unit): Pair<Long, Boolean> {
        val buffer = ByteArray(BUFFER_SIZE)
        var total = 0L
        while (true) {
            if (System.nanoTime() > deadline) throw SocketTimeoutException("total time exceeded")
            val room = maxBytes - total
            if (room <= 0) return total to (input.read() != -1)
            val read = input.read(buffer, 0, minOf(buffer.size.toLong(), room).toInt())
            if (read == -1) return total to false
            sink.write(buffer, 0, read)
            total += read
            onBytes(total)
        }
    }

    private companion object {
        const val BUFFER_SIZE = 64 * 1024
        val UsefulHeaders = listOf("x-ratelimit-remaining", "x-ratelimit-reset", "retry-after")
    }
}
