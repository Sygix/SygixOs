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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.delay
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
    private var loads = 0L
    private var foreground = false
    private var periodic: Job? = null

    fun setForeground(enabled: Boolean) {
        if (foreground == enabled) return
        foreground = enabled
        if (!enabled) { periodic?.cancel(); periodic = null; return }
        if (work?.isActive != true && state.value.failure != TrackingFailure.ACCESS) refresh()
        startPeriodic()
    }

    private fun startPeriodic() {
        if (!foreground || token == null || state.value.failure == TrackingFailure.ACCESS || periodic?.isActive == true) return
        periodic = scope.launch {
            while (true) {
                delay(15 * 60_000L)
                if (!foreground || token == null) return@launch
                if (work?.isActive != true && state.value.failure != TrackingFailure.ACCESS) refresh()
            }
        }
    }

    suspend fun <T> authenticated(block: suspend (String) -> T): T {
        val current = token ?: throw TrackingException(TrackingFailure.ACCESS)
        val generation = state.value.generation
        if (state.value.failure == TrackingFailure.ACCESS) throw TrackingException(TrackingFailure.ACCESS)
        try {
            val result = block(current)
            if (generation != state.value.generation || token != current || state.value.failure == TrackingFailure.ACCESS) throw TrackingException(TrackingFailure.ACCESS)
            return result
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == state.value.generation && token == current && error.trackingFailure() == TrackingFailure.ACCESS) markFailure(TrackingFailure.ACCESS)
            throw error
        }
    }

    fun restore() {
        val generation = state.value.generation
        val request = ++loads
        work?.cancel()
        work = scope.launch {
            try {
                val saved = storage.withLock { store.read() }
                if (generation != state.value.generation || request != loads || saved == null) return@launch
                token = saved
                startPeriodic()
                load(generation, saved, request)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == state.value.generation && request == loads) mutable.value = TrackingState(generation, failure = TrackingFailure.STORAGE, revision = request)
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
            startPeriodic()
            refresh()
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == state.value.generation) mutable.value = TrackingState(generation, failure = error.trackingFailure())
        }
    }

    fun refresh(): Long? {
        val current = token ?: return null
        if (state.value.failure == TrackingFailure.ACCESS) return null
        val generation = state.value.generation
        val request = ++loads
        work?.cancel()
        work = scope.launch { load(generation, current, request) }
        return request
    }

    suspend fun reconcile(): TrackingState {
        val generation = state.value.generation
        val request = refresh() ?: throw TrackingException(TrackingFailure.ACCESS)
        val refreshed = withTimeoutOrNull(120_000) { state.first { it.generation != generation || !it.loading && it.revision >= request } }
            ?: throw TrackingException(TrackingFailure.NETWORK)
        if (refreshed.generation != generation || refreshed.account == null) throw TrackingException(TrackingFailure.ACCESS)
        refreshed.failure?.let { throw TrackingException(it) }
        return refreshed
    }

    fun markFailure(failure: TrackingFailure) {
        val revision = ++loads
        work?.cancel()
        if (failure == TrackingFailure.ACCESS) { periodic?.cancel(); periodic = null }
        mutable.value = state.value.copy(loading = false, contents = emptyList(), memberships = null, failure = failure, revision = revision)
    }

    private suspend fun load(generation: Long, current: String, request: Long) {
        if (generation != state.value.generation || request != loads) return
        mutable.value = state.value.copy(loading = true, failure = null)
        try {
            val account = provider.account(current)
            if (generation != state.value.generation || current != token || request != loads) return
            mutable.value = state.value.copy(account = account)
            val snapshot = provider.snapshot(current)
            if (generation == state.value.generation && current == token && request == loads) {
                mutable.value = TrackingState(generation, account, contents = snapshot.contents.map { it.copy(sessionGeneration = generation) }, memberships = snapshot.memberships, revision = request)
                startPeriodic()
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == state.value.generation && request == loads) {
                mutable.value = state.value.copy(loading = false, contents = emptyList(), failure = error.trackingFailure(), memberships = null, revision = request)
                if (state.value.failure == TrackingFailure.ACCESS) { periodic?.cancel(); periodic = null }
            }
        }
    }

    fun disconnect() {
        val previous = token
        token = null
        work?.cancel()
        periodic?.cancel(); periodic = null
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
