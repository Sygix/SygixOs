/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import coil.disk.DiskCache
import coil.intercept.Interceptor
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.ImageResult
import coil.request.SuccessResult
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class VisualFreshness(private val disk: DiskCache, private val memory: MemoryCache) : Interceptor {

    private val checkedAt = ConcurrentHashMap<String, Long>()

    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val url = request.data.toString()
        if (!url.startsWith("http") || !request.diskCachePolicy.readEnabled) return chain.proceed(request)
        val now = System.currentTimeMillis()
        if (checkedAt[url]?.let { now - it < MAX_AGE_MS } == true) return chain.proceed(request)
        checkedAt[url] = now
        if (!withContext(Dispatchers.IO) { stale(url, now) }) return chain.proceed(request)
        memory.keys.filter { it.key == url }.forEach(memory::remove)
        val fresh = chain.proceed(request.newBuilder().diskCachePolicy(CachePolicy.WRITE_ONLY).build())
        return if (fresh is SuccessResult) fresh else chain.proceed(request)
    }

    private fun stale(url: String, now: Long): Boolean = disk.openSnapshot(url)?.use { snapshot ->
        disk.fileSystem.metadata(snapshot.data).lastModifiedAtMillis?.let { now - it > MAX_AGE_MS } == true
    } == true

    private companion object {
        const val MAX_AGE_MS = 24 * 60 * 60_000L
    }
}
