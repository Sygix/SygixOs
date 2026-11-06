/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingIdentityFallbackTest {
    private val olderShow = TrackingIdentity("betaseries", TrackingType.SHOW, "1")
    private val newerShow = TrackingIdentity("betaseries", TrackingType.SHOW, "2")
    private val followed = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "121"), olderShow, "Les Veilleurs de Brumeval", season = 2, episode = 1,
        aliases = setOf("The Brumeval Watch"))

    private fun episode(title: String?, year: Int? = 2025) = UpNextItem(7, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE,
        "Les Veilleurs de Brumeval - S1 E3", seriesTitle = "Les Veilleurs de Brumeval", season = "1", episode = "3", episodeTitle = title, year = year)

    private fun TestScope.identify(item: UpNextItem, shows: String, titles: Map<String, String>): TrackingContent? {
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("A", "Fixture")
            override suspend fun snapshot(token: String) = TrackingSnapshot(listOf(followed))
            override suspend fun revoke(token: String) = Unit
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) = Unit
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        val api = trackingApi(TrackingTransport { request ->
            val fields = java.net.URI(request.url).rawQuery.orEmpty().split('&').filter { it.isNotEmpty() }
                .associate { it.substringBefore('=') to java.net.URLDecoder.decode(it.substringAfter('='), "UTF-8") }
            val id = fields["id"].orEmpty()
            when {
                request.url.endsWith("/") -> TrackingResponse(200, """{"apiHost":"https://api.betaseries.com","apiKey":"fixture-client"}""")
                request.url.contains("shows/search") -> TrackingResponse(200, shows)
                request.url.contains("shows/display") -> TrackingResponse(200,
                    """{"show":{"id":$id,"title":"Fixture $id","user":{"in_account":${id == "1"},"archived":false}}}""")
                request.url.contains("shows/episodes") -> TrackingResponse(200,
                    """{"episodes":[{"id":${id}13,"season":1,"episode":3,"date":"2020-01-01","title":"${titles[id].orEmpty()}"}]}""")
                request.url.contains("episodes/display") -> TrackingResponse(200,
                    """{"episode":{"id":$id,"season":1,"episode":3,"title":"${titles[id.dropLast(2)].orEmpty()}","user":{"seen":false}}}""")
                else -> error("Unexpected identity fixture endpoint")
            }
        })
        val enrichment = TrackingEnrichment(controller, BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }, backgroundScope, { testScheduler.currentTime })
        enrichment.local(listOf(item)); runCurrent()
        return enrichment.items().single().tracking
    }

    private val reboots = """{"shows":[{"id":1,"title":"Les Veilleurs de Brumeval","creation":2001},{"id":2,"title":"Les Veilleurs de Brumeval","creation":2024}],"total":2}"""

    @Test fun sameTitleRebootIsNeverIdentifiedAsTheFollowedShowByItsTitle() = runTest {
        assertNull(identify(episode("Pilot"), reboots, mapOf("1" to "Pilot", "2" to "Pilot")))
    }

    @Test fun sameTitleRebootIsConfirmedOnlyAmongTheMatchingCandidatesByADistinctiveEpisodeTitle() = runTest {
        val identified = identify(episode("Le phare des oubliés"), reboots, mapOf("1" to "La tour de garde", "2" to "Le phare des oubliés"))
        assertEquals(newerShow, identified?.parent)
    }

    @Test fun translatedTitleWithoutAnyCandidateFallsBackToTheAccountAlias() = runTest {
        val identified = identify(episode(null).copy(title = "The Brumeval Watch - S1 E3", seriesTitle = "The Brumeval Watch"), """{"shows":[],"total":0}""", mapOf("1" to ""))
        assertEquals(olderShow, identified?.parent)
    }

    @Test fun episodeReleaseYearIsNotComparedWithTheShowYear() = runTest {
        val identified = identify(episode(null, year = 2025), """{"shows":[{"id":2,"title":"Les Veilleurs de Brumeval","creation":2024}],"total":1}""", mapOf("2" to ""))
        assertEquals(newerShow, identified?.parent)
    }
}
