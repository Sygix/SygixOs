/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.memory.MemoryCache
import coil.network.HttpException
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class VisualValidatorTest {

    private class Loader(val failure: (String) -> Throwable) : ImageLoader {
        override val defaults = DefaultRequestOptions()
        override val components = ComponentRegistry()
        override val memoryCache: MemoryCache? = null
        override val diskCache: DiskCache? = null
        override fun enqueue(request: ImageRequest): Disposable = error("unused")
        override suspend fun execute(request: ImageRequest): ImageResult {
            val error = failure(request.data.toString())
            if (error is CancellationException) throw error
            return ErrorResult(null, request, error)
        }
        override fun shutdown() = Unit
        override fun newBuilder(): ImageLoader.Builder = error("unused")
    }

    private fun validator(failure: (String) -> Throwable) =
        VisualValidator(ApplicationProvider.getApplicationContext(), Loader(failure), Dispatchers.Unconfined)

    private fun http(code: Int) = HttpException(
        Response.Builder()
            .request(Request.Builder().url("https://example.invalid/").build())
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("")
            .build(),
    )

    @Test
    fun `only network failures stay retryable`() = runBlocking {
        val failures = mapOf(
            "dns" to UnknownHostException(),
            "io" to IOException(),
            "server" to http(503),
            "missing" to http(404),
            "decode" to IllegalStateException(),
        )
        val validator = validator { failures.getValue(it) }
        assertEquals(
            mapOf(
                "dns" to VisualCheck(null, retryable = true),
                "io" to VisualCheck(null, retryable = true),
                "server" to VisualCheck(null, retryable = true),
                "missing" to VisualCheck(null),
                "decode" to VisualCheck(null),
            ),
            failures.mapValues { validator.validate(it.key) },
        )
    }

    @Test
    fun `cancellation propagates`() {
        assertThrows(CancellationException::class.java) {
            runBlocking { validator { CancellationException("left") }.validate("a") }
        }
    }

    private val requested = mutableListOf<String>()
    private val results = mutableListOf<ImageResult>()

    private fun headerValidator(served: Map<String, Any>): VisualValidator {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val real = ImageLoader.Builder(context).dispatcher(Dispatchers.Unconfined).interceptorDispatcher(Dispatchers.Unconfined)
            .components {
                add(Fetcher.Factory<Uri> { data, options, _ ->
                    Fetcher {
                        val source = when (val served = served.getValue(data.toString())) {
                            is Throwable -> throw served
                            "broken" -> brokenSource().buffer()
                            else -> Buffer().write(png(served as String))
                        }
                        SourceResult(ImageSource(source, options.context), "image/png", DataSource.NETWORK)
                    }
                })
            }
            .build()
        val loader = object : ImageLoader by real {
            override suspend fun execute(request: ImageRequest) = real.execute(request).also {
                requested += request.data.toString()
                results += it
            }
        }
        return VisualValidator(context, loader, Dispatchers.Unconfined)
    }

    @Test
    fun `checks the width from the image header without decoding pixels`() = runBlocking {
        val served = mapOf("img://a" to "2400x1350", "img://b" to "1920x1080", "img://c" to "640x360", "img://d" to "900x1200", "img://e" to "broken", "img://f" to "480x270")
        val validator = headerValidator(served)
        assertEquals(
            listOf(VisualCheck("img://a"), VisualCheck("img://b"), VisualCheck("img://c"), VisualCheck(null), VisualCheck(null, retryable = true), VisualCheck(null)),
            served.keys.map { validator.validate(it) },
        )
        val decoded = results.mapNotNull { (it as? SuccessResult)?.drawable }
        assertEquals(listOf(1920, 1920, 640, 810, 480), decoded.map { it.intrinsicWidth })
        assertTrue(decoded.none { it is BitmapDrawable })
    }

    @Test
    fun `youtube thumbnails try the full resolution variant first`() = runBlocking {
        fun maxres(id: String) = "https://i.ytimg.com/vi/$id/maxresdefault.jpg"
        val upgraded = "https://i.ytimg.com/vi/AAAAAAAAAAA/sddefault.jpg?v=65f1a2b3"
        val missingMaxres = "https://img.youtube.com/vi/BBBBBBBBBBB/hq720.jpg"
        val tooSmall = "https://i.ytimg.com/vi/CCCCCCCCCCC/sddefault.jpg"
        val offline = "https://i.ytimg.com/vi/DDDDDDDDDDD/sddefault.jpg"
        val otherHost = "https://example.invalid/vi/EEEEEEEEEEE/sddefault.jpg"
        val validator = headerValidator(
            mapOf(
                maxres("AAAAAAAAAAA") to "1280x720", upgraded to "640x480",
                maxres("BBBBBBBBBBB") to http(404), missingMaxres to "1280x720",
                maxres("CCCCCCCCCCC") to http(404), tooSmall to "480x360",
                maxres("DDDDDDDDDDD") to IOException(), offline to "480x360",
                otherHost to "640x480",
            ),
        )
        assertEquals(
            listOf(VisualCheck(maxres("AAAAAAAAAAA")), VisualCheck(missingMaxres), VisualCheck(null), VisualCheck(null, retryable = true), VisualCheck(otherHost)),
            listOf(upgraded, missingMaxres, tooSmall, offline, otherHost).map { validator.validate(it) },
        )
        assertEquals(
            listOf(
                maxres("AAAAAAAAAAA"),
                maxres("BBBBBBBBBBB"), missingMaxres,
                maxres("CCCCCCCCCCC"), tooSmall,
                maxres("DDDDDDDDDDD"), offline,
                otherHost,
            ),
            requested,
        )
    }

    @Test
    fun `youtube thumbnail ids are read from the published forms`() {
        val maxres = "https://i.ytimg.com/vi/dQw4w9WgXc_/maxresdefault.jpg"
        listOf(
            "https://i.ytimg.com/vi/dQw4w9WgXc_/sddefault.jpg",
            "https://i.ytimg.com/vi/dQw4w9WgXc_/sddefault.jpg?v=65f1a2b3",
            "https://i.ytimg.com/vi/dQw4w9WgXc_/sddefault_custom_1.jpg?sqp=CNjB-sQG&rs=AOn4CLB",
            "https://img.youtube.com/vi/dQw4w9WgXc_/hqdefault.jpg",
            "https://i9.ytimg.com/vi_webp/dQw4w9WgXc_/mqdefault.webp",
        ).forEach { assertEquals(listOf(maxres, it), visualCandidates(it)) }
        listOf(maxres, "https://example.invalid/vi/dQw4w9WgXc_/sddefault.jpg", "https://i.ytimg.com/an_webp/dQw4w9WgXc_/mqdefault_6s.webp")
            .forEach { assertEquals(listOf(it), visualCandidates(it)) }
    }

    private fun png(size: String): ByteArray {
        val (width, height) = size.split("x").map(String::toInt)
        return ByteArrayOutputStream().also { Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 0, it) }.toByteArray()
    }

    private fun brokenSource() = object : Source {
        override fun read(sink: Buffer, byteCount: Long): Long = throw IOException("reset")
        override fun timeout() = Timeout.NONE
        override fun close() = Unit
    }
}
