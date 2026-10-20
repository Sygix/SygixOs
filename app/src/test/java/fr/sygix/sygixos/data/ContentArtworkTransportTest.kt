/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.content.res.AssetFileDescriptor
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import androidx.test.core.app.ApplicationProvider
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.shadows.ShadowContentResolver

@RunWith(RobolectricTestRunner::class)
class ContentArtworkTransportTest {
    @get:Rule val folder = TemporaryFolder()
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val transport = AndroidArtworkTransport(context.contentResolver)
    private val uri = Uri.parse("content://fixture.artwork/image")
    private val entered = CountDownLatch(1)
    private val release = CountDownLatch(1)
    private val closed = CountDownLatch(1)
    private val closes = AtomicInteger()
    private val streamCloses = AtomicInteger()
    private val opens = AtomicInteger()
    private val streams = mutableListOf<java.io.FileInputStream>()
    private lateinit var file: File
    private var blockOpen = false
    private var blockRead = false

    @Before fun register() {
        file = folder.newFile("image.png")
        val bitmap = Bitmap.createBitmap(800, 450, Bitmap.Config.ARGB_8888)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        val provider = object : ContentProvider() {
            override fun onCreate() = true
            override fun query(uri: Uri, projection: Array<out String>?, selection: String?, selectionArgs: Array<out String>?, sortOrder: String?): Cursor? = null
            override fun getType(uri: Uri) = "image/png"
            override fun insert(uri: Uri, values: ContentValues?): Uri? = null
            override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?) = 0
            override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<out String>?) = 0
            override fun openTypedAssetFile(uri: Uri, mimeTypeFilter: String, opts: Bundle?, signal: CancellationSignal?): AssetFileDescriptor {
                opens.incrementAndGet()
                val descriptor = object : AssetFileDescriptor(ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY), 0, file.length()) {
                    override fun close() { closes.incrementAndGet(); super.close(); closed.countDown() }
                    override fun createInputStream(): java.io.FileInputStream = object : AutoCloseInputStream(this) {
                        private var closing = false
                        override fun read(buffer: ByteArray): Int {
                            if (blockRead) { entered.countDown(); release.await() }
                            return super.read(buffer)
                        }
                        override fun close() {
                            if (closing) { super.close(); return }
                            closing = true
                            try { streamCloses.incrementAndGet(); super.close() }
                            finally { closing = false; closed.countDown() }
                        }
                    }.also { streams += it }
                }
                if (blockOpen) { entered.countDown(); release.await() }
                return descriptor
            }
        }
        provider.attachInfo(context, android.content.pm.ProviderInfo().apply { authority = "fixture.artwork" })
        ShadowContentResolver.registerProviderInternal("fixture.artwork", provider)
    }

    @After fun release() { release.countDown() }

    @Test fun `content client reads complete bytes and closes its owned stream once`() {
        val image = transport.read(uri.toString(), ArtworkCancellation())
        assertArrayEquals(file.readBytes(), image.bytes)
        assertEquals(800, image.dimensions.width)
        assertEquals(1, opens.get())
        assertEquals(0, closes.get())
        assertEquals(1, streamCloses.get())
    }

    @Test fun `late content descriptor after timeout is closed once without reading it`() {
        blockOpen = true
        checkBlocked()
    }

    @Test fun `content stream that ignores cancellation stays bounded and closes once when released`() {
        blockRead = true
        checkBlocked()
    }

    private fun checkBlocked() = runBlocking {
        ArtworkScheduler().use { scheduler ->
            val cancellation = ArtworkCancellation()
            try {
                val result = withTimeout(8_000) { scheduler.run("fixture", 2_000, cancellation::cancel) { transport.read(uri.toString(), cancellation) } }
                assertTrue(entered.await(1, TimeUnit.SECONDS))
                assertTrue(result.exceptionOrNull() is TimeoutException)
                assertEquals(0, closes.get())
            } finally { release.countDown() }
            assertTrue(closed.await(2, TimeUnit.SECONDS))
            assertEquals(1, opens.get())
            assertEquals(if (blockOpen) 1 else 0, closes.get())
            assertEquals(if (blockOpen) 0 else 1, streamCloses.get())
        }
    }
}
