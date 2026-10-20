/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.net.Uri
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.TransferListener
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
internal class HeroVideoCache(
    private val context: Context,
    private val upstream: DataSource.Factory = DefaultDataSource.Factory(context),
    private val freeSpace: () -> Long = { context.cacheDir.usableSpace },
    private val createCache: (java.io.File, Long) -> SimpleCache = { directory, size ->
        SimpleCache(directory, LeastRecentlyUsedCacheEvictor(size), StandaloneDatabaseProvider(context))
    },
) {
    private val mutex = Mutex()
    private var factory: DataSource.Factory? = null
    private var cache: SimpleCache? = null

    suspend fun dataSource(): DataSource.Factory = withContext(Dispatchers.IO) {
        mutex.withLock {
            factory ?: create().also { factory = it }
        }
    }

    private fun create(): DataSource.Factory {
        return try {
            val directory = context.cacheDir.resolve("hero-video")
            val size = budget(freeSpace())
            if (size == 0L) return upstream
            val created = createCache(directory, size)
            cache = created
            created.checkInitialization()
            val cached = CacheDataSource.Factory().setCache(created).setUpstreamDataSourceFactory(upstream)
                .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
            DataSource.Factory { StreamingFallbackDataSource(cached, upstream) }
        } catch (_: Exception) {
            runCatching { cache?.release() }
            cache = null
            android.util.Log.w("HeroVideo", "cause=cache-unavailable")
            upstream
        }
    }

    suspend fun close() = withContext(Dispatchers.IO) { mutex.withLock { cache?.release(); cache = null; factory = null } }

    companion object {
        fun budget(usableSpace: Long): Long = minOf(300L * 1024 * 1024, usableSpace.coerceAtLeast(0) / 4).takeIf { it >= 64L * 1024 * 1024 } ?: 0
    }
}

internal class StreamingFallbackDataSource(private val cached: DataSource.Factory, private val upstream: DataSource.Factory) : DataSource {
    private var source: DataSource? = null
    private var spec: DataSpec? = null
    private var consumed = 0L
    private var streaming = false
    private val listeners = mutableListOf<TransferListener>()

    override fun addTransferListener(transferListener: TransferListener) { listeners += transferListener; source?.addTransferListener(transferListener) }
    override fun open(dataSpec: DataSpec): Long {
        spec = dataSpec
        consumed = 0
        streaming = false
        return try { replace(cached).open(dataSpec) } catch (_: IOException) { fallback() }
    }
    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        val count = try { checkNotNull(source).read(buffer, offset, length) } catch (error: IOException) {
            if (streaming) throw error
            fallback()
            checkNotNull(source).read(buffer, offset, length)
        }
        if (count > 0) consumed += count
        return count
    }
    private fun fallback(): Long { streaming = true; return replace(upstream).open(checkNotNull(spec).subrange(consumed)) }
    private fun replace(factory: DataSource.Factory): DataSource {
        runCatching { source?.close() }
        return factory.createDataSource().also { source = it; listeners.forEach(it::addTransferListener) }
    }
    override fun getUri(): Uri? = source?.uri
    override fun getResponseHeaders(): Map<String, List<String>> = source?.responseHeaders.orEmpty()
    override fun close() { source?.close(); source = null }
}
