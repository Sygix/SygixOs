/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.*
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingEnrichmentTest {
    @Test fun episodeIdentificationSearchesNormalizedSeriesTitleWithFiveCandidatesAtMost() = runTest {
        var searches = 0
        val api = trackingApi(TrackingTransport { request -> when {
            request.url.endsWith("/") -> TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""")
            request.url.contains("shows/search") -> {
                ++searches
                val fields = java.net.URI(request.url).rawQuery.split('&').associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8") }
                assertEquals("Fixture", fields["title"])
                assertEquals("5", fields["limit"])
                TrackingResponse(200, """{"shows":[{"id":1,"title":"Fixture"}],"total":1}""")
            }
            request.url.contains("shows/display") -> TrackingResponse(200, """{"show":{"id":1,"title":"Fixture","user":{"in_account":true,"archived":false}}}""")
            request.url.contains("shows/episodes") -> TrackingResponse(200, """{"episodes":[{"id":5,"season":1,"episode":1}]}""")
            request.url.contains("episodes/display") -> TrackingResponse(200, """{"episode":{"id":5,"season":1,"episode":1,"user":{"seen":false}}}""")
            else -> error("Unexpected series fixture endpoint")
        } })
        val provider = BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }
        val item = UpNextItem(7, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Fixture - S1 E1 - Episode 1", season = "1", episode = "1")
        val identified = provider.identify("fixture-token", item)
        assertEquals("5", identified?.identity?.id)
        assertEquals(1, searches)
    }

    @Test fun partialAccountKeepsEnrichmentAndRefreshOnlyReadsExpiredCachedIdentity() = runTest {
        var searches = 0
        var details = 0
        val api = trackingApi(TrackingTransport { request -> when {
            request.url.endsWith("/") -> TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""")
            request.url.contains("movies/search") -> { ++searches; TrackingResponse(200, """{"movies":[{"id":42,"title":"Fixture","production_year":2020}],"total":1}""") }
            request.url.contains("movies/movie") -> { ++details; TrackingResponse(200, """{"movie":{"id":42,"title":"Fixture","user":{"in_account":true,"status":1}}}""") }
            else -> error("Unexpected identity fixture endpoint")
        } })
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList(), TrackingFailure.INCOMPLETE)
            override suspend fun revoke(token: String) {}
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) {}
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        val enrichment = TrackingEnrichment(controller, BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }, backgroundScope, { testScheduler.currentTime })
        val item = UpNextItem(7, UpNextSourceEntry("fixture.player"), UpNextContentType.MOVIE, "Fixture", year = 2020)
        enrichment.local(listOf(item)); runCurrent()
        assertEquals(1, searches)
        assertEquals(1, details)
        assertEquals(AccountMembership.PRESENT, enrichment.items().single().tracking?.membership)
        assertEquals(item.key, enrichment.items().single().key)
        controller.refresh(); runCurrent()
        enrichment.refresh(); runCurrent()
        assertEquals(1, searches)
        assertEquals(1, details)
        advanceTimeBy(900_001)
        enrichment.refresh(); runCurrent()
        assertEquals(1, searches)
        assertEquals(2, details)
    }
}
