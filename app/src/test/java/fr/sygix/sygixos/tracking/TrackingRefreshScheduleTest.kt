/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test

class TrackingRefreshScheduleTest {
    @Test fun foregroundUsesLazyTtlAndMutationOnlyPatchesItsContent() = runTest {
        var lists = 0
        var details = 0
        val content = TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "1"), title = "Fixture")
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String): TrackingSnapshot { ++lists; return TrackingSnapshot(listOf(content)) }
            override suspend fun revoke(token: String) {}
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) {}
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        assertEquals(1, lists)
        controller.setForeground(true); runCurrent()
        controller.setForeground(false); advanceTimeBy(60_000)
        controller.setForeground(true); runCurrent()
        assertEquals(1, lists)
        val actions = TrackingActions(controller, { _, entry, _ -> ++details; controller.patch(entry.copy(seen = true)) }, backgroundScope,
            clock = { testScheduler.currentTime * 1_000_000 })
        actions.perform(controller.state.value.contents.single(), TrackingAction.SEEN); runCurrent()
        assertEquals(1, details)
        assertEquals(1, lists)
        advanceTimeBy(20 * 60_000); runCurrent()
        assertEquals(1, lists)
        controller.setForeground(false); controller.setForeground(true); runCurrent()
        assertEquals(2, lists)
    }

    @Test fun transientStorageFailureRetriesOnNextForegroundWithoutDeletingSession() = runTest {
        var reads = 0
        var writes = 0
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList())
            override suspend fun revoke(token: String) {}
        }, object : TrackingSessionStore {
            override suspend fun read(): String { if (++reads == 1) throw TrackingException(TrackingFailure.STORAGE); return "fixture-token" }
            override suspend fun write(token: String?) { ++writes }
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        assertEquals(TrackingFailure.STORAGE, controller.state.value.failure)
        controller.setForeground(true); runCurrent()
        assertEquals(2, reads)
        assertEquals(0, writes)
        assertNotNull(controller.state.value.account)
    }
}
