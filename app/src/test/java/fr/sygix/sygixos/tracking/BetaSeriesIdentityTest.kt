/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BetaSeriesIdentityTest {
    private val localized = UpNextItem(7, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Les Sorcières de Brumeval - S1 E4",
        seriesTitle = "Les Sorcières de Brumeval", season = "1", episode = "4")
    private val today = java.time.LocalDate.of(2026, 1, 1)

    private fun details(request: TrackingRequest): TrackingResponse? = when {
        request.url.endsWith("/") -> TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""")
        request.url.contains("shows/display") -> TrackingResponse(200, """{"show":{"id":1,"title":"Brumeval Witches","user":{"in_account":true,"archived":false}}}""")
        request.url.contains("shows/episodes") -> TrackingResponse(200, """{"episodes":[{"id":14,"season":1,"episode":4}]}""")
        request.url.contains("episodes/display") -> TrackingResponse(200, """{"episode":{"id":14,"season":1,"episode":4,"user":{"seen":true}}}""")
        else -> null
    }

    @Test fun searchAliasesResolveALocalizedTitleWithoutExternalIds() = runTest {
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
            details(request) ?: if (request.url.contains("shows/search")) TrackingResponse(200,
                """{"shows":[{"id":1,"title":"Brumeval Witches","original_title":"Brumeval Witches","aliases":{"0":"Les Sorcières de Brumeval"}}],"total":1}""")
            else error("Unexpected fixture endpoint")
        })) { today }
        val identified = provider.identify("fixture-token", localized)!!
        assertEquals(TrackingIdentity("betaseries", TrackingType.SHOW, "1"), identified.parent)
        assertEquals("14", identified.identity.id)
    }

    @Test fun localizedTitleMissingFromTheCatalogSearchResolvesThroughTheAccountContents() = runTest {
        var searches = 0
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request ->
            details(request) ?: if (request.url.contains("shows/search")) { searches++; TrackingResponse(200, """{"shows":[],"total":0}""") }
            else error("Unexpected fixture endpoint")
        })) { today }
        val parent = TrackingIdentity("betaseries", TrackingType.SHOW, "1")
        val next = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "21"), parent, "Brumeval Witches", season = 2, episode = 1,
            aliases = setOf("Les Sorcières de Brumeval"))
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String) = TrackingSnapshot(listOf(next))
            override suspend fun revoke(token: String) = Unit
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) = Unit
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        val enrichment = TrackingEnrichment(controller, provider, backgroundScope, { testScheduler.currentTime })
        enrichment.local(listOf(localized)); runCurrent()
        val tracking = enrichment.items().single().tracking!!
        assertEquals(1, searches)
        assertEquals(parent, tracking.parent)
        assertEquals("14", tracking.identity.id)
        assertTrue(tracking.seen)
    }

    private suspend fun kotlinx.coroutines.test.TestScope.snapshot(): List<TrackingContent> = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
        request.url.endsWith("/") -> TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""")
        request.url.contains("shows/member") -> TrackingResponse(200,
            """{"shows":[{"id":1,"title":"Brumeval Witches","original_title":"Brumeval Witches","aliases":["Les Sorcières de Brumeval"],"user":{"archived":false}}],"total":1}""")
        request.url.contains("movies/member") -> TrackingResponse(200, """{"movies":[{"id":9,"title":"Fixture movie","length":8220,"user":{"status":0}}],"total":1}""")
        request.url.contains("episodes/list") -> TrackingResponse(200, """{"shows":[{"id":1,"unseen":[
            {"id":30,"season":0,"episode":1,"title":"Special","date":"2025-01-01","special":1,"user":{"seen":false}},
            {"id":31,"season":2,"episode":1,"title":"TBA","date":"2025-01-02","user":{"seen":false}},
            {"id":32,"season":2,"episode":2,"title":"Episode 2","date":"2025-01-09","user":{"seen":false}}]}],"total":1}""")
        else -> error("Unexpected fixture endpoint")
    } })) { today }.snapshot("fixture-token").contents

    @Test fun snapshotKeepsTheShowAliasesOnItsEpisodes() = runTest {
        assertEquals(setOf("Brumeval Witches", "Les Sorcières de Brumeval"), snapshot().first { it.identity.type == TrackingType.EPISODE }.aliases)
    }
}
