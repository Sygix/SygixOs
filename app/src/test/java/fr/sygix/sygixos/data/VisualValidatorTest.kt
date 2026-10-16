/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import androidx.test.core.app.ApplicationProvider
import coil.ComponentRegistry
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.network.HttpException
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
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
        val checks = validator { failures.getValue(it) }.validate(failures.keys.toList(), keepInMemory = 0).toList()
        assertEquals(
            listOf(
                VisualCheck("dns", false, retryable = true),
                VisualCheck("io", false, retryable = true),
                VisualCheck("server", false, retryable = true),
                VisualCheck("missing", false),
                VisualCheck("decode", false),
            ),
            checks,
        )
    }

    @Test
    fun `cancellation propagates without emitting a check`() {
        val emitted = mutableListOf<VisualCheck>()
        assertThrows(CancellationException::class.java) {
            runBlocking { validator { CancellationException("left") }.validate(listOf("a", "b"), keepInMemory = 0).collect { emitted += it } }
        }
        assertTrue(emitted.isEmpty())
    }
}
