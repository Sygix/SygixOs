/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.async
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BetaSeriesApiTest {
    @Test fun concurrentRejectedReadsShareOneConfigurationRecoveryAndEachReplayOnlyOnce() = runTest {
        var discoveries = 0
        val firstWave = kotlinx.coroutines.CompletableDeferred<Unit>()
        var refused = 0
        var accepted = 0
        val transport = TrackingTransport { request ->
            if (request.url.endsWith("/")) {
                discoveries++
                TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-$discoveries\"}")
            } else if (request.headers["X-BetaSeries-Key"] == "fixture-1") {
                if (++refused == 8) firstWave.complete(Unit)
                firstWave.await()
                TrackingResponse(400, """{"errors":[{"code":1001}]}""")
            } else {
                accepted++
                TrackingResponse(200, "{}")
            }
        }
        val dispatcher = kotlinx.coroutines.test.StandardTestDispatcher(testScheduler)
        val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport, null, { testScheduler.currentTime }, dispatcher), dispatcher)
        kotlinx.coroutines.coroutineScope {
            (1..8).map { async { api.request("shows/list") } }.forEach { it.await() }
        }
        assertEquals(2, discoveries)
        assertEquals(8, refused)
        assertEquals(8, accepted)
    }

    @Test fun seenCrossesSeasonBoundaryAndUndoRestoresOneCandidateThroughRealReadbacks() = runTest {
        val original = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "10"), TrackingIdentity("betaseries", TrackingType.SHOW, "1"), "Fixture", season = 1, episode = 10, archived = false, sessionGeneration = 0)
        val seen = mutableMapOf("10" to false, "11" to false)
        fun episode(id: String) = """{"id":$id,"season":${if (id == "10") 1 else 2},"episode":${if (id == "10") 10 else 1},"user":{"seen":${seen[id]}}}"""
        var snapshots = 0
        val provider = BetaSeriesProvider(api { request -> when {
            request.url.contains("episodes/watched") -> {
                val id = request.fields["id"] ?: java.net.URI(request.url).rawQuery.substringAfter("id=")
                seen[id] = request.method == "POST"
                TrackingResponse(200, "{}")
            }
            request.url.contains("episodes/display") -> {
                val id = java.net.URI(request.url).rawQuery.substringAfter("id=")
                TrackingResponse(200, "{\"episode\":${episode(id)}}")
            }
            request.url.contains("episodes/list") -> {
                assertTrue("showId=1" in request.url)
                val next = seen.entries.firstOrNull { !it.value }?.key
                TrackingResponse(200, "{\"shows\":[{\"id\":1,\"unseen\":[${next?.let(::episode).orEmpty()}]}]}")
            }
            else -> error("Unexpected mutation fixture endpoint")
        } }) { java.time.LocalDate.of(2026, 1, 1) }
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String): TrackingSnapshot { ++snapshots; return TrackingSnapshot(listOf(original)) }
            override suspend fun revoke(token: String) {}
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) {}
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        val actions = TrackingActions(controller, { token, content, action -> provider.mutate(token, content, action).forEach(controller::patch) }, backgroundScope, clock = { testScheduler.currentTime * 1_000_000 })
        actions.perform(original, TrackingAction.SEEN); runCurrent()
        assertEquals(listOf("11"), TrackingProjection.candidates(controller.state.value.contents).map { it.identity.id })
        actions.undo(); runCurrent()
        assertEquals(listOf("10"), TrackingProjection.candidates(controller.state.value.contents).map { it.identity.id })
        val heroes = TrackingHeroSource.project(fr.sygix.sygixos.domain.HeroRead(emptyList()), controller.state.value, emptyList(), emptyList(), emptySet()).items
        assertEquals(listOf("10"), heroes.mapNotNull { it.tracking?.identity?.id })
        assertEquals(1, snapshots)
    }

    @Test fun repeatedEmptyAndMalformedPagesPreservePreviouslyDecodedItems() = runTest {
        for (second in listOf("""{"shows":[{"id":1},{"id":2}],"total":3}""", """{"shows":[],"total":3}""", "{}")) {
            val api = api { request -> TrackingResponse(200, if ("offset=0" in request.url) """{"shows":[{"id":1},{"id":2}],"total":3,"totalMissingShows":0}""" else second) }
            try { api.pages("shows/member", "shows", "fixture-token"); fail() }
            catch (error: TrackingPagesException) { assertEquals(listOf("1", "2"), error.items.map { it.requiredId() }) }
        }
    }

    @Test fun episodesLoadCurrentSeasonFirstAndOtherSeasonsOnlyOnDemandWithoutPagination() = runTest {
        val seasons = mutableListOf<String>()
        val provider = BetaSeriesProvider(api { request -> when {
            request.url.contains("shows/display") -> TrackingResponse(200, """{"show":{"id":1,"title":"Fixture","user":{"archived":false}}}""")
            request.url.contains("shows/seasons") -> TrackingResponse(200, """{"seasons":[{"number":1},{"number":2},{"number":3}]}""")
            request.url.contains("shows/episodes") -> {
                val fields = java.net.URI(request.url).rawQuery.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
                assertFalse(fields.containsKey("page"))
                assertFalse(fields.containsKey("offset"))
                seasons += fields.getValue("season")
                TrackingResponse(200, """{"episodes":[{"id":5,"season":${fields["season"]},"episode":1,"date":"2020-01-01","user":{"seen":false}}]}""")
            }
            else -> error("Unexpected season fixture endpoint")
        } }) { java.time.LocalDate.of(2026, 1, 1) }
        val content = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), TrackingIdentity("betaseries", TrackingType.SHOW, "1"), "Fixture", season = 2)
        assertEquals(2, provider.episodes("fixture-token", content).single().season)
        assertEquals(listOf("2"), seasons)
        assertEquals(listOf(1, 2, 3), provider.seasons("fixture-token", content))
        assertEquals(listOf("2"), seasons)
        provider.episodes("fixture-token", content.copy(season = 3))
        assertEquals(listOf("2", "3"), seasons)
    }

    @Test fun snapshotRequestsSixReleasedEpisodesAndOnlyUnseenMoviesAndKeepsMoviesWhenEpisodePageFails() = runTest {
        val requests = mutableListOf<TrackingRequest>()
        var oversized = false
        val provider = BetaSeriesProvider(api { request ->
            requests += request
            when {
                request.url.contains("shows/member") -> TrackingResponse(200, """{"shows":[{"id":1,"title":"Fixture","user":{"archived":false}}],"total":1}""")
                request.url.contains("movies/member") -> TrackingResponse(200, """{"movies":[{"id":2,"title":"Fixture movie","user":{"status":0}}],"total":1}""")
                request.url.contains("episodes/list") -> {
                    val parameters = java.net.URI(request.url).rawQuery.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
                    assertEquals("6", parameters["limit"])
                    assertEquals("100", parameters["showsLimit"])
                    assertEquals("1", parameters["released"])
                    if (oversized) throw TrackingException(TrackingFailure.PROTOCOL)
                    TrackingResponse(200, """{"shows":[{"id":1,"unseen":[{"id":3,"season":1,"episode":1,"date":"2020-01-01","user":{"seen":false}}]}],"total":1,"totalEpisodes":50,"totalMissingShows":0}""")
                }
                else -> error("Unexpected fixture endpoint")
            }
        }) { java.time.LocalDate.of(2026, 1, 1) }
        assertEquals(2, provider.snapshot("fixture-token").contents.size)
        assertEquals(1, requests.count { it.url.contains("movies/member") })
        assertTrue(requests.filter { it.url.contains("movies/member") }.all { "state=0" in it.url })
        oversized = true
        val partial = provider.snapshot("fixture-token")
        assertEquals(TrackingFailure.INCOMPLETE, partial.failure)
        assertEquals(listOf("2"), partial.contents.map { it.identity.id })
    }

    @Test fun nonJsonRefusalIsRetryableAndDeleteQueryIsExplicit() = runTest {
        val refused = api { TrackingResponse(401, "Denied") }
        try { refused.request("members/infos", "fixture-token"); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        val sent = mutableListOf<TrackingRequest>()
        val service = api { request -> sent += request; TrackingResponse(200, "{}") }
        service.request("shows/show", "fixture-token", "DELETE", mapOf("id" to "42"))
        assertTrue(sent.single().url.endsWith("?id=42"))
        assertTrue(sent.single().fields.isEmpty())
    }

    @Test fun genericHttpRefusalsNeverProveThatTheTokenExpired() = runTest {
        for (status in listOf(401, 403)) for (body in listOf("{}", "{\"errors\":[]}", "<html>Fixture service unavailable</html>", "{invalid")) {
            try { api { TrackingResponse(status, body) }.request("members/infos", "fixture-token"); fail() }
            catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        }
    }

    @Test fun explicitTokenRefusalRequiresReconnectionRegardlessOfHttpStatus() = runTest {
        for (status in listOf(400, 401, 403)) {
            try { api { TrackingResponse(status, "{\"errors\":[{\"code\":1002}]}") }.request("members/infos", "fixture-token"); fail() }
            catch (error: BetaSeriesApiException) { assertEquals(TrackingFailure.ACCESS, error.trackingFailure()) }
        }
    }

    @Test fun loadsAllPagesBeyondTwentyWithoutTreatingMissingPagesAsEmpty() = runTest {
        val api = api { request ->
            assertFalse("page=" in request.url)
            val page = if ("offset=100" in request.url) 2 else 1
            val entries = ((page - 1) * 100 + 1..minOf(page * 100, 125)).joinToString(",") { "{\"id\":$it}" }
            TrackingResponse(200, "{\"shows\":[$entries],\"total\":125,\"totalMissingShows\":0}")
        }
        assertEquals(125, api.pages("shows/member", "shows", "fixture-token").size)
    }

    @Test fun moviePaginationUsesStartAndIgnoresMissingCountUntilTotalIsReached() = runTest {
        val offsets = mutableListOf<Int>()
        val api = api { request ->
            assertFalse("offset=" in request.url)
            assertFalse("page=" in request.url)
            val start = java.net.URI(request.url).rawQuery.split('&').first { it.startsWith("start=") }.substringAfter('=').toInt()
            offsets += start
            val entries = (start + 1..minOf(start + 2, 5)).joinToString(",") { "{\"id\":$it}" }
            TrackingResponse(200, "{\"movies\":[$entries],\"total\":5,\"totalMissingMovies\":0}")
        }
        assertEquals(5, api.pages("movies/member", "movies", "fixture-token", mapOf("limit" to "2", "state" to "0")).size)
        assertEquals(listOf(0, 2, 4), offsets)
    }

    @Test fun episodePaginationUsesDocumentedPageAndRejectsRepeatedResults() = runTest {
        val pages = mutableListOf<String>()
        val api = api { request ->
            val parameters = java.net.URI(request.url).rawQuery.split('&').associate { it.substringBefore('=') to it.substringAfter('=') }
            pages += parameters.getValue("page")
            assertEquals("1", parameters["limit"])
            assertEquals("100", parameters["showsLimit"])
            TrackingResponse(200, """{"shows":[{"id":1}],"total":101,"totalMissingShows":0}""")
        }
        try { api.pages("episodes/list", "shows", "fixture-token", mapOf("limit" to "1", "showsLimit" to "100")); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.INCOMPLETE, error.failure) }
        assertEquals(listOf("1", "2"), pages)
    }

    @Test fun emptyPageWithRemainingResultsIsIncomplete() = runTest {
        val api = api { TrackingResponse(200, "{\"shows\":[],\"total\":25,\"totalMissingShows\":25}") }
        try { api.pages("shows/member", "shows", "fixture-token"); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.INCOMPLETE, error.failure) }
    }

    @Test fun repeatedPageCannotPretendToBeComplete() = runTest {
        val api = api { TrackingResponse(200, "{\"shows\":[{\"id\":1}],\"total\":25,\"totalMissingShows\":24}") }
        try { api.pages("shows/member", "shows", "fixture-token"); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.INCOMPLETE, error.failure) }
    }

    @Test fun episodeMutationDisablesBulkAndUnseenDoesNotRemoveMovie() = runTest {
        val sent = mutableListOf<TrackingRequest>()
        val api = api { request ->
            if (request.method == "GET") TrackingResponse(200, if (request.url.contains("episodes/display")) "{\"episode\":{\"user\":{\"seen\":true}}}" else "{\"movie\":{\"user\":{\"status\":0}}}")
            else { sent += request; TrackingResponse(200, "{}") }
        }
        val provider = BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }
        provider.mutate("fixture-token", TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), title = "Fixture"), TrackingAction.SEEN)
        assertEquals("false", sent.single().fields["bulk"])
        assertEquals("5", sent.single().fields["id"])
        assertEquals("POST", sent.single().method)
        sent.clear()
        provider.mutate("fixture-token", TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "9"), title = "Fixture"), TrackingAction.UNSEEN)
        assertEquals("POST", sent.single().method)
        assertEquals("0", sent.single().fields["state"])
    }

    @Test fun nonJsonIsProtocolFailureAndNotAnEmptyAccount() = runTest {
        val api = api { TrackingResponse(502, "Unavailable") }
        try { api.pages("shows/member", "shows", "fixture-token"); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.PROTOCOL, error.failure) }
    }

    @Test fun lostMutationResponseIsReconciledWithoutReplayingMutation() = runTest {
        var writes = 0
        var reads = 0
        val api = api { request ->
            if (request.method == "POST") { ++writes; throw TrackingException(TrackingFailure.NETWORK) }
            ++reads
            TrackingResponse(200, "{\"episode\":{\"user\":{\"seen\":true}}}")
        }
        BetaSeriesProvider(api) { java.time.LocalDate.of(2026, 1, 1) }.mutate("fixture-token", TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), title = "Fixture"), TrackingAction.SEEN)
        assertEquals(1, writes)
        assertEquals(1, reads)
    }

    private fun kotlinx.coroutines.test.TestScope.api(handler: suspend (TrackingRequest) -> TrackingResponse): BetaSeriesApi {
        val transport = TrackingTransport { request ->
            if (request.url == "https://www.betaseries.com/") TrackingResponse(200, "<script>window.config={apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}</script>")
            else handler(request)
        }
        return trackingApi(transport)
    }

    @Test fun archiveMappingAndUndoRestoreBothOriginalArchiveStates() = runTest {
        for (initial in listOf(false, true)) {
            var archived = initial
            val writes = mutableListOf<Boolean>()
            val provider = BetaSeriesProvider(api { request -> when {
                request.url.contains("shows/archive") -> { archived = request.method == "POST"; writes += archived; TrackingResponse(200, "{}") }
                request.url.contains("shows/display") -> TrackingResponse(200, "{\"show\":{\"id\":1,\"title\":\"Fixture\",\"user\":{\"archived\":$archived}}}")
                request.url.contains("shows/episodes") -> TrackingResponse(200, "{\"episodes\":[{\"id\":5,\"season\":2,\"episode\":5,\"date\":\"2020-01-01\",\"user\":{\"seen\":false}}],\"total\":1,\"totalMissingEpisodes\":0}")
                else -> error("Unexpected archive fixture endpoint")
            } }) { java.time.LocalDate.of(2026, 1, 1) }
            val requested = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), TrackingIdentity("betaseries", TrackingType.SHOW, "1"), "Fixture")
            val mapped = provider.episodes("fixture-token", requested).single()
            assertEquals(initial, mapped.archived)
            val controller = TrackingController(object : WatchTrackingProvider {
                override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
                override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList())
                override suspend fun revoke(token: String) {}
            }, object : TrackingSessionStore {
                override suspend fun read() = "fixture-token"
                override suspend fun write(token: String?) {}
            }, backgroundScope)
            controller.restore(); runCurrent()
            val actions = TrackingActions(controller, provider::mutate, backgroundScope)
            actions.perform(mapped, if (initial) TrackingAction.UNARCHIVE else TrackingAction.ARCHIVE); runCurrent()
            actions.undo(); runCurrent()
            assertEquals(listOf(!initial, initial), writes)
            assertEquals(initial, archived)
            assertNull(actions.notice.value)
        }
    }

    @Test fun rejectedPostConfigurationIsInvalidatedButMutationIsNotReplayed() = runTest {
        var discoveries = 0
        val writes = mutableListOf<String?>()
        val transport = TrackingTransport { request ->
            if (request.url.endsWith("/")) {
                ++discoveries
                TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-$discoveries\"}")
            } else {
                writes += request.fields["client_id"]
                if (request.fields["client_id"] == "fixture-1") TrackingResponse(400, "{\"errors\":[{\"code\":1001}]}") else TrackingResponse(200, "{}")
            }
        }
        val api = trackingApi(transport)
        try { api.request("oauth/device", method = "POST"); fail() }
        catch (error: BetaSeriesApiException) { assertEquals(1001, error.code) }
        assertEquals(listOf("fixture-1"), writes)
        assertEquals(1, discoveries)
        api.request("oauth/device", method = "POST")
        assertEquals(listOf("fixture-1", "fixture-2"), writes)
        assertEquals(2, discoveries)
    }

    @Test fun missingEpisodeNumbersRemainUnknownRatherThanBecomingSeasonZero() = runTest {
        val provider = BetaSeriesProvider(api { request -> when {
            request.url.contains("shows/display") -> TrackingResponse(200, "{\"show\":{\"id\":1,\"title\":\"Fixture\",\"user\":{\"archived\":false}}}")
            else -> TrackingResponse(200, "{\"episodes\":[{\"id\":5,\"date\":\"2020-01-01\",\"user\":{\"seen\":false}}],\"total\":1,\"totalMissingEpisodes\":0}")
        } }) { java.time.LocalDate.of(2026, 1, 1) }
        val requested = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), TrackingIdentity("betaseries", TrackingType.SHOW, "1"), "Fixture")
        val mapped = provider.episodes("fixture-token", requested).single()
        assertNull(mapped.season)
        assertNull(mapped.episode)
        assertNull(mapped.durationMillis)
        assertTrue(mapped.offers.isEmpty())
    }
}
