/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork
import java.io.Closeable
import java.io.FileNotFoundException
import java.util.concurrent.TimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

internal enum class ArtworkFailure { ABSENT, QUALITY, DENIED, TIMEOUT, PROVIDER, TRANSPORT }
internal sealed interface ArtworkResult {
    data class Ready(val artwork: CachedArtwork) : ArtworkResult
    data class Failed(val cause: ArtworkFailure) : ArtworkResult
}

internal class ProgramArtworkService(
    private val store: ArtworkDiskStore,
    private val transport: ArtworkTransport,
    private val scheduler: ArtworkScheduler = ArtworkScheduler(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val timeoutMillis: Long = 8_000,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val diskDispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : Closeable {
    private class Flight(val job: Deferred<ArtworkResult>, var detached: Boolean, var subscribers: Int = 0)
    private val pending = mutableMapOf<String, Flight>()
    private val failures = LinkedHashMap<String, Pair<Long, ArtworkResult.Failed>>()
    private val revisions = MutableStateFlow(0L)
    private val expirations = LinkedHashMap<String, Pair<Long, kotlinx.coroutines.Job>>()
    val changes: StateFlow<Long> = revisions

    suspend fun load(source: ProgramArtwork, policy: ArtworkPolicy? = null): ArtworkResult {
        if (source.candidates.isEmpty()) return ArtworkResult.Failed(ArtworkFailure.ABSENT)
        return withTimeoutOrNull(timeoutMillis) {
            var failure = ArtworkResult.Failed(ArtworkFailure.ABSENT)
            for (uri in source.candidates.distinct()) {
                val key = artworkHash(source.packageName + "\u0000" + uri + "\u0000" + source.identity)
                val cached = withContext(diskDispatcher) { store.read(key, uri, clock()) }
                val result = if (cached != null) {
                    track(key, cached)
                    if (!ArtworkAge.fresh(cached.fetchedAt, clock())) refresh(source.packageName, uri, key, notify = true)
                    ArtworkResult.Ready(cached)
                } else await(refresh(source.packageName, uri, key))
                if (result is ArtworkResult.Ready) {
                    if (policy == null || policy.accepts(result.artwork.dimensions)) return@withTimeoutOrNull result
                    failure = ArtworkResult.Failed(ArtworkFailure.QUALITY)
                } else failure = result as ArtworkResult.Failed
            }
            failure
        } ?: ArtworkResult.Failed(ArtworkFailure.TIMEOUT)
    }

    private suspend fun await(flight: Flight): ArtworkResult {
        synchronized(this) { flight.subscribers++ }
        return try { flight.job.await() } finally {
            synchronized(this) {
                flight.subscribers--
                if (flight.subscribers == 0 && !flight.detached && !flight.job.isCompleted) flight.job.cancel()
            }
        }
    }

    @Synchronized private fun refresh(app: String, uri: String, key: String, notify: Boolean = false): Flight {
        pending[key]?.takeUnless { it.job.isCancelled }?.let { it.detached = it.detached || notify; return it }
        failures[key]?.let { (at, result) ->
            if (clock() >= at && clock() - at < RETRY_MS) return Flight(kotlinx.coroutines.CompletableDeferred(result), false)
            failures.remove(key)
        }
        if (pending.size >= 64) return Flight(kotlinx.coroutines.CompletableDeferred(ArtworkResult.Failed(ArtworkFailure.TIMEOUT)), false)
        val job = scope.async(start = kotlinx.coroutines.CoroutineStart.LAZY) {
            val current = store.read(key, uri, clock())
            if (current != null && ArtworkAge.fresh(current.fetchedAt, clock())) return@async ArtworkResult.Ready(current)
            val cancellation = ArtworkCancellation()
            val result = scheduler.run(app, timeoutMillis, cancellation::cancel) { transport.read(uri, cancellation) }
            val loaded = result.fold(
                onSuccess = { bytes ->
                    val artwork = CachedArtwork(uri, bytes.bytes, bytes.dimensions, clock(), artworkHash(bytes.bytes))
                    try {
                        store.write(key, artwork)
                        track(key, artwork)
                        if (notify) revisions.update { it + 1 }
                        ArtworkResult.Ready(artwork)
                    } catch (_: Exception) { ArtworkResult.Failed(ArtworkFailure.TRANSPORT) }
                },
                onFailure = { error ->
                    ArtworkResult.Failed(when (error) {
                        is SecurityException -> ArtworkFailure.DENIED
                        is TimeoutException, is ArtworkBusyException -> ArtworkFailure.TIMEOUT
                        is android.os.RemoteException -> ArtworkFailure.PROVIDER
                        is FileNotFoundException -> ArtworkFailure.ABSENT
                        else -> ArtworkFailure.TRANSPORT
                    })
                },
            )
            synchronized(this@ProgramArtworkService) {
                if (loaded is ArtworkResult.Failed) {
                    failures[key] = clock() to loaded
                    while (failures.size > 256) failures.remove(failures.keys.first())
                }
            }
            loaded
        }
        val flight = Flight(job, notify)
        pending[key] = flight
        job.invokeOnCompletion { synchronized(this) { if (pending[key] === flight) pending.remove(key) } }
        job.start()
        return flight
    }

    override fun close() { scope.cancel(); scheduler.close() }

    fun onForeground() { revisions.update { it + 1 } }

    suspend fun decodeFailed(source: ProgramArtwork, artwork: CachedArtwork): CachedArtwork? = withContext(diskDispatcher) {
        val key = artworkHash(source.packageName + "\u0000" + artwork.uri + "\u0000" + source.identity)
        synchronized(this@ProgramArtworkService) { failures[key] = clock() to ArtworkResult.Failed(ArtworkFailure.TRANSPORT) }
        store.reject(key, artwork.uri, artwork.version, clock())
    }

    @Synchronized private fun track(key: String, artwork: CachedArtwork) {
        val deadline = if (ArtworkAge.fresh(artwork.fetchedAt, clock())) artwork.fetchedAt + ArtworkAge.FRESH_MS else artwork.expiresAt
        if (expirations[key]?.first == deadline) return
        expirations.remove(key)?.second?.cancel()
        val job = scope.launch {
            delay((deadline - clock()).coerceAtLeast(1))
            revisions.update { it + 1 }
        }
        expirations[key] = deadline to job
        while (expirations.size > 256) expirations.remove(expirations.keys.first())?.second?.cancel()
    }

    private companion object { const val RETRY_MS = 30_000L }
}
