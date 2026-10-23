/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BetaSeriesApiTest {
    @Test fun loadsAllPagesBeyondTwentyWithoutTreatingMissingPagesAsEmpty() = runTest {
        val api = api { request ->
            val page = if ("page=2" in request.url) 2 else 1
            val entries = ((page - 1) * 100 + 1..minOf(page * 100, 125)).joinToString(",") { "{\"id\":$it}" }
            TrackingResponse(200, "{\"shows\":[$entries],\"total\":125,\"totalMissingShows\":${if (page == 1) 25 else 0}}")
        }
        assertEquals(125, api.pages("shows/member", "shows", "fixture-token").size)
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
        val provider = BetaSeriesProvider(api)
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
        BetaSeriesProvider(api).mutate("fixture-token", TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), title = "Fixture"), TrackingAction.SEEN)
        assertEquals(1, writes)
        assertEquals(1, reads)
    }

    private fun api(handler: suspend (TrackingRequest) -> TrackingResponse): BetaSeriesApi {
        val transport = TrackingTransport { request ->
            if (request.url == "https://www.betaseries.com/") TrackingResponse(200, "<script>window.config={apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}</script>")
            else handler(request)
        }
        return BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport))
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
            } })
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
        val api = BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport))
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
        } })
        val requested = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "5"), TrackingIdentity("betaseries", TrackingType.SHOW, "1"), "Fixture")
        val mapped = provider.episodes("fixture-token", requested).single()
        assertNull(mapped.season)
        assertNull(mapped.episode)
        assertNull(mapped.durationMillis)
        assertTrue(mapped.offers.isEmpty())
    }
}
