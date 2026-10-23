/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class TrackingController(
    private val provider: WatchTrackingProvider,
    private val store: TrackingSessionStore,
    private val scope: CoroutineScope,
) {
    private val mutable = MutableStateFlow(TrackingState())
    val state = mutable.asStateFlow()
    private var token: String? = null
    private var work: Job? = null
    private val storage = Mutex()

    suspend fun <T> authenticated(block: suspend (String) -> T): T {
        val current = token ?: throw TrackingException(TrackingFailure.ACCESS)
        val generation = state.value.generation
        val result = block(current)
        if (generation != state.value.generation || token != current) throw TrackingException(TrackingFailure.ACCESS)
        return result
    }

    fun restore() {
        val generation = state.value.generation
        work?.cancel()
        work = scope.launch {
            try {
                val saved = storage.withLock { store.read() }
                if (generation != state.value.generation || saved == null) return@launch
                token = saved
                load(generation, saved)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == state.value.generation) mutable.value = TrackingState(generation, failure = TrackingFailure.STORAGE)
            }
        }
    }

    suspend fun connect(value: String, stillValid: () -> Boolean = { true }) {
        work?.cancel()
        val generation = state.value.generation + 1
        mutable.value = TrackingState(generation, loading = true)
        token = null
        try {
            val account = provider.account(value)
            currentCoroutineContext().ensureActive()
            storage.withLock {
                if (generation != state.value.generation || !stillValid()) return
                store.write(value)
                token = value
            }
            if (generation != state.value.generation) return
            mutable.value = TrackingState(generation, account, loading = true)
            refresh()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == state.value.generation) mutable.value = TrackingState(generation, failure = error.trackingFailure())
        }
    }

    fun refresh() {
        val current = token ?: return
        val generation = state.value.generation
        work?.cancel()
        work = scope.launch { load(generation, current) }
    }

    private suspend fun load(generation: Long, current: String) {
        if (generation != state.value.generation) return
        mutable.value = state.value.copy(loading = true, failure = null)
        try {
            val account = provider.account(current)
            if (generation != state.value.generation || current != token) return
            mutable.value = state.value.copy(account = account)
            val snapshot = provider.snapshot(current)
            if (generation == state.value.generation && current == token) {
                mutable.value = TrackingState(generation, account, contents = snapshot.contents.map { it.copy(sessionGeneration = generation) }, memberships = snapshot.memberships)
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == state.value.generation) mutable.value = state.value.copy(loading = false, contents = emptyList(), failure = error.trackingFailure(), memberships = null)
        }
    }

    fun disconnect() {
        val previous = token
        token = null
        work?.cancel()
        val generation = state.value.generation + 1
        mutable.value = TrackingState(generation)
        scope.launch {
            try { storage.withLock { if (generation == state.value.generation) store.write(null) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == state.value.generation) mutable.value = state.value.copy(failure = TrackingFailure.STORAGE)
            }
            if (previous != null) try { provider.revoke(previous) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == state.value.generation) mutable.value = state.value.copy(failure = TrackingFailure.NETWORK)
            }
        }
    }
}

internal fun Exception.trackingFailure(): TrackingFailure = when (this) {
    is TrackingException -> failure
    is BetaSeriesApiException -> if (code in setOf(1002, 2002, 2003, 2004)) TrackingFailure.ACCESS else TrackingFailure.PROTOCOL
    else -> TrackingFailure.NETWORK
}
