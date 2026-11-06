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

    @Test fun uncertainSessionReassociationRetainsSameAccountIntentAndPurgesOnlyAfterDifferentAccountConfirmation() = runTest {
        for (sameAccount in listOf(true, false)) {
            val db = database()
            try {
                val repository = TrackingRepository(db)
                val account = repository.bind("fixture-session", TrackingAccount("A", "Fixture"))
                repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
                repository.record(account.incarnation, movie, TrackingAction.SEEN, 5000)
                repository.failure(account.incarnation, TrackingFailure.RECONNECT)
                val provider = object : WatchTrackingProvider {
                    override suspend fun account(token: String) = TrackingAccount(if (sameAccount) "A" else "B", "Fixture")
                    override suspend fun snapshot(token: String) = TrackingSnapshot(listOf(movie))
                    override suspend fun revoke(token: String) = Unit
                }
                val controller = TrackingController(provider, store, backgroundScope, repository = repository)
                controller.restore()
                withContext(Dispatchers.IO) { withTimeout(5000) { controller.state.first { it.account != null && !it.loading } } }
                assertEquals(TrackingFailure.RECONNECT, controller.state.value.failure)
                assertEquals(1, controller.state.value.pendingCount)
                controller.connect("new-session")
                val current = repository.account()!!
                assertEquals(if (sameAccount) "A" else "B", current.accountId)
                if (sameAccount) {
                    assertEquals(account.incarnation, current.incarnation)
                    assertTrue(repository.pending(current.incarnation).single().undoHeld)
                } else {
                    assertNotEquals(account.incarnation, current.incarnation)
                    assertTrue(repository.pending(account.incarnation).isEmpty())
                }
                assertNull(current.failure)
            } finally { db.close() }
        }
    }

    @Test fun retryAfterReleaseStorageFailureKeepsTheRevisionAndDoesNotRestartUndo() = runTest {
        val db = database()
        try {
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture-session", TrackingAccount("A", "Fixture"))
            repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
            val controller = TrackingController(offline, store, backgroundScope, repository = repository)
            controller.restore()
            val state = controller.state.first { it.account != null && !it.loading }
            val actions = TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope,
                clock = { testScheduler.currentTime * 1_000_000 })
            actions.setSettingsOpen(true)
            actions.perform(state.contents.single(), TrackingAction.SEEN)
            val saved = actions.notice.first { it?.locallySaved == true }!!
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER fail_undo_release BEFORE INSERT ON tracking_intent WHEN NEW.undoHeld = 0 BEGIN SELECT RAISE(ABORT, 'fixture-release'); END")
            actions.setSettingsOpen(false); runCurrent()
            advanceTimeBy(10_000); runCurrent()
            withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it?.failure == TrackingFailure.STORAGE } } }
            db.openHelper.writableDatabase.execSQL("DROP TRIGGER fail_undo_release")
            actions.retry(); runCurrent()
            withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it == null || !it.busy && it.failure == null } } }
            assertEquals(saved.intent!!.revision, repository.pending(account.incarnation).single().revision)
            assertNull(actions.notice.value)
            assertFalse(repository.pending(account.incarnation).single().undoHeld)
        } finally { db.close() }
    }

    @Test fun completionPromptWaitsForHeldUndoInsteadOfStrandingTheRequest() = runTest {
        val db = database()
        try {
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture-session", TrackingAccount("A", "Fixture"))
            repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
            val controller = TrackingController(offline, store, backgroundScope, repository = repository)
            controller.restore()
            val state = controller.state.first { it.account != null && !it.loading }
            val actions = TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope,
                clock = { testScheduler.currentTime * 1_000_000 })
            actions.setSettingsOpen(true)
            actions.perform(state.contents.single(), TrackingAction.SEEN)
            actions.notice.first { it?.locallySaved == true }
            actions.setSettingsOpen(false); runCurrent()
            advanceTimeBy(3_000)
            val other = movie.copy(identity = movie.identity.copy(id = "2"), parent = movie.parent.copy(id = "2"))
            actions.returned(other, state.generation); runCurrent()
            assertFalse(actions.notice.value?.returnPrompt == true)
            advanceTimeBy(7_000); runCurrent()
            val prompt = withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it?.returnPrompt == true } } }!!
            assertEquals(other.identity, prompt.content.identity)
            assertFalse(repository.pending(account.incarnation).single().undoHeld)
        } finally { db.close() }
    }

    @Test fun restoredUndoDoesNotSpendItsRemainingTimeBeforeForegroundActivation() = runTest {
        val db = database()
        try {
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture-session", TrackingAccount("A", "Fixture"))
            val held = repository.record(account.incarnation, movie, TrackingAction.SEEN, 7_000)
            val controller = TrackingController(offline, store, backgroundScope, repository = repository)
            controller.restore()
            controller.state.first { it.account != null && !it.loading }
            val actions = TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope,
                clock = { testScheduler.currentTime * 1_000_000 }, initialForeground = false)
            actions.notice.first { it?.locallySaved == true }
            advanceTimeBy(50_000); runCurrent()
            assertEquals(7_000L, actions.notice.value?.remainingMillis)
            assertFalse(repository.markSent(held))
            actions.setForeground(true); runCurrent()
            advanceTimeBy(7_000); runCurrent()
            withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it == null } } }
            assertFalse(repository.pending(account.incarnation).single().undoHeld)
        } finally { db.close() }
    }

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

    @Test fun addingAnIdentifiedShowSchedulesItsReserveFillInsteadOfStayingIncomplete() = runTest {
        val db = database()
        val repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(emptyList()))
        val scheduled = kotlinx.coroutines.channels.Channel<String>(kotlinx.coroutines.channels.Channel.UNLIMITED)
        val controller = TrackingController(offline, store, backgroundScope, repository = repository, scheduler = object : TrackingWorkScheduler {
            override fun enqueue(incarnation: String) { scheduled.trySend(incarnation) }
            override fun cancel(incarnation: String) = Unit
        })
        controller.restore()
        val restored = withContext(Dispatchers.IO) { withTimeout(5000) { controller.state.first { it.account != null && !it.loading } } }
        val show = TrackingIdentity("trakt", TrackingType.SHOW, "42")
        val identified = TrackingContent(show, title = "Example", membership = AccountMembership.ABSENT, sessionGeneration = restored.generation)
        controller.patch(identified)
        withContext(Dispatchers.IO) { withTimeout(5000) { controller.state.first { state -> state.contents.any { it.identity == show } } } }
        while (scheduled.tryReceive().isSuccess) continue
        controller.patch(identified.copy(membership = AccountMembership.PRESENT))
        assertEquals(account.incarnation, withContext(Dispatchers.IO) { withTimeout(5000) { scheduled.receive() } })
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
        assertFalse(repository.markSent(saved.intent!!))
        actions.setSettingsOpen(false)
        runCurrent()
        advanceTimeBy(10_000)
        runCurrent()
        withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it == null } } }
        assertNull(actions.notice.value)
        db.close()
    }

    @Test fun activeUndoPausePersistsRemainingTimeAndRestoresNoticeAfterDatabaseRecreation() = runTest {
        val name = "${UUID.randomUUID()}.db"
        var db = Room.databaseBuilder(context, TrackingDatabase::class.java, context.noBackupFilesDir.resolve(name).absolutePath)
            .addMigrations(TrackingDatabase.MIGRATION_1_2, TrackingDatabase.MIGRATION_2_3, TrackingDatabase.MIGRATION_3_4).build()
        var repository = TrackingRepository(db)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(movie)))
        var controller = TrackingController(offline, store, backgroundScope, repository = repository)
        controller.restore()
        val restored = controller.state.first { it.account != null && !it.loading }
        var actions = TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope,
            clock = { testScheduler.currentTime * 1_000_000 })
        actions.setSettingsOpen(true)
        actions.perform(restored.contents.single(), TrackingAction.SEEN)
        val saved = actions.notice.first { it?.locallySaved == true }!!
        actions.setSettingsOpen(false)
        runCurrent()
        advanceTimeBy(3_000)
        actions.setSettingsOpen(true)
        runCurrent()
        withContext(Dispatchers.IO) { withTimeout(5000) { controller.state.first { it.undoContexts.firstOrNull()?.remainingMillis == 7_000L } } }
        withContext(Dispatchers.IO) { withTimeout(5000) {
            assertEquals(7_000L, repository.undoContext(account.incarnation, saved.intent!!.key, saved.intent.revision)?.remainingMillis)
        } }
        db.close()

        db = Room.databaseBuilder(context, TrackingDatabase::class.java, context.noBackupFilesDir.resolve(name).absolutePath)
            .addMigrations(TrackingDatabase.MIGRATION_1_2, TrackingDatabase.MIGRATION_2_3, TrackingDatabase.MIGRATION_3_4).build()
        repository = TrackingRepository(db)
        controller = TrackingController(offline, store, backgroundScope, repository = repository)
        controller.restore()
        controller.state.first { it.account != null && it.undoContexts.isNotEmpty() }
        actions = TrackingActions(controller, { _, _, _ -> fail() }, backgroundScope,
            clock = { testScheduler.currentTime * 1_000_000 })
        actions.setSettingsOpen(true)
        runCurrent()
        val restoredNotice = withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it?.intent?.revision == saved.intent?.revision } } }!!
        assertEquals(7_000L, restoredNotice.remainingMillis)
        assertFalse(restoredNotice.confirmed)
        actions.undo()
        runCurrent()
        withContext(Dispatchers.IO) { withTimeout(5000) { actions.notice.first { it == null } } }
        assertTrue(repository.pending(account.incarnation).isEmpty())
        db.close()
    }
}
