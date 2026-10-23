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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
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

    @Test fun confirmedRemovalRemainsExcludedUntilFreshSnapshotConfirmsAbsence() = runTest {
        val reconciled = kotlinx.coroutines.CompletableDeferred<TrackingSnapshot>()
        var removing = false
        var writes = 0
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String) = if (removing) reconciled.await() else TrackingSnapshot(listOf(content))
            override suspend fun revoke(token: String) {}
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) {}
        }, backgroundScope)
        controller.restore(); runCurrent()
        val actions = TrackingActions(controller, { _, _, _ -> ++writes; removing = true }, backgroundScope) { testScheduler.currentTime * 1_000_000 }
        actions.perform(content, TrackingAction.REMOVE)
        runCurrent(); advanceTimeBy(10_000); runCurrent()
        assertEquals(1, writes)
        assertTrue(content.parent in actions.excluded.value)
        assertTrue(actions.notice.value?.busy == true)
        reconciled.complete(TrackingSnapshot(emptyList())); runCurrent()
        assertTrue(actions.excluded.value.isEmpty())
        assertNull(actions.notice.value)
        assertTrue(controller.state.value.contents.isEmpty())
    }

    @Test fun movieUndoRestoresStateTwoAndConcurrentDuplicateActionIsNotSent() = runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.state = 2
        fixture.runtime.controller.restore(); runCurrent()
        val requested = content.copy(identity = TrackingIdentity("betaseries", TrackingType.MOVIE, "42"), parent = TrackingIdentity("betaseries", TrackingType.MOVIE, "42"), movieState = 2,
            sessionGeneration = fixture.runtime.controller.state.value.generation)
        fixture.runtime.actions.perform(requested, TrackingAction.SEEN)
        fixture.runtime.actions.perform(requested, TrackingAction.SEEN)
        runCurrent()
        assertEquals(1, fixture.state)
        assertEquals(1, fixture.writes)
        fixture.runtime.actions.undo(); runCurrent()
        assertEquals(2, fixture.state)
        assertEquals(2, fixture.writes)
        assertEquals(0, fixture.removals)
        assertNull(fixture.runtime.actions.notice.value)
    }
}
