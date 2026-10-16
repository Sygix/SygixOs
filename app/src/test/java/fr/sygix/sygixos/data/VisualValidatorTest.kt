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
                "dns" to VisualCheck(false, retryable = true),
                "io" to VisualCheck(false, retryable = true),
                "server" to VisualCheck(false, retryable = true),
                "missing" to VisualCheck(false),
                "decode" to VisualCheck(false),
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

    @Test
    fun `checks the width from the image header without decoding pixels`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val results = mutableListOf<ImageResult>()
        val real = ImageLoader.Builder(context).dispatcher(Dispatchers.Unconfined).interceptorDispatcher(Dispatchers.Unconfined)
            .components {
                add(Fetcher.Factory<Uri> { data, options, _ ->
                    Fetcher {
                        val source = if (data.scheme == "broken") brokenSource().buffer() else Buffer().write(png(data.host!!))
                        SourceResult(ImageSource(source, options.context), "image/png", DataSource.NETWORK)
                    }
                })
            }
            .build()
        val loader = object : ImageLoader by real {
            override suspend fun execute(request: ImageRequest) = real.execute(request).also { results += it }
        }
        val validator = VisualValidator(context, loader, Dispatchers.Unconfined)
        assertEquals(
            listOf(VisualCheck(true), VisualCheck(false), VisualCheck(false, retryable = true)),
            listOf("img://2400x1350", "img://900x1200", "broken://x").map { validator.validate(it) },
        )
        val decoded = results.mapNotNull { (it as? SuccessResult)?.drawable }
        assertEquals(listOf(1920, 810), decoded.map { it.intrinsicWidth })
        assertTrue(decoded.none { it is BitmapDrawable })
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
