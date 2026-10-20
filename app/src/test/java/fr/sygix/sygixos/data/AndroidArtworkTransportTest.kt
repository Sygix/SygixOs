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
    @get:org.junit.Rule val main = fr.sygix.sygixos.MainDispatcherRule()
    @get:org.junit.Rule val folder = org.junit.rules.TemporaryFolder()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val transport = ProgramImageReader(context.contentResolver, okhttp3.OkHttpClient())
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
        val image = transport.read(android.net.Uri.parse(url("/image")), ImageCancellation())
        assertArrayEquals(bytes, image)
        assertEquals(800, verifiedDimensions(image).width)
        assertEquals(450, verifiedDimensions(image).height)
    }

    @Test fun `HTTP to HTTPS redirect is followed and retained under the published URI`() = runBlocking {
        val file = java.io.File(folder.root, "fixture.p12")
        val password = "fixture-only".toCharArray()
        val process = ProcessBuilder(java.io.File(System.getProperty("java.home"), "bin/keytool").path,
            "-genkeypair", "-alias", "fixture", "-keyalg", "RSA", "-keysize", "2048", "-dname", "CN=localhost",
            "-ext", "SAN=dns:localhost", "-validity", "1", "-storetype", "PKCS12", "-keystore", file.path,
            "-storepass", String(password), "-keypass", String(password), "-noprompt").redirectErrorStream(true).start()
        process.inputStream.use { it.readBytes() }
        assertTrue(process.waitFor(15, TimeUnit.SECONDS))
        assertEquals(0, process.exitValue())
        val store = java.security.KeyStore.getInstance("PKCS12").apply { file.inputStream().use { load(it, password) } }
        val keys = javax.net.ssl.KeyManagerFactory.getInstance(javax.net.ssl.KeyManagerFactory.getDefaultAlgorithm()).apply { init(store, password) }
        val trust = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm()).apply { init(store) }
        val tls = javax.net.ssl.SSLContext.getInstance("TLS").apply { init(keys.keyManagers, trust.trustManagers, null) }
        val secure = com.sun.net.httpserver.HttpsServer.create(InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0)
        secure.httpsConfigurator = com.sun.net.httpserver.HttpsConfigurator(tls)
        secure.executor = workers
        val hits = java.util.concurrent.atomic.AtomicInteger()
        secure.createContext("/image") { exchange ->
            hits.incrementAndGet()
            exchange.sendResponseHeaders(200, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
            exchange.close()
        }
        secure.start()
        server.createContext("/redirect") { exchange ->
            exchange.responseHeaders.add("Location", "https://localhost:${secure.address.port}/image")
            exchange.sendResponseHeaders(302, -1)
            exchange.close()
        }
        try {
            val http = okhttp3.OkHttpClient.Builder().sslSocketFactory(tls.socketFactory, trust.trustManagers.single() as javax.net.ssl.X509TrustManager).build()
            ArtworkFixture(folder.newFolder(), ProgramImageReader(context.contentResolver, http)).use { fixture ->
                val uri = url("/redirect")
                val source = fr.sygix.sygixos.model.ProgramArtwork("fixture.app", listOf(uri), "fiction")
                assertNotNull(fixture.repository.resolve(source, fr.sygix.sygixos.domain.ArtworkPolicy.Hero).first)
                assertNotNull(fixture.repository.resolve(source, fr.sygix.sygixos.domain.ArtworkPolicy.Card(396)).first)
                assertEquals(1, hits.get())
                assertEquals(uri, fixture.db.artwork().images().single().diskKey)
            }
            http.connectionPool.evictAll()
            http.dispatcher.executorService.shutdown()
        } finally { secure.stop(0) }
    }

    @Test fun `HTTP failures preserve status without publishing image bytes`() {
        for (status in listOf(404, 410, 503)) {
            val failure = assertThrows(java.io.IOException::class.java) { transport.read(android.net.Uri.parse(url("/status?$status")), ImageCancellation()) }
            assertTrue(failure.message.orEmpty().contains(status.toString()))
        }
    }

    @Test fun `total result deadline covers a stalled HTTP response body`() = runBlocking {
        ArtworkScheduler().use { scheduler ->
            val cancellation = ImageCancellation()
            val result = withTimeout(3_000) { scheduler.run("app", 500, cancellation::cancel) { transport.read(android.net.Uri.parse(url("/slow")), cancellation) } }
            assertTrue(entered.await(1, TimeUnit.SECONDS))
            assertTrue(result.exceptionOrNull() is TimeoutException)
        }
    }
}
