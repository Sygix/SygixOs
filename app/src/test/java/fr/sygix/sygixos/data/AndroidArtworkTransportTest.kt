/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.Bitmap
import androidx.test.core.app.ApplicationProvider
import com.sun.net.httpserver.HttpServer
import java.net.InetAddress
import java.net.InetSocketAddress
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidArtworkTransportTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val transport = AndroidArtworkTransport(context.contentResolver)
    private lateinit var server: HttpServer
    private val workers = Executors.newFixedThreadPool(2)
    private val release = CountDownLatch(1)
    private val entered = CountDownLatch(1)
    private lateinit var bytes: ByteArray

    @Before fun start() {
        val bitmap = Bitmap.createBitmap(800, 450, Bitmap.Config.ARGB_8888)
        val output = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
        bitmap.recycle()
        bytes = output.toByteArray()
        server = HttpServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        server.executor = workers
        server.createContext("/image") { exchange ->
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        server.createContext("/status") { exchange ->
            exchange.sendResponseHeaders(exchange.requestURI.query.toInt(), -1)
            exchange.close()
        }
        server.createContext("/slow") { exchange ->
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.write(bytes, 0, 1)
            exchange.responseBody.flush()
            entered.countDown()
            release.await(5, TimeUnit.SECONDS)
            exchange.close()
        }
        server.start()
    }

    @After fun stop() {
        release.countDown()
        server.stop(0)
        workers.shutdownNow()
        assertTrue(workers.awaitTermination(5, TimeUnit.SECONDS))
    }

    private fun url(path: String) = "http://${server.address.hostString}:${server.address.port}$path"

    @Test fun `HTTP source preserves complete bytes and actual dimensions`() {
        val image = transport.read(url("/image"), ArtworkCancellation())
        assertArrayEquals(bytes, image.bytes)
        assertEquals(800, image.dimensions.width)
        assertEquals(450, image.dimensions.height)
    }

    @Test fun `HTTP failures preserve status without publishing image bytes`() {
        for (status in listOf(404, 410, 503)) {
            val failure = assertThrows(ArtworkHttpException::class.java) { transport.read(url("/status?$status"), ArtworkCancellation()) }
            assertEquals(status, failure.status)
        }
    }

    @Test fun `total result deadline covers a stalled HTTP response body`() = runBlocking {
        ArtworkScheduler().use { scheduler ->
            val cancellation = ArtworkCancellation()
            val result = withTimeout(3_000) { scheduler.run("app", 500, cancellation::cancel) { transport.read(url("/slow"), cancellation) } }
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            assertTrue(result.exceptionOrNull() is TimeoutException)
        }
    }
}
