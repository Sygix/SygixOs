/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.SocketTimeoutException
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class HttpsUrlTransportTest {

    private lateinit var server: HttpServer
    private val requests = CopyOnWriteArrayList<Map<String, List<String>>>()
    private val release = CountDownLatch(1)
    private val transport = HttpsUrlTransport()
    private val headers = GitHubReleaseSource.apiHeaders("SygixOs/1.2.3")
    private val fast = HttpTimeouts(connectMs = 2_000, readMs = 2_000, totalMs = 10_000)

    @Before
    fun start() {
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.executor = Executors.newCachedThreadPool()
        server.createContext("/body") { reply(it, 200, BODY, "x-ratelimit-remaining" to "59", "retry-after" to "30", "Set-Cookie" to "id=1") }
        server.createContext("/missing") { reply(it, 404, "{}".toByteArray()) }
        server.createContext("/gone") { reply(it, 410, "{}".toByteArray()) }
        server.createContext("/limited") { reply(it, 403, "{}".toByteArray(), "x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "1700000000") }
        server.createContext("/redirect") { reply(it, 302, ByteArray(0), "Location" to "/body") }
        server.createContext("/slow") { exchange ->
            record(exchange)
            exchange.sendResponseHeaders(200, 0)
            exchange.responseBody.use { out ->
                repeat(100) {
                    out.write(ByteArray(8))
                    out.flush()
                    if (release.await(50, TimeUnit.MILLISECONDS)) return@use
                }
            }
            exchange.close()
        }
        server.createContext("/silent") {
            record(it)
            release.await(10, TimeUnit.SECONDS)
            it.close()
        }
        server.start()
    }

    @After
    fun stop() {
        release.countDown()
        server.stop(0)
    }

    private fun record(exchange: HttpExchange) {
        requests += exchange.requestHeaders.mapKeys { it.key.lowercase() }
    }

    private fun reply(exchange: HttpExchange, status: Int, body: ByteArray, vararg extra: Pair<String, String>) {
        record(exchange)
        extra.forEach { (name, value) -> exchange.responseHeaders.add(name, value) }
        exchange.sendResponseHeaders(status, if (body.isEmpty()) -1 else body.size.toLong())
        if (body.isNotEmpty()) exchange.responseBody.use { it.write(body) }
        exchange.close()
    }

    private fun url(path: String) = "http://${server.address.hostString}:${server.address.port}$path"

    @Test
    fun `returns the body and the useful headers`() {
        val sink = ByteArrayOutputStream()
        val response = transport.get(url("/body"), headers, 1_000, fast, sink)
        assertEquals(200, response.status)
        assertArrayEquals(BODY, sink.toByteArray())
        assertEquals(BODY.size.toLong(), response.bodyBytes)
        assertFalse(response.exceeded)
        assertEquals("59", response.headers["x-ratelimit-remaining"])
        assertEquals("30", response.headers["retry-after"])
        assertNull(response.headers["set-cookie"])
    }

    @Test
    fun `rate limit headers are returned with the error status`() {
        val response = transport.get(url("/limited"), headers, 1_000, fast, ByteArrayOutputStream())
        assertEquals(403, response.status)
        assertEquals("0", response.headers["x-ratelimit-remaining"])
        assertEquals("1700000000", response.headers["x-ratelimit-reset"])
    }

    @Test
    fun `silent server ends in a timeout`() {
        val start = System.nanoTime()
        val error = runCatching {
            transport.get(url("/silent"), headers, 1_000, HttpTimeouts(connectMs = 2_000, readMs = 300, totalMs = 10_000), ByteArrayOutputStream())
        }.exceptionOrNull()
        assertTrue(error.toString(), error is SocketTimeoutException)
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 5_000)
    }

    @Test
    fun `server that keeps sending slowly ends in a timeout once the total time is over`() {
        val start = System.nanoTime()
        val error = runCatching {
            transport.get(url("/slow"), headers, 10_000, HttpTimeouts(connectMs = 2_000, readMs = 2_000, totalMs = 300), ByteArrayOutputStream())
        }.exceptionOrNull()
        assertTrue(error.toString(), error is SocketTimeoutException)
        assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) < 3_000)
    }

    @Test
    fun `body longer than the expected size is cut and flagged`() {
        val sink = ByteArrayOutputStream()
        val response = transport.get(url("/body"), headers, 10, fast, sink)
        assertTrue(response.exceeded)
        assertEquals(10L, response.bodyBytes)
        assertEquals(10, sink.size())
    }

    @Test
    fun `body of exactly the expected size is not flagged`() {
        val response = transport.get(url("/body"), headers, BODY.size.toLong(), fast, ByteArrayOutputStream())
        assertFalse(response.exceeded)
        assertEquals(BODY.size.toLong(), response.bodyBytes)
    }

    @Test
    fun `progress reports the bytes received`() {
        val seen = mutableListOf<Long>()
        transport.get(url("/body"), headers, 1_000, fast, ByteArrayOutputStream()) { seen += it }
        assertEquals(BODY.size.toLong(), seen.last())
    }

    @Test
    fun `404 and 410 are returned without a body`() {
        listOf("/missing" to 404, "/gone" to 410).forEach { (path, status) ->
            val sink = ByteArrayOutputStream()
            val response = transport.get(url(path), headers, 1_000, fast, sink)
            assertEquals(status, response.status)
            assertEquals(0, sink.size())
        }
    }

    @Test
    fun `redirects are followed and the final url is reported`() {
        val response = transport.get(url("/redirect"), headers, 1_000, fast, ByteArrayOutputStream())
        assertEquals(200, response.status)
        assertEquals(url("/body"), response.finalUrl)
    }

    @Test
    fun `requests carry only technical headers and never an authorization or a cookie`() {
        transport.get(url("/body"), headers, 1_000, fast, ByteArrayOutputStream())
        transport.get(url("/body"), headers, 1_000, fast, ByteArrayOutputStream())
        assertEquals(2, requests.size)
        requests.forEach { request ->
            assertFalse(request.containsKey("authorization"))
            assertFalse(request.containsKey("cookie"))
            assertEquals(listOf("SygixOs/1.2.3"), request["user-agent"])
            assertEquals(listOf("application/vnd.github+json"), request["accept"])
            assertEquals(listOf("2022-11-28"), request["x-github-api-version"])
        }
    }

    private companion object {
        val BODY = "[{\"tag_name\":\"v0.0.2\"}]".toByteArray()
    }
}
