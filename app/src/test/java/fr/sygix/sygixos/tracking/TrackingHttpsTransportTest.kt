/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okio.Buffer
import okio.ForwardingSource
import okio.Timeout
import okio.buffer
import org.junit.Assert.*
import org.junit.Test

class TrackingHttpsTransportTest {
    @Test fun jsonMutationsPreserveTheBodyAndPaginationResponseHeaders() = runTest {
        val payload = """{"episodes":[]}"""
        val transport = TrackingHttpsTransport(Call.Factory { request ->
            assertEquals("POST", request.method)
            assertEquals("application/json", request.body?.contentType()?.toString()?.substringBefore(';'))
            val buffer = Buffer()
            request.body!!.writeTo(buffer)
            assertEquals(payload, buffer.readUtf8())
            TestCall(request) { call, callback -> callback.onResponse(call,
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("Fixture")
                    .header("X-Pagination-Page-Count", "2").body("[]".toResponseBody("application/json".toMediaType())).build()) }
        }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val response = transport.execute(TrackingRequest("https://api.trakt.tv/sync/history", "POST", jsonBody = payload))
        assertEquals("2", response.headers.entries.single().value)
    }

    @Test fun successfulJsonArraysAreNotRetriedAsMalformedObjects() = runTest {
        var reads = 0
        val transport = BoundedTrackingTransport({ testScheduler.currentTime }, TrackingTransport {
            reads++
            TrackingResponse(200, "[]")
        })
        assertEquals("[]", transport.execute(TrackingRequest("https://api.trakt.tv/sync/watchlist", headers = mapOf("Accept" to "application/json"))).body)
        assertEquals(1, reads)
    }

    @Test fun oversizedResponseIsRejectedAndItsBodyIsClosed() = runTest {
        val closed = AtomicBoolean()
        val source = object : ForwardingSource(Buffer().write(ByteArray(64))) {
            override fun close() { closed.set(true); super.close() }
        }.buffer()
        val body = object : ResponseBody() {
            override fun contentType(): MediaType? = null
            override fun contentLength() = 64L
            override fun source() = source
        }
        val transport = TrackingHttpsTransport(Call.Factory { request ->
            TestCall(request) { call, callback -> callback.onResponse(call,
                Response.Builder().request(request).protocol(Protocol.HTTP_1_1).code(200).message("Fixture").body(body).build()) }
        }, clock = { 0 }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        try { transport.execute(TrackingRequest("https://api.betaseries.com/fixture", maxBytes = 32)); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.PROTOCOL, error.failure) }
        assertTrue(closed.get())
    }

    @Test fun cancellationCancelsTheUnderlyingCallWithoutWaitingForAResponse() = runTest {
        val started = CompletableDeferred<Call>()
        val transport = TrackingHttpsTransport(Call.Factory { request -> TestCall(request) { call, _ -> started.complete(call) } }, clock = { 0 }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val job = launch { transport.execute(TrackingRequest("https://api.betaseries.com/fixture")) }
        val call = started.await()
        job.cancelAndJoin()
        assertTrue(call.isCanceled())
    }

    @Test fun departureAuthorizationIsCheckedAtTheActualEnqueueBoundary() = runTest {
        var invalidated = false
        var enqueued = false
        val transport = TrackingHttpsTransport(Call.Factory { request ->
            invalidated = true
            TestCall(request) { _, _ -> enqueued = true }
        }, dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler))
        val authorization = TrackingRequestAuthorization({ true }, { dispatch ->
            if (invalidated) throw TrackingSessionChangedException()
            dispatch()
        })
        try {
            kotlinx.coroutines.withContext(authorization) { transport.execute(TrackingRequest("https://api.betaseries.com/fixture", "POST")) }
            fail()
        } catch (_: TrackingSessionChangedException) { }
        assertFalse(enqueued)
    }

    private class TestCall(private val value: Request, private val enqueue: (Call, Callback) -> Unit) : Call {
        private val cancelled = AtomicBoolean()
        override fun request() = value
        override fun execute(): Response = error("Only asynchronous fixture calls are supported")
        override fun enqueue(responseCallback: Callback) { enqueue(this, responseCallback) }
        override fun cancel() { cancelled.set(true) }
        override fun isExecuted() = true
        override fun isCanceled() = cancelled.get()
        override fun timeout(): Timeout = Timeout.NONE
        override fun clone(): Call = TestCall(value, enqueue)
    }
}
