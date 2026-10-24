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
)

class TrackingActions(
    private val controller: TrackingController,
    private val mutate: suspend (String, TrackingContent, TrackingAction) -> Unit,
    private val scope: CoroutineScope,
    private val reconcile: suspend (String, TrackingContent, TrackingAction) -> Boolean = { _, _, _ -> false },
    private val clock: () -> Long = System::nanoTime,
) {
    private val mutable = MutableStateFlow<TrackingNotice?>(null)
    val notice = mutable.asStateFlow()
    private val excludedMutable = MutableStateFlow<Set<TrackingIdentity>>(emptySet())
    val excluded = excludedMutable.asStateFlow()
    private var work: Job? = null
    private var timer: Job? = null
    private var foreground = true
    private var focused = false
    private var settingsOpen = false
    private var pendingReturn: TrackingNotice? = null
    private var tickAt = 0L

    init {
        scope.launch {
            controller.state.collect { state ->
                if (mutable.value?.generation?.let { it != state.generation } == true) {
                    work?.cancel(); timer?.cancel(); mutable.value = null; pendingReturn = null; excludedMutable.value = emptySet()
                }
                if (mutable.value?.returnPrompt == true && state.contents.any { it.identity == mutable.value?.content?.identity && it.seen }) mutable.value = null
            }
        }
    }

    fun perform(content: TrackingContent, action: TrackingAction) {
        if (controller.state.value.account == null || content.sessionGeneration?.let { it != controller.state.value.generation } == true) return
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
            TrackingAction.REMOVE -> content.membership == AccountMembership.PRESENT
        }
        if (!applicable) return
        pendingReturn = null
        timer?.cancel()
        val notice = TrackingNotice(content.copy(sessionGeneration = controller.state.value.generation), action, controller.state.value.generation, busy = action != TrackingAction.REMOVE)
        mutable.value = notice
        if (action == TrackingAction.REMOVE) {
            excludedMutable.value = excludedMutable.value + content.parent
            startTimer()
        } else send(notice)
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
        } else send(current, inverse = true)
    }

    fun retry() {
        val current = mutable.value ?: return
        if (current.busy || current.failure == null) return
        send(current)
    }

    fun returned(content: TrackingContent, generation: Long) {
        if (generation != controller.state.value.generation || controller.state.value.account == null) return
        pendingReturn = TrackingNotice(content, TrackingAction.SEEN, generation, returnPrompt = true)
        val current = mutable.value
        if (current?.busy != true && (current == null || current.action != TrackingAction.REMOVE && current.failure == null)) finishNotice()
    }

    fun confirmReturn() { val current = mutable.value ?: return; if (current.returnPrompt) { mutable.value = null; perform(current.content, TrackingAction.SEEN) } }
    fun dismiss() { if (mutable.value?.returnPrompt == true || mutable.value?.failure != null) finishNotice() }
    fun dismissReturn() { pendingReturn = null; if (mutable.value?.returnPrompt == true) mutable.value = null }
    fun setForeground(value: Boolean) { accountTime(); foreground = value; startTimer() }
    fun setFocused(value: Boolean) { accountTime(); focused = value; startTimer() }
    fun setSettingsOpen(value: Boolean) { accountTime(); settingsOpen = value; startTimer() }

    private fun finishNotice() {
        timer?.cancel()
        mutable.value = pendingReturn?.takeIf { it.generation == controller.state.value.generation }
        pendingReturn = null
    }

    private fun accountTime() {
        val current = mutable.value ?: return
        if (timer?.isActive == true && foreground && !focused) {
            val elapsed = ((clock() - tickAt) / 1_000_000).coerceAtLeast(0)
            mutable.value = current.copy(remainingMillis = (current.remainingMillis - elapsed).coerceAtLeast(0))
        }
        timer?.cancel()
    }

    private fun startTimer() {
        timer?.cancel()
        val current = mutable.value ?: return
        if (!foreground || focused || settingsOpen || current.busy || current.failure != null || current.returnPrompt) return
        tickAt = clock()
        timer = scope.launch {
            delay(current.remainingMillis)
            if (mutable.value != current) return@launch
            if (current.action == TrackingAction.REMOVE) send(current) else finishNotice()
        }
    }
}
