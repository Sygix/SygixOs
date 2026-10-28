/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TrackingAccountLifecycleTest {
    @Test fun interruptedPurgeSurvivesRecreationAndCannotRestoreOldAccountOrQueue() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.databaseBuilder(context, TrackingDatabase::class.java,
            context.noBackupFilesDir.resolve("${UUID.randomUUID()}.db").absolutePath).build()
        val repository = TrackingRepository(database)
        val account = repository.bind("fixture-session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "1"), title = "Example"), TrackingAction.SEEN)
        var token: String? = "fixture-session"
        var failClear = true
        val session = object : TrackingSessionStore {
            override suspend fun read() = token
            override suspend fun write(value: String?) {
                if (failClear) throw TrackingException(TrackingFailure.STORAGE)
                token = value
            }
        }
        var cancellations = 0
        val scheduler = object : TrackingWorkScheduler {
            override fun enqueue(incarnation: String) = Unit
            override fun cancel(incarnation: String) { cancellations++ }
        }
        val file = context.noBackupFilesDir.resolve("${UUID.randomUUID()}.purge")
        val first = TrackingAccountLifecycle(repository, session, scheduler, FileTrackingPurgeMarker(file))
        first.invalidate()
        assertFalse(first.allowed)
        try { first.recoverPurge(); fail() } catch (error: TrackingException) { assertEquals(TrackingFailure.STORAGE, error.failure) }
        assertTrue(file.exists())
        assertNull(repository.account())
        val recreated = TrackingAccountLifecycle(repository, session, scheduler, FileTrackingPurgeMarker(file))
        assertFalse(recreated.allowed)
        failClear = false
        assertTrue(recreated.recoverPurge())
        assertTrue(recreated.allowed)
        assertFalse(file.exists())
        assertNull(session.read())
        val renewed = repository.bind("new-session", TrackingAccount("A", "Example"))
        assertTrue(repository.pending(renewed.incarnation).isEmpty())
        assertEquals(1, cancellations)
        database.close()
    }
}
