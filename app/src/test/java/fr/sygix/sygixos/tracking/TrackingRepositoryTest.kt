/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
class TrackingRepositoryTest {
    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
    private val show = TrackingIdentity("betaseries", TrackingType.SHOW, "show")
    private fun episode(number: Int) = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "e$number"), show,
        "Example", season = if (number <= 3) 1 else 2, episode = if (number <= 3) number else number - 3, airDate = "2020-01-01")
    private fun database(name: String) = Room.databaseBuilder(context, TrackingDatabase::class.java,
        context.noBackupFilesDir.resolve(name).absolutePath).build()

    @Test fun offlineRestartRetainsSixIndividualIntentsAndAccount() = runTest {
        val name = "${UUID.randomUUID()}.db"
        var db = database(name)
        var repository = TrackingRepository(db) { 1000L }
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot((1..6).map(::episode)))
        db.close()
        db = database(name)
        repository = TrackingRepository(db) { 2000L }
        for (number in 1..6) {
            repository.record(account.incarnation, episode(number), TrackingAction.SEEN)
            db.close()
            db = database(name)
            repository = TrackingRepository(db) { 2000L }
            val projected = repository.view.first { it.account != null }
            assertEquals(number, projected.pending)
            assertEquals(if (number < 6) episode(number + 1).identity else null,
                TrackingProjection.candidates(projected.contents).firstOrNull()?.identity)
        }
        assertEquals("A", repository.restore("session")?.accountId)
        assertEquals(6, repository.pending(account.incarnation).size)
        val effective = repository.view.first { it.account != null }
        assertEquals(6, effective.contents.count { it.seen })
        assertTrue(TrackingProjection.candidates(effective.contents).isEmpty())
        db.close()
    }

    @Test fun oldConfirmationAndRefreshCannotEraseNewIntent() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db) { 1000L }
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(episode(1))))
        val first = repository.record(account.incarnation, episode(1), TrackingAction.SEEN)
        repository.markSent(first)
        val last = repository.record(account.incarnation, episode(1).copy(seen = true), TrackingAction.UNSEEN)
        repository.confirm(first, episode(1).copy(seen = true))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(episode(1).copy(seen = true))))
        assertEquals(last.revision, repository.pending(account.incarnation).single().revision)
        assertFalse(repository.view.first { it.pending == 1 }.contents.single().seen)
        db.close()
    }

    @Test fun undoBeforeSendDropsIntentButUndoAfterSendRecordsExactMovieState() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db) { 1000L }
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "movie"), title = "Example", movieState = 2)
        val first = repository.record(account.incarnation, movie, TrackingAction.UNSEEN)
        repository.undo(first, movie)
        assertTrue(repository.pending(account.incarnation).isEmpty())
        val second = repository.record(account.incarnation, movie, TrackingAction.SEEN)
        repository.markSent(second)
        repository.undo(second, movie)
        assertEquals(2, repository.pending(account.incarnation).single().desiredState)
        db.close()
    }

    @Test fun differentTokenCannotRestoreOldCacheAndDifferentAccountPurgesIntents() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db) { 1000L }
        val first = repository.bind("one", TrackingAccount("A", "Example"))
        repository.record(first.incarnation, episode(1), TrackingAction.SEEN)
        assertNull(repository.restore("two"))
        val second = repository.bind("two", TrackingAccount("B", "Example"))
        repository.confirm(repository.pending(first.incarnation).firstOrNull() ?: StoredTrackingIntent(first.incarnation, "e1", 1, 1, 0), episode(1))
        assertTrue(repository.pending(second.incarnation).isEmpty())
        assertTrue(db.tracking().contents(second.incarnation).isEmpty())
        db.close()
    }

    @Test fun cleanupKeepsUndoAndLocalProofThenDeletesReleasedConsumedRows() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db) { 1000L }
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot((1..6).map(::episode)))
        val intent = repository.record(account.incarnation, episode(1), TrackingAction.SEEN)
        repository.references(account.incarnation, "local", setOf(episode(1).identity))
        repository.confirm(intent, episode(1).copy(seen = true))
        assertTrue(db.tracking().contents(account.incarnation).any { it.key == episode(1).identity.storageKey })
        repository.references(account.incarnation, "undo", emptySet())
        assertTrue(db.tracking().contents(account.incarnation).any { it.key == episode(1).identity.storageKey })
        repository.references(account.incarnation, "local", emptySet())
        assertFalse(db.tracking().contents(account.incarnation).any { it.key == episode(1).identity.storageKey })
        assertEquals(5, db.tracking().contents(account.incarnation).size)
        db.close()
    }

    @Test fun unsupportedSchemaUpgradeFailsWithoutDestroyingPendingIntents() = runTest {
        val name = "${UUID.randomUUID()}.db"
        var db = database(name)
        val repository = TrackingRepository(db)
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        repository.record(account.incarnation, episode(1), TrackingAction.SEEN)
        db.openHelper.writableDatabase.version = 99
        db.close()
        db = database(name)
        try { db.tracking().account(); fail() } catch (_: IllegalStateException) { }
        db.close()
        val raw = android.database.sqlite.SQLiteDatabase.openDatabase(context.noBackupFilesDir.resolve(name).absolutePath, null,
            android.database.sqlite.SQLiteDatabase.OPEN_READONLY)
        raw.rawQuery("SELECT COUNT(*) FROM tracking_intent", null).use { rows -> assertTrue(rows.moveToFirst()); assertEquals(1, rows.getInt(0)) }
        raw.close()
    }

    @Test fun confirmedMovieIsDeletedOnlyAfterUndoAndLocalReferencesAreReleased() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db)
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "movie"), title = "Example")
        val intent = repository.record(account.incarnation, movie, TrackingAction.SEEN)
        repository.references(account.incarnation, "local", setOf(movie.identity))
        repository.confirm(intent, movie.copy(seen = true, movieState = 1))
        repository.references(account.incarnation, "undo", emptySet())
        assertEquals(1, db.tracking().contents(account.incarnation).size)
        repository.references(account.incarnation, "local", emptySet())
        assertTrue(db.tracking().contents(account.incarnation).isEmpty())
        assertTrue(repository.reserves(account.incarnation).isEmpty())
        db.close()
    }

    @Test fun parentArchiveRemovalAndUndoApplyToTheEntireSixEpisodeReserve() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db)
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot((1..6).map { episode(it).copy(archived = false) }))
        repository.record(account.incarnation, episode(6), TrackingAction.SEEN)
        repository.patch(account.incarnation, episode(1).copy(archived = true))
        assertTrue(TrackingProjection.candidates(repository.view.first { it.account != null }.contents).isEmpty())
        assertEquals(1, repository.pending(account.incarnation).size)
        repository.patch(account.incarnation, episode(1).copy(archived = false))
        assertEquals(episode(1).identity, TrackingProjection.candidates(repository.view.first { it.account != null }.contents).single().identity)
        repository.patch(account.incarnation, episode(1).copy(membership = AccountMembership.ABSENT))
        assertTrue(TrackingProjection.candidates(repository.view.first { it.account != null }.contents).isEmpty())
        assertEquals(1, repository.pending(account.incarnation).size)
        db.close()
    }

    @Test fun oldSnapshotCannotClearARefillRequestedByAnOverlappingLocalIntent() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db)
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        val snapshot = TrackingSnapshot((1..6).map(::episode))
        repository.snapshot(account.incarnation, 0, snapshot)
        val started = repository.account()!!.revision
        val intent = repository.record(account.incarnation, episode(1), TrackingAction.SEEN)
        repository.snapshot(account.incarnation, started, snapshot)
        repository.confirm(intent, episode(1).copy(seen = true))
        assertEquals(1, repository.reserves(account.incarnation).size)
        assertTrue(repository.view.first { it.account != null }.incomplete)
        repository.fill(account.incarnation, show, (2..7).map(::episode), true, repository.account()!!.revision)
        assertTrue(repository.reserves(account.incarnation).isEmpty())
        assertEquals(6, repository.contents(account.incarnation).count { !it.seen })
        db.close()
    }

    @Test fun completeAbsenceRemovesObsoleteReserveWithoutUsingPartialAbsenceAsProof() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db)
        val account = repository.bind("session", TrackingAccount("A", "Example"))
        repository.snapshot(account.incarnation, 0, TrackingSnapshot((1..6).map(::episode), TrackingFailure.INCOMPLETE))
        repository.snapshot(account.incarnation, repository.account()!!.revision, TrackingSnapshot(emptyList(), TrackingFailure.INCOMPLETE))
        assertEquals(1, repository.reserves(account.incarnation).size)
        repository.snapshot(account.incarnation, repository.account()!!.revision, TrackingSnapshot(emptyList()))
        assertTrue(repository.contents(account.incarnation).isEmpty())
        assertTrue(repository.reserves(account.incarnation).isEmpty())
        var calls = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { calls++; error("Empty work must not call the network") })) {
            java.time.LocalDate.of(2026, 1, 1)
        }
        val session = object : TrackingSessionStore {
            override suspend fun read() = "session"
            override suspend fun write(token: String?) = Unit
        }
        assertTrue(TrackingSyncCoordinator(repository, provider, session).drain(account.incarnation))
        assertEquals(0, calls)
        db.close()
    }

    @Test fun migrationFromVersionOnePreservesAccountIntentsStatusesDatesAndReferences() = runTest {
        val name = "${UUID.randomUUID()}.db"
        val schema = context.assets.open("fr.sygix.sygixos.tracking.TrackingDatabase/1.json").bufferedReader().use {
            org.json.JSONObject(it.readText()).getJSONObject("database")
        }
        val path = context.noBackupFilesDir.resolve(name).absolutePath
        val raw = android.database.sqlite.SQLiteDatabase.openOrCreateDatabase(path, null)
        val entities = schema.getJSONArray("entities")
        for (index in 0 until entities.length()) {
            val entity = entities.getJSONObject(index)
            raw.execSQL(entity.getString("createSql").replace("\u0024{TABLE_NAME}", entity.getString("tableName")))
        }
        val setup = schema.getJSONArray("setupQueries")
        for (index in 0 until setup.length()) raw.execSQL(setup.getString(index))
        raw.execSQL("INSERT INTO tracking_account VALUES(1, 'A', 'Example', 'incarnation', 'hash', 7, 1234, NULL, 0, 0)")
        raw.execSQL("INSERT INTO tracking_content VALUES(?, ?, ?, ?, ?)", arrayOf<Any>("incarnation", episode(1).identity.storageKey,
            show.storageKey, TrackingContentCodec.encode(episode(1).copy(seen = true)), 7))
        raw.execSQL("INSERT INTO tracking_intent VALUES(?, ?, 7, 0, 1, 1)", arrayOf("incarnation", episode(1).identity.storageKey))
        raw.execSQL("INSERT INTO tracking_reference VALUES(?, ?, 'local')", arrayOf("incarnation", episode(1).identity.storageKey))
        raw.execSQL("INSERT INTO tracking_reserve VALUES(?, ?, 0, 1)", arrayOf("incarnation", show.storageKey))
        raw.version = 1
        raw.close()
        val migrated = Room.databaseBuilder(context, TrackingDatabase::class.java, path).addMigrations(TrackingDatabase.MIGRATION_1_2).build()
        assertEquals("A", migrated.tracking().account()?.accountId)
        assertEquals(1234L, migrated.tracking().account()?.snapshotAt)
        assertTrue(migrated.tracking().intents("incarnation").single().possiblySent)
        assertEquals(7L, migrated.tracking().intents("incarnation").single().revision)
        val content = TrackingContentCodec.decode(migrated.tracking().contents("incarnation").single().payload)
        assertTrue(content.seen)
        assertEquals("2020-01-01", content.airDate)
        assertEquals("local", migrated.tracking().references("incarnation").single().owner)
        assertTrue(migrated.tracking().reserves("incarnation").single().needsFill)
        assertEquals(0L, migrated.tracking().reserves("incarnation").single().requestedRevision)
        migrated.close()
    }

    @Test fun oldBindingCannotRecordAccessFailureOrBackoffAfterSameAccountRenewal() = runTest {
        val db = database("${UUID.randomUUID()}.db")
        val repository = TrackingRepository(db)
        val first = repository.bind("old-session", TrackingAccount("A", "Example"))
        repository.record(first.incarnation, episode(1), TrackingAction.SEEN)
        val renewed = repository.bind("new-session", TrackingAccount("A", "Example"))
        assertEquals(first.incarnation, renewed.incarnation)
        repository.failure(first.incarnation, TrackingFailure.ACCESS, "old-session")
        repository.defer(first.incarnation, 90_000L, "old-session")
        assertFalse(repository.account()!!.accessDenied)
        assertNull(repository.account()!!.failure)
        assertEquals(0L, repository.account()!!.retryAt)
        assertEquals(1, repository.pending(renewed.incarnation).size)
        repository.failure(renewed.incarnation, TrackingFailure.ACCESS, "new-session")
        assertTrue(repository.account()!!.accessDenied)
        db.close()
    }
}
