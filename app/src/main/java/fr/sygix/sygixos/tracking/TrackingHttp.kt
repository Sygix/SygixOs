/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class TrackingRequest(
    val url: String,
    val method: String = "GET",
    val headers: Map<String, String> = emptyMap(),
    val fields: Map<String, String> = emptyMap(),
    val maxBytes: Int = 2 * 1024 * 1024,
)

data class TrackingResponse(val status: Int, val body: String, val retryAfterMillis: Long? = null)

fun interface TrackingTransport {
    suspend fun execute(request: TrackingRequest): TrackingResponse
}

class TrackingHttpsTransport : TrackingTransport {
    private val client = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build()

    override suspend fun execute(request: TrackingRequest): TrackingResponse = withContext(Dispatchers.IO) {
        if (!officialTrackingUrl(request.url)) throw TrackingException(TrackingFailure.CONFIGURATION)
        val builder = Request.Builder().url(request.url)
        request.headers.forEach { (name, value) -> builder.header(name, value) }
        val body = FormBody.Builder().apply { request.fields.forEach { (key, value) -> add(key, value) } }.build()
        builder.method(request.method, body.takeUnless { request.method == "GET" })
        val call = client.newCall(builder.build())
        suspendCancellableCoroutine { continuation ->
            continuation.invokeOnCancellation { call.cancel() }
            call.enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) { if (continuation.isActive) continuation.resumeWithException(TrackingException(TrackingFailure.NETWORK)) }
                override fun onResponse(call: Call, response: Response) {
                    try {
                        val result = response.use {
                            val bytes = it.body?.byteStream()?.use { stream -> stream.readNBytes(request.maxBytes + 1) } ?: byteArrayOf()
                            if (bytes.size > request.maxBytes) throw TrackingException(TrackingFailure.PROTOCOL)
                            val retryAfter = it.header("Retry-After")?.let { header ->
                                header.toLongOrNull()?.takeIf { seconds -> seconds in 0..Long.MAX_VALUE / 1000 }?.times(1000)
                                    ?: runCatching { (java.time.ZonedDateTime.parse(header, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - System.currentTimeMillis()).coerceAtLeast(0) }.getOrNull()
                            }
                            TrackingResponse(it.code, bytes.toString(Charsets.UTF_8), retryAfter)
                        }
                        if (continuation.isActive) continuation.resume(result)
                    } catch (_: Exception) {
                        if (continuation.isActive) continuation.resumeWithException(TrackingException(TrackingFailure.PROTOCOL))
                    }
                }
            })
        }
    }
}

class BoundedTrackingTransport(private val delegate: TrackingTransport) : TrackingTransport {
    override suspend fun execute(request: TrackingRequest): TrackingResponse = withTimeoutOrNull(15_000) {
        var last: TrackingResponse? = null
        repeat(if (request.method == "GET") 3 else 1) { attempt ->
            try {
                val response = delegate.execute(request)
                last = response
                if (request.method != "GET" || response.status !in setOf(429, 502, 503, 504)) return@withTimeoutOrNull response
                if (attempt < 2) delay(response.retryAfterMillis ?: (500L shl attempt))
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request.method != "GET" || error is TrackingException && error.failure != TrackingFailure.NETWORK || attempt == 2) throw error
                delay(500L shl attempt)
            }
        }
        last ?: throw TrackingException(TrackingFailure.NETWORK)
    } ?: throw TrackingException(TrackingFailure.NETWORK)
}

internal fun officialTrackingUrl(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme == "https" && uri.host in setOf("www.betaseries.com", "betaseries.com", "api.betaseries.com") && uri.userInfo == null && uri.port in setOf(-1, 443)
}.getOrDefault(false)
