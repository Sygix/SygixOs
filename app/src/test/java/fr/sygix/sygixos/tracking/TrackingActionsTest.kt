/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.*
import org.junit.Test

class TrackingActionsTest {
    private val content = TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "42"), title = "Fixture")

    @Test fun removalWaitsTenActiveSecondsAndFocusPreservesRemainingTime() = runTest {
        val controller = controller(backgroundScope)
        controller.restore(); runCurrent()
        val sent = mutableListOf<TrackingAction>()
        val actions = TrackingActions(controller, { _, _, action -> sent += action }, backgroundScope) { testScheduler.currentTime * 1_000_000 }
        actions.perform(content, TrackingAction.REMOVE)
        runCurrent()
        advanceTimeBy(3000)
        actions.setFocused(true)
        advanceTimeBy(20_000)
        assertTrue(sent.isEmpty())
        assertEquals(7000L, actions.notice.value?.remainingMillis)
        actions.setFocused(false)
        advanceTimeBy(6999); runCurrent()
        assertTrue(sent.isEmpty())
        advanceTimeBy(1); runCurrent()
        assertEquals(listOf(TrackingAction.REMOVE), sent)
    }

    @Test fun undoBeforeDeadlineAndDisconnectNeverDeleteRemotely() = runTest {
        val controller = controller(backgroundScope)
        controller.restore(); runCurrent()
        var sent = 0
        val actions = TrackingActions(controller, { _, _, _ -> ++sent }, backgroundScope) { testScheduler.currentTime * 1_000_000 }
        actions.perform(content, TrackingAction.REMOVE)
        actions.undo()
        advanceTimeBy(15_000); runCurrent()
        assertEquals(0, sent)
        assertTrue(actions.excluded.value.isEmpty())
        actions.perform(content, TrackingAction.REMOVE)
        runCurrent()
        controller.disconnect(); runCurrent()
        advanceTimeBy(15_000); runCurrent()
        assertEquals(0, sent)
        assertNull(actions.notice.value)
    }

    private fun controller(scope: kotlinx.coroutines.CoroutineScope) = TrackingController(object : WatchTrackingProvider {
        override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
        override suspend fun snapshot(token: String) = TrackingSnapshot(listOf(content))
        override suspend fun revoke(token: String) {}
    }, object : TrackingSessionStore {
        override suspend fun read() = "fixture-token"
        override suspend fun write(token: String?) {}
    }, scope)
}
