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
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingUndoDispatchTest {
    private val episode = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "7"),
        TrackingIdentity("betaseries", TrackingType.SHOW, "8"), "Fixture", season = 1, episode = 1, airDate = "2020-01-01")

    @Test fun heldOnlyQueueStopsWithoutDispatchingOrRefillingItsReserve() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TrackingDatabase::class.java).build()
        try {
            val repository = TrackingRepository(database)
            val account = repository.bind("fixture-session", TrackingAccount("fixture", "Fixture"))
            repository.record(account.incarnation, episode, TrackingAction.SEEN, 10_000)
            var calls = 0
            val provider = BetaSeriesProvider(trackingApi(TrackingTransport {
                calls++
                throw TrackingException(TrackingFailure.NETWORK)
            })) { java.time.LocalDate.of(2026, 1, 1) }
            val store = object : TrackingSessionStore {
                override suspend fun read() = "fixture-session"
                override suspend fun write(token: String?) = Unit
            }
            val coordinator = TrackingSyncCoordinator(repository, provider, store)
            assertTrue(coordinator.drain(account.incarnation))
            assertTrue(coordinator.drain(account.incarnation))
            assertEquals(0, calls)
            assertTrue(repository.pending(account.incarnation).single().undoHeld)
            assertFalse(repository.pending(account.incarnation).single().possiblySent)
        } finally { database.close() }
    }

    @Test fun releasedMovieCanSynchronizeWhileAnEpisodeOfAnotherParentIsHeld() = runTest {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val database = Room.inMemoryDatabaseBuilder(context, TrackingDatabase::class.java).build()
        try {
            val repository = TrackingRepository(database)
            val account = repository.bind("fixture-session", TrackingAccount("fixture", "Fixture"))
            val held = repository.record(account.incarnation, episode, TrackingAction.SEEN, 10_000)
            val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "9"), title = "Fixture movie")
            repository.record(account.incarnation, movie, TrackingAction.SEEN)
            val requests = mutableListOf<TrackingRequest>()
            val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
                requests += request
                when {
                    request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                    request.method == "POST" -> TrackingResponse(200, "{}")
                    "movies/movie" in request.url -> TrackingResponse(200, """{"movie":{"id":9,"title":"Fixture movie","user":{"status":1}}}""")
                    else -> throw AssertionError("A held series must not be requested")
                }
            })) { java.time.LocalDate.of(2026, 1, 1) }
            val store = object : TrackingSessionStore {
                override suspend fun read() = "fixture-session"
                override suspend fun write(token: String?) = Unit
            }
            assertTrue(TrackingSyncCoordinator(repository, provider, store).drain(account.incarnation))
            assertEquals(1, requests.count { it.method == "POST" })
            assertEquals(listOf(held), repository.pending(account.incarnation))
            assertTrue(requests.none { "episodes/" in it.url })
        } finally { database.close() }
    }
}
