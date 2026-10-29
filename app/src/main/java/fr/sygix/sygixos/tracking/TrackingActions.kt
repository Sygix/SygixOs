/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TrackingNotice(
    val content: TrackingContent,
    val action: TrackingAction,
    val generation: Long,
    val remainingMillis: Long = 10_000,
    val busy: Boolean = false,
    val failure: TrackingFailure? = null,
    val returnPrompt: Boolean = false,
    val undoing: Boolean = false,
    val blocked: Boolean = false,
    val locallySaved: Boolean = false,
    val intent: StoredTrackingIntent? = null,
    val confirmed: Boolean = false,
)

class TrackingActions(
    private val controller: TrackingController,
    private val mutate: suspend (String, TrackingContent, TrackingAction) -> Unit,
    private val scope: CoroutineScope,
    private val reconcile: suspend (String, TrackingContent, TrackingAction) -> Boolean = { _, _, _ -> false },
    initialForeground: Boolean = true,
    private val clock: () -> Long = System::nanoTime,
) {
    private val mutable = MutableStateFlow<TrackingNotice?>(null)
    val notice = mutable.asStateFlow()
    private val excludedMutable = MutableStateFlow<Set<TrackingIdentity>>(emptySet())
    val excluded = excludedMutable.asStateFlow()
    private var work: Job? = null
    private var timer: Job? = null
    private var foreground = initialForeground
    private var focused = false
    private var settingsOpen = false
    private var pendingReturn: TrackingNotice? = null
    private var tickAt = 0L
    private var persistence: Job? = null
    private var restoring: Job? = null

    init {
        scope.launch {
            controller.state.collect { state ->
                if (mutable.value?.generation?.let { it != state.generation } == true) {
                    work?.cancel(); timer?.cancel(); persistence?.cancel(); restoring?.cancel()
                    mutable.value = null; pendingReturn = null; excludedMutable.value = emptySet()
                }
                restoreNotice(state)
                if (mutable.value?.returnPrompt == true && state.contents.any { it.identity == mutable.value?.content?.identity && it.seen }) mutable.value = null
                confirmLocal(state)
            }
        }
    }

    fun perform(content: TrackingContent, action: TrackingAction) {
        if (controller.state.value.account == null || content.sessionGeneration?.let { it != controller.state.value.generation } == true) return
        if (mutable.value == null && controller.state.value.undoContexts.isNotEmpty()) { restoreNotice(controller.state.value); return }
        if (work?.isActive == true || mutable.value?.let { !it.returnPrompt } == true) {
            accountTime()
            mutable.value = mutable.value?.copy(blocked = true)
            startTimer()
            return
        }
        val applicable = when (action) {
            TrackingAction.ADD -> content.membership == AccountMembership.ABSENT
            TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE -> content.membership == AccountMembership.PRESENT && content.parent.type == TrackingType.SHOW && content.archived != null
            TrackingAction.SEEN, TrackingAction.UNSEEN, TrackingAction.RESTORE_MOVIE_STATE -> content.membership == AccountMembership.PRESENT && content.identity.type != TrackingType.SHOW
            TrackingAction.REMOVE -> content.membership == AccountMembership.PRESENT && content.removableFromAccount
        }
        if (!applicable) return
        pendingReturn = null
        timer?.cancel()
        val notice = TrackingNotice(content.copy(sessionGeneration = controller.state.value.generation), action, controller.state.value.generation, busy = action != TrackingAction.REMOVE)
        mutable.value = notice
        if (action == TrackingAction.REMOVE) {
            excludedMutable.value = excludedMutable.value + content.parent
            startTimer()
        } else if (controller.repository != null && action in setOf(TrackingAction.SEEN, TrackingAction.UNSEEN)) sendLocal(notice)
        else send(notice)
    }

    private fun sendLocal(notice: TrackingNotice, inverse: Boolean = false) {
        work = scope.launch {
            val operation = notice.copy(busy = true, failure = null, undoing = inverse || notice.undoing)
            mutable.value = operation
            try {
                if (operation.undoing) {
                    controller.undo(requireNotNull(operation.intent), operation.content)
                    if (controller.state.value.generation == operation.generation) finishNotice()
                } else {
                    val intent = controller.record(operation.content, operation.action, 10_000)
                    if (controller.state.value.generation != operation.generation) return@launch
                    mutable.value = operation.copy(busy = false, locallySaved = true, intent = intent)
                    confirmLocal(controller.state.value)
                    startTimer()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (controller.state.value.generation == operation.generation) mutable.value = operation.copy(busy = false, failure = error.trackingFailure())
            }
        }
    }

    private fun confirmLocal(state: TrackingState) {
        val notice = mutable.value?.takeIf { it.locallySaved && !it.confirmed && !it.busy } ?: return
        val intent = notice.intent ?: return
        if (state.durableRevision < intent.revision || notice.content.identity in state.pendingIdentities) return
        val target = state.contents.firstOrNull { it.identity == notice.content.identity } ?: return
        if (target.seen == (notice.action == TrackingAction.SEEN)) {
            accountTime()
            mutable.value = mutable.value?.copy(confirmed = true)
            startTimer()
        }
    }

    private fun restoreNotice(state: TrackingState) {
        if (state.undoContexts.isNotEmpty() && mutable.value?.returnPrompt == true) {
            pendingReturn = mutable.value
            mutable.value = null
        }
        if (mutable.value != null || state.account == null || restoring?.isActive == true) return
        val held = state.undoContexts.firstOrNull() ?: return
        val repository = controller.repository ?: return
        restoring = scope.launch {
            try {
                val pending = repository.pending(held.incarnation).firstOrNull { it.key == held.key && it.revision == held.revision }
                val saved = repository.undoContext(held.incarnation, held.key, held.revision)
                if (pending?.undoHeld == true && saved != null && mutable.value == null && controller.state.value.generation == state.generation) {
                    val action = if (pending.desiredState == 1) TrackingAction.SEEN else TrackingAction.UNSEEN
                    val previous = TrackingContentCodec.decode(saved.previousPayload).copy(sessionGeneration = state.generation)
                    mutable.value = TrackingNotice(previous, action, state.generation, saved.remainingMillis,
                        locallySaved = true, intent = pending)
                    startTimer()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { if (controller.state.value.generation == state.generation) controller.markFailure(TrackingFailure.STORAGE) }
        }
    }

    private fun send(notice: TrackingNotice, inverse: Boolean = false) {
        work = scope.launch {
            val operation = notice.copy(undoing = inverse || notice.undoing)
            mutable.value = operation.copy(busy = true, failure = null)
            try {
                controller.authenticated { token ->
                    val action = if (operation.undoing) inverse(operation) else operation.action
                    if (notice.failure == null || !reconcile(token, operation.content, action)) mutate(token, operation.content, action)
                }
                if (controller.state.value.generation != notice.generation) return@launch
                excludedMutable.value = excludedMutable.value - notice.content.parent
                if (operation.undoing || notice.action == TrackingAction.REMOVE || notice.action == TrackingAction.ADD || pendingReturn != null) finishNotice()
                else { mutable.value = operation.copy(busy = false, failure = null); startTimer() }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (controller.state.value.generation == notice.generation) {
                    excludedMutable.value = excludedMutable.value - notice.content.parent
                    mutable.value = operation.copy(busy = false, failure = error.trackingFailure())
                }
            }
        }
    }

    private fun inverse(notice: TrackingNotice): TrackingAction = when (notice.action) {
        TrackingAction.SEEN, TrackingAction.UNSEEN -> if (notice.content.identity.type == TrackingType.MOVIE) TrackingAction.RESTORE_MOVIE_STATE
            else if (notice.content.seen) TrackingAction.SEEN else TrackingAction.UNSEEN
        TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE -> if (notice.content.archived == true) TrackingAction.ARCHIVE else TrackingAction.UNARCHIVE
        else -> throw TrackingException(TrackingFailure.PROTOCOL)
    }

    fun undo() {
        val current = mutable.value ?: return
        if (current.busy || current.failure != null) return
        timer?.cancel()
        if (current.action == TrackingAction.REMOVE) {
            excludedMutable.value = excludedMutable.value - current.content.parent
            finishNotice()
        } else if (current.locallySaved) sendLocal(current, inverse = true)
        else send(current, inverse = true)
    }

    fun retry() {
        val current = mutable.value ?: return
        if (current.busy || current.failure == null) return
        if (current.locallySaved && current.intent != null) retryLocal(current)
        else if (controller.repository != null && current.action in setOf(TrackingAction.SEEN, TrackingAction.UNSEEN)) sendLocal(current)
        else send(current)
    }

    private fun retryLocal(notice: TrackingNotice) {
        val intent = notice.intent ?: return
        work = scope.launch {
            mutable.value = notice.copy(busy = true, failure = null)
            try {
                controller.updateUndoRemaining(intent, notice.remainingMillis)
                if (controller.state.value.generation != notice.generation) return@launch
                if (notice.remainingMillis == 0L) {
                    val released = controller.releaseUndo(intent)
                    if (controller.state.value.generation != notice.generation) return@launch
                    if (released) finishNotice()
                    else mutable.value = notice.copy(failure = TrackingFailure.STORAGE)
                } else {
                    mutable.value = notice.copy(busy = false, failure = null)
                    startTimer()
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (controller.state.value.generation == notice.generation) mutable.value = notice.copy(busy = false, failure = error.trackingFailure())
            }
        }
    }

    fun returned(content: TrackingContent, generation: Long) {
        if (generation != controller.state.value.generation || controller.state.value.account == null) return
        pendingReturn = TrackingNotice(content, TrackingAction.SEEN, generation, returnPrompt = true)
        val current = mutable.value
        if (current?.busy != true && (current == null || current.action != TrackingAction.REMOVE && !current.locallySaved && current.failure == null)) finishNotice()
    }

    fun confirmReturn() { val current = mutable.value ?: return; if (current.returnPrompt) { mutable.value = null; perform(current.content, TrackingAction.SEEN) } }
    fun dismiss() {
        val current = mutable.value ?: return
        if (current.returnPrompt || current.failure != null && !current.locallySaved) finishNotice()
    }
    fun dismissReturn() { pendingReturn = null; if (mutable.value?.returnPrompt == true) mutable.value = null }
    fun setForeground(value: Boolean) { accountTime(); foreground = value; startTimer() }
    fun setFocused(value: Boolean) { accountTime(); focused = value; startTimer() }
    fun setSettingsOpen(value: Boolean) { accountTime(); settingsOpen = value; startTimer() }

    private fun finishNotice() {
        timer?.cancel()
        controller.releaseUndo()
        mutable.value = null
        if (controller.state.value.undoContexts.isNotEmpty()) restoreNotice(controller.state.value)
        else {
            mutable.value = pendingReturn?.takeIf { it.generation == controller.state.value.generation }
            pendingReturn = null
        }
    }

    private fun accountTime() {
        val current = mutable.value ?: return
        if (timer?.isActive == true && foreground && !focused && !settingsOpen) {
            val elapsed = ((clock() - tickAt) / 1_000_000).coerceAtLeast(0)
            mutable.value = current.copy(remainingMillis = (current.remainingMillis - elapsed).coerceAtLeast(0))
            current.intent?.let { intent -> persistence = scope.launch {
                try { controller.updateUndoRemaining(intent, current.remainingMillis - elapsed) }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    if (mutable.value?.intent == intent && controller.state.value.generation == current.generation) {
                        timer?.cancel(); mutable.value = mutable.value?.copy(failure = TrackingFailure.STORAGE)
                    }
                }
            } }
        }
        timer?.cancel()
    }

    private fun startTimer() {
        timer?.cancel()
        val current = mutable.value ?: return
        if (!foreground || focused || settingsOpen || current.busy || current.failure != null || current.returnPrompt) return
        timer = scope.launch {
            persistence?.join()
            val waiting = mutable.value ?: return@launch
            if (waiting.generation != controller.state.value.generation || waiting.generation != current.generation || waiting.intent != current.intent || waiting.action != current.action ||
                waiting.busy || waiting.failure != null || !foreground || focused || settingsOpen) return@launch
            tickAt = clock()
            delay(waiting.remainingMillis)
            val latest = mutable.value ?: return@launch
            if (latest.generation != controller.state.value.generation || latest.generation != current.generation || latest.intent != current.intent || latest.action != current.action) return@launch
            if (latest.busy || latest.failure != null || !foreground || focused || settingsOpen) return@launch
            if (latest.action == TrackingAction.REMOVE) send(latest)
            else if (latest.locallySaved && latest.intent != null) {
                mutable.value = latest.copy(remainingMillis = 0, busy = true)
                try {
                    val released = controller.releaseUndo(latest.intent)
                    if (controller.state.value.generation != latest.generation) return@launch
                    if (released) finishNotice()
                    else if (controller.state.value.generation == latest.generation) mutable.value = latest.copy(remainingMillis = 0, failure = TrackingFailure.STORAGE)
                } catch (cancelled: CancellationException) { throw cancelled }
                catch (_: Exception) {
                    if (controller.state.value.generation == latest.generation) mutable.value = latest.copy(remainingMillis = 0, failure = TrackingFailure.STORAGE)
                }
            } else finishNotice()
        }
    }
}
