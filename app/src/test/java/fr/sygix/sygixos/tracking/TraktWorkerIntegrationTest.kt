/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TraktWorkerIntegrationTest {
    @Test fun rotatedSessionKeepsItsQueueAndAnUncertainMovieRemovalCompletesOnlyItsMissingWatchlistStep() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TrackingDatabase::class.java).build()
        try {
            val dispatcher = UnconfinedTestDispatcher(testScheduler)
            fun now() = 1_800_000_000_000L + testScheduler.currentTime
            val original = TraktSessionEnvelope.create("a".repeat(64), "fixture-old-access", "fixture-old-refresh", now() - 1).encode()
            var saved: String? = original
            val store = CoordinatedTrackingSessionStore(object : TrackingSessionStore {
                override suspend fun read() = saved
                override suspend fun write(token: String?) { saved = token }
            })
            var refreshes = 0
            var removals = 0
            var additions = 0
            var remoteSeen = true
            var watchlisted = false
            var reconciliationFails = true
            val requested = mutableListOf<String>()
            val transport = TrackingTransport { request ->
                requested += request.url
                when {
                    request.url == "https://auth.trakt.tv/oauth/token" -> {
                        refreshes++
                        TrackingResponse(200, """{"access_token":"fixture-new-access","refresh_token":"fixture-new-refresh","expires_in":604800}""")
                    }
                    request.url.startsWith("https://api.trakt.tv/sync/history/movies/7") -> {
                        assertEquals("Bearer fixture-new-access", request.headers["Authorization"])
                        if (!remoteSeen && reconciliationFails) throw TrackingException(TrackingFailure.NETWORK)
                        TrackingResponse(200, if (remoteSeen) """[{"id":9001,"watched_at":"2020-01-01T00:00:00Z"}]""" else "[]")
                    }
                    request.url.startsWith("https://api.trakt.tv/sync/watchlist/movies") ->
                        TrackingResponse(200, if (watchlisted) """[{"movie":{"ids":{"trakt":7},"title":"Fixture"}}]""" else "[]")
                    request.url == "https://api.trakt.tv/sync/history/remove" && request.method == "POST" -> {
                        removals++
                        remoteSeen = false
                        throw TrackingException(TrackingFailure.NETWORK)
                    }
                    request.url == "https://api.trakt.tv/sync/watchlist" && request.method == "POST" -> {
                        additions++
                        watchlisted = true
                        TrackingResponse(201, """{"added":{"movies":1,"shows":0,"episodes":0}}""")
                    }
                    else -> error("Unexpected Trakt worker fixture endpoint")
                }
            }
            val repository = TrackingRepository(database, ::now)
            val account = repository.bind(original, TrackingAccount("trakt:fixture-account", "Fixture"))
            val movie = TrackingContent(TrackingIdentity("trakt", TrackingType.MOVIE, "7"), title = "Fixture", seen = true, movieState = 1)
            repository.record(account.incarnation, movie, TrackingAction.UNSEEN)
            val api = TraktApi(transport, TraktConfigurationSource(transport, null, ::now, dispatcher), store, dispatcher, ::now)
            val provider = TraktProvider(api, ::now)
            fun coordinator() = TrackingSyncCoordinator(repository, provider, store, clock = ::now)
            assertFalse(coordinator().drain(account.incarnation))
            assertEquals("failure=${repository.account()?.failure}; requests=$requested", 1, refreshes)
            assertEquals(1, removals)
            assertEquals(0, additions)
            assertTrue(repository.pending(account.incarnation).single().possiblySent)
            assertEquals(account.incarnation, repository.restore(saved!!)?.incarnation)
            reconciliationFails = false
            assertTrue(coordinator().drain(account.incarnation))
            assertEquals(1, refreshes)
            assertEquals(1, removals)
            assertEquals(1, additions)
            assertTrue(watchlisted)
            assertFalse(remoteSeen)
            assertTrue(repository.pending(account.incarnation).isEmpty())
            assertEquals(1, TrackingProjection.candidates(repository.contents(account.incarnation)).size)
        } finally { database.close() }
    }
}
