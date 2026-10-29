/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TrackingRecoveryRegressionTest {
    @Test fun failedMovieCollectionDoesNotForceReloadOfACompleteEpisodeReserve() = runTest {
        val transport = TrackingTransport { request -> when {
            "movies/member" in request.url -> throw TrackingException(TrackingFailure.NETWORK)
            "shows/member" in request.url -> TrackingResponse(200, """{"shows":[{"id":1,"title":"Fixture","user":{"archived":false}}],"total":1}""")
            "episodes/list" in request.url -> TrackingResponse(200, """{"shows":[{"id":1,"unseen":[{"id":3,"season":1,"episode":1,"date":"2020-01-01","user":{"seen":false}}]}],"total":1}""")
            else -> TrackingResponse(200, """{apiHost:"https://api.betaseries.com",apiKey:"fixture"}""")
        } }
        val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport, null, { 0 }, Dispatchers.Unconfined), Dispatchers.Unconfined)
        val snapshot = BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }.snapshot("fixture")
        assertEquals(TrackingFailure.INCOMPLETE, snapshot.failure)
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TrackingDatabase::class.java).build()
        try {
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture", TrackingAccount("A", "Fixture"))
            repository.snapshot(account.incarnation, 0, snapshot)
            assertTrue(repository.reserves(account.incarnation).isEmpty())
            assertEquals(TrackingFailure.INCOMPLETE.name, repository.account()!!.failure)
        } finally { db.close() }
    }

    @Test fun undatedFutureEpisodeDoesNotDiscardAiredTraktEpisode() = runTest {
        val raw = TraktSessionEnvelope("client", "fixture-access", "fixture-refresh", Long.MAX_VALUE, "binding").encode()
        val store = CoordinatedTrackingSessionStore(object : TrackingSessionStore {
            override suspend fun read() = raw
            override suspend fun write(token: String?) = Unit
        })
        val transport = TrackingTransport { request ->
            TrackingResponse(200, when {
                "sync/watchlist/shows" in request.url -> """[{"show":{"title":"Fixture","ids":{"trakt":42}}}]"""
                "/progress/watched" in request.url -> """{"seasons":[{"number":1,"aired":1,"completed":0,"episodes":[{"number":1,"completed":false}]}]}"""
                "/seasons/1" in request.url -> """[{"title":"Aired","season":1,"number":1,"first_aired":"2020-01-01T00:00:00Z","ids":{"trakt":420}},{"title":"Future","season":1,"number":2,"first_aired":null,"ids":{"trakt":421}}]"""
                "/shows/42?" in request.url -> """{"title":"Fixture","ids":{"trakt":42}}"""
                else -> "[]"
            })
        }
        val api = TraktApi(transport, TraktConfigurationSource(transport, null, { 0 }, Dispatchers.Unconfined), store, Dispatchers.Unconfined, { 0 })
        val provider = TraktProvider(api) { 1_800_000_000_000 }
        val result = provider.snapshot(raw)
        assertEquals(listOf("420"), result.contents.map { it.identity.id })
        assertEquals(listOf("420"), provider.episodes(raw, result.contents.single()).map { it.identity.id })
        assertNull(result.failure)
    }

    @Test fun malformedBetaSeriesGroupKeepsValidMovie() = runTest {
        val transport = TrackingTransport { request -> TrackingResponse(200, when {
            "shows/member" in request.url -> """{"shows":[{"id":1,"title":"Fixture","user":{"archived":false}}],"total":1}"""
            "movies/member" in request.url -> """{"movies":[{"id":2,"title":"Movie","user":{"status":0}}],"total":1}"""
            "episodes/list" in request.url -> """{"shows":[{"id":1}],"total":1}"""
            else -> """{apiHost:"https://api.betaseries.com",apiKey:"fixture"}"""
        }) }
        val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport, null, { 0 }, Dispatchers.Unconfined), Dispatchers.Unconfined)
        val result = BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }.snapshot("fixture-token")
        assertEquals(listOf("2"), result.contents.map { it.identity.id })
        assertEquals(TrackingFailure.INCOMPLETE, result.failure)
    }

    @Test fun completeReserveReplacesStaleUnseenCandidates() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TrackingDatabase::class.java).build()
        try {
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture", TrackingAccount("A", "Fixture"))
            val parent = TrackingIdentity("betaseries", TrackingType.SHOW, "1")
            fun episode(number: Int) = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "$number"), parent,
                "Fixture", season = 1, episode = number, airDate = "2020-01-01")
            repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(episode(1), episode(2))))
            repository.fill(account.incarnation, parent, listOf(episode(2)), true, repository.account()!!.revision)
            assertEquals(listOf("2"), TrackingProjection.candidates(repository.contents(account.incarnation)).map { it.identity.id })
        } finally { db.close() }
    }

    @Test fun rejectedConfigurationDiscoveryIsSharedAcrossConsumers() = runTest {
        var discoveries = 0
        val source = TrackingApplicationConfiguration<String>(null, Dispatchers.Unconfined,
            decode = { null }, encode = { org.json.JSONObject() }, discover = { discoveries++; "rejected" })
        source.get()
        source.invalidate("rejected")
        repeat(3) { try { source.get() } catch (_: TrackingException) { } }
        assertEquals(2, discoveries)
    }

    @Test fun successfulReserveDoesNotClearUnrecoveredSnapshotFailure() = runTest {
        val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), TrackingDatabase::class.java).build()
        try {
            val repository = TrackingRepository(db)
            val account = repository.bind("fixture", TrackingAccount("A", "Fixture"))
            val episode = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "1"),
                TrackingIdentity("betaseries", TrackingType.SHOW, "10"), "Fixture", season = 1, episode = 1, airDate = "2020-01-01")
            repository.snapshot(account.incarnation, 0, TrackingSnapshot(listOf(episode), TrackingFailure.INCOMPLETE))
            val transport = TrackingTransport { error("Unexpected HTTP call") }
            val beta = BetaSeriesProvider(BetaSeriesApi(transport,
                BetaSeriesConfigurationSource(transport, null, { 0 }, Dispatchers.Unconfined))) { java.time.LocalDate.of(2026, 1, 1) }
            val provider = object : TrackingContentProvider by beta {
                override suspend fun reserve(token: String, content: TrackingContent, desired: Map<String, Int>) = listOf(episode)
            }
            val store = object : TrackingSessionStore {
                override suspend fun read() = "fixture"
                override suspend fun write(token: String?) = Unit
            }
            assertTrue(TrackingSyncCoordinator(repository, provider, store).drain(account.incarnation))
            assertEquals(TrackingFailure.INCOMPLETE.name, repository.account()!!.failure)
            repository.snapshot(account.incarnation, repository.account()!!.revision, TrackingSnapshot(listOf(episode)))
            assertNull(repository.account()!!.failure)
        } finally { db.close() }
    }
}
