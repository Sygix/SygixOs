/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TrackingOfflineControllerTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "1"), title = "Example")
    private val store = object : TrackingSessionStore {
        override suspend fun read() = "fixture-session"
        override suspend fun write(token: String?) = Unit
    }
    private val offline = object : WatchTrackingProvider {
        override suspend fun account(token: String): TrackingAccount = throw TrackingException(TrackingFailure.NETWORK)
        override suspend fun snapshot(token: String): TrackingSnapshot = throw TrackingException(TrackingFailure.NETWORK)
        override suspend fun revoke(token: String) = Unit
    }
    private fun database() = Room.databaseBuilder(context, TrackingDatabase::class.java,
        context.noBackupFilesDir.resolve("${UUID.randomUUID()}.db").absolutePath).build()

    @Test fun savedAccountAndMovieCanBeMarkedAndUndoneWithoutAnyNetwork() = runTest {
        val db = database()
        val repository = TrackingRepository(db) { 1000L }
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
        var enqueued = 0
        val scheduler = object : TrackingWorkScheduler {
            override fun enqueue(incarnation: String) { enqueued++ }
            override fun cancel(incarnation: String) = Unit
        }
        val controller = TrackingController(offline, store, backgroundScope, repository = repository, scheduler = scheduler)
        controller.restore()
        val restored = withContext(Dispatchers.IO) { withTimeout(5000) { controller.state.first { it.account != null && !it.loading } } }
        assertEquals("A", restored.account?.id)
        assertEquals(movie.identity, restored.contents.single().identity)
        val intent = controller.record(restored.contents.single(), TrackingAction.SEEN)
        assertEquals(1, controller.state.value.pendingCount)
        assertTrue(TrackingProjection.candidates(controller.state.value.contents).isEmpty())
        controller.undo(intent, movie)
        assertEquals(0, controller.state.value.pendingCount)
        assertEquals(movie.identity, TrackingProjection.candidates(controller.state.value.contents).single().identity)
        assertTrue(enqueued >= 3)
        db.close()
    }

    @Test fun failedLocalWriteDoesNotScheduleOrChangeTheProjection() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        var enqueued = 0
        val controller = TrackingController(offline, store, backgroundScope, repository = repository,
            scheduler = object : TrackingWorkScheduler {
                override fun enqueue(incarnation: String) { enqueued++ }
                override fun cancel(incarnation: String) = Unit
            })
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
        controller.restore()
        withContext(Dispatchers.IO) { withTimeout(5000) { controller.state.first { it.account != null && !it.loading } } }
        val before = enqueued
        val contents = controller.state.value.contents
        db.close()
        try { controller.record(contents.single(), TrackingAction.SEEN); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.STORAGE, error.failure) }
        assertEquals(before, enqueued)
        assertEquals(contents, controller.state.value.contents)
    }

    @Test fun noticeOnlyReportsSyncedAfterAcknowledgementAndKeepsItsUndoDeadline() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
        val controller = TrackingController(offline, store, backgroundScope, repository = repository)
        controller.restore()
        val restored = controller.state.first { it.account != null && !it.loading }
        val actions = TrackingActions(controller, { _, _, _ -> fail("Durable actions must not use an unjournaled mutation") },
            backgroundScope, clock = { testScheduler.currentTime * 1_000_000 })
        actions.setSettingsOpen(true)
        actions.perform(restored.contents.single(), TrackingAction.SEEN)
        val saved = actions.notice.first { it?.locallySaved == true }!!
        assertFalse(saved.confirmed)
        repository.confirm(saved.intent!!, movie.copy(seen = true, movieState = 1))
        actions.notice.first { it?.confirmed == true }
        actions.setSettingsOpen(false)
        advanceTimeBy(10_000)
        runCurrent()
        assertNull(actions.notice.value)
        db.close()
    }
}
