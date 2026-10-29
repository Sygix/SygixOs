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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class TrackingController(
    private val provider: WatchTrackingProvider,
    private val store: TrackingSessionStore,
    private val scope: CoroutineScope,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    internal val repository: TrackingRepository? = null,
    private val scheduler: TrackingWorkScheduler? = null,
    private val beforeRestore: suspend () -> Boolean = { false },
    private val invalidate: () -> Unit = {},
    private val wallClock: () -> Long = System::currentTimeMillis,
) {
    private val mutable = MutableStateFlow(TrackingState())
    val state = mutable.asStateFlow()
    private var token: String? = null
    private var work: Job? = null
    private val storage = Mutex()
    private var foreground = false
    private var observation: Job? = null
    private var publishedRevision = -1L
    internal var incarnation: String? = null
        private set

    init { observeRepository() }

    private fun observeRepository() {
        if (observation?.isActive == true) return
        if (repository != null) observation = scope.launch {
            try { repository.view.collect { publish(it) } }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
        }
    }

    private fun publish(view: TrackingRepositoryView) {
        val account = view.account ?: return
        if (account.incarnation != incarnation || token == null) return
        if (account.revision < publishedRevision) return
        publishedRevision = account.revision
        mutable.value = state.value.copy(account = TrackingAccount(account.accountId, account.name),
            contents = if (account.accessDenied) emptyList() else view.contents.map { it.copy(sessionGeneration = state.value.generation) },
            pendingCount = view.pending, reserveIncomplete = view.incomplete,
            pendingIdentities = view.pendingIdentities, durableRevision = account.revision,
            undoContexts = view.undoContexts,
            failure = account.failure?.let(TrackingFailure::valueOf) ?: state.value.failure?.takeIf { it == TrackingFailure.STORAGE })
    }

    internal suspend fun record(content: TrackingContent, action: TrackingAction, undoRemainingMillis: Long? = null): StoredTrackingIntent {
        val repository = repository ?: throw TrackingException(TrackingFailure.STORAGE)
        val incarnation = incarnation ?: throw TrackingException(TrackingFailure.ACCESS)
        val intent = try { repository.record(incarnation, content, action, undoRemainingMillis) }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: TrackingException) { throw error }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
        publish(repository.view.first())
        if (undoRemainingMillis == null) schedule()
        return intent
    }

    internal suspend fun undo(intent: StoredTrackingIntent, content: TrackingContent): Boolean {
        val undone = try { repository?.undo(intent, content) ?: false }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (error: TrackingException) { throw error }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
        repository?.let { publish(it.view.first()) }
        schedule()
        return undone
    }

    internal suspend fun updateUndoRemaining(intent: StoredTrackingIntent, remainingMillis: Long) {
        repository?.updateUndoRemaining(intent.incarnation, intent.key, intent.revision, remainingMillis)
    }

    internal suspend fun releaseUndo(intent: StoredTrackingIntent): Boolean {
        val repository = repository ?: return false
        val released = repository.releaseUndo(intent.incarnation, intent.key, intent.revision) ||
            repository.account()?.incarnation == intent.incarnation && repository.undoContext(intent.incarnation, intent.key, intent.revision) == null &&
            repository.pending(intent.incarnation).none { it.key == intent.key && (it.revision != intent.revision || it.undoHeld) }
        if (released) {
            publish(repository.view.first())
            schedule()
        }
        return released
    }

    internal fun releaseUndo() { incarnation?.let { current -> scope.launch {
        try { repository?.let { it.restoreUndoReferences(current); publish(it.view.first()) } }
        catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
    } } }

    internal fun schedule() {
        val current = incarnation ?: return
        try { scheduler?.enqueue(current) }
        catch (_: Exception) { mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
    }

    fun setForeground(enabled: Boolean) {
        if (foreground == enabled) return
        foreground = enabled
        if (enabled) schedule()
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
        observeRepository()
        if (token != null) { if (snapshotDue()) refresh(); return }
        if (work?.isActive == true) return
        val generation = state.value.generation
        work?.cancel()
        mutable.value = state.value.copy(sessionStatus = TrackingSessionStatus.RESTORING, loading = true, failure = null)
        work = scope.launch {
            try {
                if (beforeRestore()) { mutable.value = TrackingState(generation); return@launch }
                val saved = storage.withLock { store.read() }
                currentCoroutineContext().ensureActive()
                if (generation != state.value.generation) return@launch
                if (saved == null) { mutable.value = TrackingState(generation); return@launch }
                token = saved
                mutable.value = state.value.copy(sessionStatus = TrackingSessionStatus.SAVED)
                repository?.restore(saved)?.let { account ->
                    if (incarnation != account.incarnation) publishedRevision = -1L
                    incarnation = account.incarnation
                    publish(repository.view.first())
                    mutable.value = state.value.copy(snapshotAt = account.snapshotAt?.let { clock() - (wallClock() - it).coerceAtLeast(0) })
                    schedule()
                }
                if (state.value.failure == TrackingFailure.ACCESS || !snapshotDue()) mutable.value = state.value.copy(loading = false)
                else load(generation, saved)
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) {
                if (generation == state.value.generation) mutable.value = state.value.copy(loading = false, failure = TrackingFailure.STORAGE)
            }
        }
    }

    suspend fun connect(value: String, stillValid: () -> Boolean = { true }) = scope.async {
        val previousGeneration = state.value.generation
        try {
            val account = provider.validateSession(value)
            withContext(NonCancellable) {
                storage.withLock {
                    if (previousGeneration != state.value.generation || !stillValid()) { revoke(value); return@withLock }
                    beforeRestore()
                    store.write(value)
                    work?.cancel()
                    val previous = token
                    token = value
                    mutable.value = TrackingState(previousGeneration + 1, account, sessionStatus = TrackingSessionStatus.SAVED)
                    repository?.bind(value, account)?.let { saved ->
                        incarnation?.takeIf { it != saved.incarnation }?.let { scheduler?.cancel(it) }
                        if (incarnation != saved.incarnation) publishedRevision = -1L
                        incarnation = saved.incarnation
                        publish(repository.view.first())
                        schedule()
                    }
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

    fun retry() {
        observeRepository()
        schedule()
        if (token == null) restore() else refresh()
    }

    fun patch(content: TrackingContent) {
        if (content.sessionGeneration != state.value.generation || state.value.account == null || state.value.failure == TrackingFailure.ACCESS) return
        if (repository != null) {
            val current = incarnation ?: return
            scope.launch {
                try { repository.patch(current, content) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
            }
            return
        }
        val entry = content
        val entries = state.value.contents.filterNot { it.identity == entry.identity || it.parent == entry.parent && it.identity != entry.identity && entry.identity.type == TrackingType.SHOW }
            .map { if (it.parent == entry.parent) it.copy(membership = entry.membership, archived = entry.archived) else it }
        mutable.value = state.value.copy(contents = entries + entry)
    }

    fun markFailure(failure: TrackingFailure) {
        work?.cancel()
        mutable.value = state.value.copy(loading = false, contents = emptyList(), failure = failure)
        if (failure == TrackingFailure.ACCESS) incarnation?.let { current -> scope.launch {
            try { repository?.failure(current, failure) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
        } }
    }

    private suspend fun load(generation: Long, current: String) {
        mutable.value = state.value.copy(loading = true, failure = null)
        val previous = state.value.contents.associateBy { it.identity }
        try {
            val account = state.value.account ?: provider.account(current)
            currentCoroutineContext().ensureActive()
            if (generation != state.value.generation || current != token) return
            mutable.value = state.value.copy(account = account)
            if (repository != null && incarnation == null) {
                incarnation = repository.bind(current, account).incarnation
                publish(repository.view.first())
            }
            val startedRevision = repository?.account()?.revision ?: 0
            val snapshot = provider.snapshot(current)
            currentCoroutineContext().ensureActive()
            if (generation != state.value.generation || current != token) return
            if (repository != null) {
                incarnation?.let { repository.snapshot(it, startedRevision, snapshot) }
                mutable.value = state.value.copy(loading = false, snapshotAt = clock(), failure = snapshot.failure)
                publish(repository.view.first())
                schedule()
                return
            }
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
                try { incarnation?.let { repository?.failure(it, failure) } }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) { mutable.value = state.value.copy(loading = false, failure = TrackingFailure.STORAGE); return }
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
        val previousIncarnation = incarnation
        incarnation = null
        publishedRevision = -1L
        var invalidationFailed = false
        try { invalidate() } catch (_: Exception) { invalidationFailed = true }
        previousIncarnation?.let { scheduler?.cancel(it) }
        work?.cancel()
        val generation = state.value.generation + 1
        mutable.value = TrackingState(generation)
        if (invalidationFailed) mutable.value = state.value.copy(failure = TrackingFailure.STORAGE)
        scope.launch {
            withContext(NonCancellable) {
                try { storage.withLock { if (generation == state.value.generation) { repository?.purge(); store.write(null) } } }
                catch (_: Exception) { if (generation == state.value.generation) mutable.value = state.value.copy(failure = TrackingFailure.STORAGE) }
                if (previous != null) revoke(previous, generation)
            }
        }
    }
}

internal fun Exception.trackingFailure(): TrackingFailure = when (this) {
    is TrackingException -> failure
    is android.database.SQLException, is androidx.sqlite.SQLiteException -> TrackingFailure.STORAGE
    is org.json.JSONException -> TrackingFailure.PROTOCOL
    is BetaSeriesApiException -> classifyBetaSeriesRefusal(code)
    else -> TrackingFailure.NETWORK
}
