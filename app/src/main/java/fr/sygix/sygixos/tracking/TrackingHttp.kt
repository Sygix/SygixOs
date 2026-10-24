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

class TrackingHttpsTransport(private val client: Call.Factory = OkHttpClient.Builder().connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS).callTimeout(15, TimeUnit.SECONDS)
        .followRedirects(false).followSslRedirects(false).build(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : TrackingTransport {

    override suspend fun execute(request: TrackingRequest): TrackingResponse = withContext(dispatcher) {
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
                                    ?: runCatching { (java.time.ZonedDateTime.parse(header, java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME).toInstant().toEpochMilli() - clock()).coerceAtLeast(0) }.getOrNull()
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

class BoundedTrackingTransport(private val clock: () -> Long, private val delegate: TrackingTransport) : TrackingTransport {
    constructor(delegate: TrackingTransport) : this({ System.nanoTime() / 1_000_000 }, delegate)
    override suspend fun execute(request: TrackingRequest): TrackingResponse = withTimeoutOrNull(if (request.method == "GET") 60_000 else 15_000) {
        val deadline = clock() + if (request.method == "GET") 60_000 else 15_000
        suspend fun waitForRetry(millis: Long) {
            if (millis > deadline - clock()) throw TrackingException(TrackingFailure.NETWORK)
            delay(millis.coerceAtLeast(0))
        }
        var last: TrackingResponse? = null
        repeat(if (request.method == "GET") 3 else 1) { attempt ->
            val response = try {
                withTimeoutOrNull(minOf(15_000, (deadline - clock()).coerceAtLeast(1))) { delegate.execute(request) } ?: throw TrackingException(TrackingFailure.NETWORK)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (request.method != "GET" || error is TrackingException && error.failure != TrackingFailure.NETWORK || attempt == 2) throw error
                waitForRetry(500L shl attempt)
                return@repeat
            }
            last = response
            val nonJson = request.headers["Accept"] == "application/json" && !response.body.trimStart().startsWith("{") && response.status !in setOf(401, 403)
            if (request.method != "GET" || response.status !in setOf(429, 502, 503, 504) && !nonJson) return@withTimeoutOrNull response
            if (attempt < 2) waitForRetry(response.retryAfterMillis ?: (500L shl attempt))
        }
        last ?: throw TrackingException(TrackingFailure.NETWORK)
    } ?: throw TrackingException(TrackingFailure.NETWORK)
}

internal fun officialTrackingUrl(value: String): Boolean = runCatching {
    val uri = java.net.URI(value)
    uri.scheme == "https" && uri.host in setOf("www.betaseries.com", "betaseries.com", "api.betaseries.com") && uri.userInfo == null && uri.port in setOf(-1, 443)
}.getOrDefault(false)
