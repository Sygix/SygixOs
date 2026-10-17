/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import androidx.test.core.app.ApplicationProvider
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Size
import java.io.File
import java.io.IOException
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VisualFreshnessTest {
    @get:Rule val folder = TemporaryFolder()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val disk by lazy { DiskCache.Builder().directory(folder.newFolder()).build() }
    private val memory by lazy { MemoryCache.Builder(context).maxSizeBytes(1024 * 1024).build() }
    private val url = "https://example.invalid/poster.jpg"

    private class Chain(override val request: ImageRequest, private val offline: Boolean) : Interceptor.Chain {
        val policies = mutableListOf<CachePolicy>()
        override val size: Size = Size.ORIGINAL
        override fun withRequest(request: ImageRequest): Interceptor.Chain = this
        override fun withSize(size: Size): Interceptor.Chain = this
        override suspend fun proceed(request: ImageRequest): ImageResult {
            policies += request.diskCachePolicy
            val refetch = request.diskCachePolicy == CachePolicy.WRITE_ONLY
            return if (offline && refetch) ErrorResult(null, request, IOException("offline")) else SuccessResult(ColorDrawable(), request, DataSource.DISK)
        }
    }

    private fun cached(ageMillis: Long) {
        val editor = disk.openEditor(url)!!
        disk.fileSystem.write(editor.data) { writeUtf8("poster") }
        disk.fileSystem.write(editor.metadata) { writeUtf8("headers") }
        editor.commit()
        val data = disk.openSnapshot(url)!!.use { it.data.toString() }
        assertTrue(File(data).setLastModified(System.currentTimeMillis() - ageMillis))
        memory[MemoryCache.Key(url)] = MemoryCache.Value(Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888))
    }

    private fun intercept(offline: Boolean = false, data: Any = url): Chain = runBlocking {
        Chain(ImageRequest.Builder(context).data(data).build(), offline).also { VisualFreshness(disk, memory).intercept(it) }
    }

    @Test
    fun `a recent entry is served without a network request`() {
        cached(ageMillis = 60_000)
        assertEquals(listOf(CachePolicy.ENABLED), intercept().policies)
        assertNotNull(memory[MemoryCache.Key(url)])
    }

    @Test
    fun `an entry older than a day is fetched again and its memory copies are dropped`() {
        cached(ageMillis = DAY + 60_000)
        assertEquals(listOf(CachePolicy.WRITE_ONLY), intercept().policies)
        assertNull(memory[MemoryCache.Key(url)])
    }

    @Test
    fun `the cached entry is served when the new fetch fails`() {
        cached(ageMillis = DAY + 60_000)
        assertEquals(listOf(CachePolicy.WRITE_ONLY, CachePolicy.ENABLED), intercept(offline = true).policies)
    }

    @Test
    fun `images outside the network are never rechecked`() {
        assertEquals(listOf(CachePolicy.ENABLED), intercept(data = "content://media/poster").policies)
    }

    private companion object {
        const val DAY = 24 * 60 * 60_000L
    }
}
