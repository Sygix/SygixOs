/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.net.Uri
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.test.core.app.ApplicationProvider
import java.io.IOException
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
class HeroVideoCacheTest {
    @get:Rule val folder = TemporaryFolder()
    @Test fun budgetScalesWithFreeSpaceAndDisablesSmallCache() {
        val mb = 1024L * 1024
        assertEquals(0L, HeroVideoCache.budget(255 * mb))
        assertEquals(64 * mb, HeroVideoCache.budget(256 * mb))
        assertEquals(128 * mb, HeroVideoCache.budget(512 * mb))
        assertEquals(300 * mb, HeroVideoCache.budget(4_000 * mb))
    }

    @Test fun writeFailureContinuesStreamingAtTheExactUnreadPosition() {
        val offsets = mutableListOf<Long>()
        val source = StreamingFallbackDataSource(DataSource.Factory { BytesSource(failAt = 2) }, DataSource.Factory { BytesSource(onOpen = { offsets += it }) })
        source.open(DataSpec(Uri.parse("https://example.invalid/clip")))
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(2)
        while (true) { val n = source.read(buffer, 0, buffer.size); if (n < 0) break; output.write(buffer, 0, n) }
        source.close()
        assertArrayEquals(byteArrayOf(0, 1, 2, 3, 4, 5), output.toByteArray())
        assertEquals(listOf(2L), offsets)
    }

    @Test fun openFailureUsesStreamingWithoutExposingCacheError() {
        val source = StreamingFallbackDataSource(DataSource.Factory { BytesSource(failAt = -1) }, DataSource.Factory { BytesSource() })
        assertEquals(6L, source.open(DataSpec(Uri.parse("https://example.invalid/clip"))))
        val buffer = ByteArray(6)
        assertEquals(6, source.read(buffer, 0, 6))
        source.close()
    }

    @Test fun noSpaceAndCreationFailureUseTheInjectedStreamingSource() = kotlinx.coroutines.runBlocking {
        val context: Context = ApplicationProvider.getApplicationContext()
        for (free in listOf(0L, Long.MAX_VALUE)) {
            val cache = HeroVideoCache(context, upstream = DataSource.Factory { BytesSource() }, freeSpace = { free },
                createCache = { _, _ -> throw IOException("Cache creation fixture") })
            try {
                val source = cache.dataSource().createDataSource()
                assertEquals(6L, source.open(DataSpec(Uri.parse("https://example.invalid/clip"))))
                assertEquals(6, source.read(ByteArray(6), 0, 6))
                source.close()
            } finally { cache.close() }
        }
    }

    @Test fun fullCacheEvictsOldestVideoBeforeAddingAnother() {
        val context: Context = ApplicationProvider.getApplicationContext()
        val database = androidx.media3.database.StandaloneDatabaseProvider(context)
        val cache = SimpleCache(folder.newFolder(), LeastRecentlyUsedCacheEvictor(8), database)
        try {
            fun write(key: String) {
                val hole = cache.startReadWrite(key, 0, 6)
                val file = cache.startFile(key, 0, 6)
                file.writeBytes(ByteArray(6))
                cache.commitFile(file, 6)
                cache.releaseHoleSpan(hole)
            }
            write("first")
            write("second")
            assertFalse(cache.isCached("first", 0, 6))
            assertTrue(cache.isCached("second", 0, 6))
        } finally { cache.release(); database.close() }
    }

    private class BytesSource(private val failAt: Int? = null, private val onOpen: (Long) -> Unit = {}) : DataSource {
        private var offset = 0
        override fun open(dataSpec: DataSpec): Long {
            if (failAt == -1) throw IOException("Cache unavailable")
            offset = dataSpec.position.toInt()
            onOpen(dataSpec.position)
            return 6L - offset
        }
        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (this.offset == failAt) throw IOException("Disk full")
            if (this.offset >= 6) return -1
            val count = minOf(length, 6 - this.offset)
            repeat(count) { buffer[offset + it] = (this.offset + it).toByte() }
            this.offset += count
            return count
        }
        override fun getUri(): Uri = Uri.parse("https://example.invalid/clip")
        override fun addTransferListener(transferListener: TransferListener) {}
        override fun close() {}
    }
}
