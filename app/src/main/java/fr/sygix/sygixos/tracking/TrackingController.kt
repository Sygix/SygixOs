/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TrackingController(
    private val provider: WatchTrackingProvider,
    private val store: TrackingSessionStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutable = MutableStateFlow(TrackingState())
    val state = mutable.asStateFlow()
    private var token: String? = null
    private var work: Job? = null
    private val storage = Mutex()
    private var foreground = false

    fun setForeground(enabled: Boolean) {
        if (foreground == enabled) return
        foreground = enabled
        if (!enabled || work?.isActive == true || state.value.failure == TrackingFailure.ACCESS) return
        if (token == null && state.value.failure == TrackingFailure.STORAGE) restore()
        else if (snapshotDue()) refresh()
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
        if (token != null) { if (snapshotDue()) refresh(); return }
        if (work?.isActive == true) return
        val generation = state.value.generation
        work?.cancel()
        work = scope.launch {
            try {
                val saved = storage.withLock { store.read() } ?: return@launch
                currentCoroutineContext().ensureActive()
                if (generation != state.value.generation) return@launch
                token = saved
                load(generation, saved)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == state.value.generation) mutable.value = state.value.copy(loading = false, failure = TrackingFailure.STORAGE)
            }
        }
    }

    suspend fun connect(value: String, stillValid: () -> Boolean = { true }) = scope.async {
        val previousGeneration = state.value.generation
        try {
            val account = provider.account(value)
            withContext(NonCancellable) {
                storage.withLock {
                    if (previousGeneration != state.value.generation || !stillValid()) { revoke(value); return@withLock }
                    store.write(value)
                    work?.cancel()
                    val previous = token
                    token = value
                    mutable.value = TrackingState(previousGeneration + 1, account)
                    refresh()
                    if (previous != null && previous != value) revoke(previous, state.value.generation)
                }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            withContext(NonCancellable) { revoke(value) }
            if (previousGeneration == state.value.generation && state.value.account == null) mutable.value = state.value.copy(failure = error.trackingFailure())
        }
    }.await()

    fun refresh() {
        val current = token ?: return
        if (state.value.failure == TrackingFailure.ACCESS || work?.isActive == true) return
        val generation = state.value.generation
        work = scope.launch { load(generation, current) }
    }

    fun patch(content: TrackingContent) {
        if (content.sessionGeneration != state.value.generation || state.value.account == null || state.value.failure == TrackingFailure.ACCESS) return
        val entry = content
        val entries = state.value.contents.filterNot { it.identity == entry.identity || it.parent == entry.parent && it.identity != entry.identity && entry.identity.type == TrackingType.SHOW }
            .map { if (it.parent == entry.parent) it.copy(membership = entry.membership, archived = entry.archived) else it }
        mutable.value = state.value.copy(contents = entries + entry)
    }

    fun markFailure(failure: TrackingFailure) {
        work?.cancel()
        mutable.value = state.value.copy(loading = false, contents = emptyList(), failure = failure)
    }

    private suspend fun load(generation: Long, current: String) {
        mutable.value = state.value.copy(loading = true, failure = null)
        val previous = state.value.contents.associateBy { it.identity }
        try {
            val account = state.value.account ?: provider.account(current)
            currentCoroutineContext().ensureActive()
            if (generation != state.value.generation || current != token) return
            mutable.value = state.value.copy(account = account)
            val snapshot = provider.snapshot(current)
            currentCoroutineContext().ensureActive()
            if (generation != state.value.generation || current != token) return
            val patched = state.value.contents.filter { previous[it.identity] !== it }
            val now = clock()
            val retained = if (snapshot.failure == null) emptyList() else state.value.contents.filter { old -> snapshot.contents.none { it.parent == old.parent } && patched.none { it.parent == old.parent } }
            mutable.value = state.value.copy(account = account, loading = false, snapshotAt = now, failure = snapshot.failure,
                contents = snapshot.contents.filterNot { entry -> patched.any { it.parent == entry.parent } }.map {
                    it.copy(sessionGeneration = generation)
                } + patched + retained)
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (generation == state.value.generation) {
                val failure = error.trackingFailure()
                mutable.value = state.value.copy(loading = false, contents = if (failure == TrackingFailure.ACCESS) emptyList() else state.value.contents,
                    failure = failure, snapshotAt = clock())
            }
        }
    }

    private fun snapshotDue() = state.value.snapshotAt?.let {
        val failure = state.value.failure
        clock() - it >= if (failure != null && failure != TrackingFailure.ACCESS) 60_000 else 15 * 60_000
    } != false

    private suspend fun revoke(value: String, reportGeneration: Long? = null) {
        try { provider.revoke(value) }
        catch (_: Exception) { if (reportGeneration == state.value.generation) mutable.value = state.value.copy(revocationUnconfirmed = true) }
    }

    fun disconnect() {
        val previous = token
        token = null
        work?.cancel()
        val generation = state.value.generation + 1
        mutable.value = TrackingState(generation)
        scope.launch {
            withContext(NonCancellable) {
                try { storage.withLock { if (generation == state.value.generation) store.write(null) } }
                catch (_: Exception) { if (generation == state.value.generation) mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
                if (previous != null) revoke(previous, generation)
            }
        }
    }
}

internal fun Exception.trackingFailure(): TrackingFailure = when (this) {
    is TrackingException -> failure
    is org.json.JSONException -> TrackingFailure.PROTOCOL
    is BetaSeriesApiException -> when (code) {
        1001 -> TrackingFailure.CONFIGURATION
        1002, 2002, 2003, 2004 -> TrackingFailure.ACCESS
        else -> TrackingFailure.PROTOCOL
    }
    else -> TrackingFailure.NETWORK
}
