/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.net.URI
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
class BetaSeriesReserveTest {
    private val seed = TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "1"),
        TrackingIdentity("betaseries", TrackingType.SHOW, "3"), "Example", season = 1, episode = 1)
    private fun episode(id: Int, date: String? = "2020-01-01"): JSONObject = JSONObject().put("id", id)
        .put("season", if (id <= 3) 1 else 2).put("episode", if (id <= 3) id else id - 3)
        .put("date", date).put("user", JSONObject().put("seen", false))
    private fun show() = JSONObject().put("id", 3).put("title", "Example").put("user", JSONObject().put("archived", false))
    private fun response(key: String, value: Any) = TrackingResponse(200, JSONObject().put(key, value).toString())

    @Test fun sixEpisodeWindowCrossesSeasonsAndPendingSeenDoesNotCountAsCandidate() = runTest {
        val seasons = mutableListOf<String>()
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
            request.url.endsWith('/') -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-key\"}")
            "episodes/list" in request.url -> response("shows", JSONArray().put(JSONObject().put("id", 3).put("unseen", JSONArray((1..6).map { episode(it) }))))
            "shows/display" in request.url -> response("show", show())
            "shows/seasons" in request.url -> response("seasons", JSONArray().put(JSONObject().put("number", 1)).put(JSONObject().put("number", 2)))
            "shows/episodes" in request.url -> {
                val number = URI(request.url).rawQuery.substringAfter("season=").substringBefore('&')
                seasons += number
                response("episodes", JSONArray((if (number == "1") 1..3 else 4..8).map { episode(it) }))
            }
            else -> error("Unexpected endpoint")
        } })) { LocalDate.of(2026, 1, 1) }
        val result = provider.reserve("fixture-session", seed, mapOf("1" to 1, "2" to 1))
        assertEquals(listOf("3", "4", "5", "6", "7", "8"), result.map { it.identity.id })
        assertEquals(listOf("1", "2"), seasons)
        assertEquals(setOf(1, 2), result.map { it.season }.toSet())
        assertTrue(result.all { it.airDate == "2020-01-01" })
    }

    @Test fun futureDatesAreExcludedAndUnknownDatesNeverProveACompleteReserve() = runTest {
        var unknown = false
        val provider = BetaSeriesProvider(trackingApi(TrackingTransport { request -> when {
            request.url.endsWith('/') -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-key\"}")
            "episodes/list" in request.url -> response("shows", JSONArray())
            "shows/display" in request.url -> response("show", show())
            "shows/seasons" in request.url -> response("seasons", JSONArray().put(JSONObject().put("number", 1)))
            "shows/episodes" in request.url -> response("episodes", JSONArray().put(episode(1)).put(episode(2, if (unknown) null else "2030-01-01")))
            else -> error("Unexpected endpoint")
        } })) { LocalDate.of(2026, 1, 1) }
        assertEquals(listOf("1"), provider.reserve("fixture-session", seed, emptyMap()).map { it.identity.id })
        unknown = true
        try { provider.reserve("fixture-session", seed, emptyMap()); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.INCOMPLETE, error.failure) }
    }
}
