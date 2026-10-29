/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.Instant
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class TraktProtocolTest {
    @Test fun concurrentMutationsHaveOneSecondBetweenTheirActualStarts() = runTest {
        val starts = mutableListOf<Long>()
        val raw = TraktSessionEnvelope.create("client", "access-fixture", "refresh-fixture", 1_000_000).encode()
        val store = CoordinatedTrackingSessionStore(MemorySession(raw))
        val transport = ScriptedTransport { starts += testScheduler.currentTime; TrackingResponse(200, "{}") }
        val api = TraktApi(transport, TraktConfigurationSource(transport, null, { testScheduler.currentTime }, kotlinx.coroutines.Dispatchers.Unconfined),
            store, kotlinx.coroutines.Dispatchers.Unconfined, { testScheduler.currentTime }, { testScheduler.currentTime })
        kotlinx.coroutines.coroutineScope {
            (1..3).map { async { api.request("sync/watchlist", raw, "POST", JSONObject()) } }.forEach { it.await() }
        }
        assertEquals(listOf(0L, 1_000L, 2_000L), starts)
    }
    @Test fun sessionEnvelopeRestoresProviderAndStableBindingWithoutExposingCredentials() {
        val original = TraktSessionEnvelope.create("client", "access-secret", "refresh-secret", 1234)
        val encoded = original.encode()
        val restored = TraktSessionEnvelope.decode(encoded)!!
        assertEquals("trakt", TraktSessionEnvelope.provider(encoded))
        assertEquals(original.binding, TraktSessionEnvelope.binding(encoded))
        assertEquals(original.accessToken, restored.accessToken)
        assertFalse(restored.toString().contains("secret"))
        assertEquals("betaseries", TraktSessionEnvelope.provider("legacy-token"))
        assertEquals("legacy-token", TraktSessionEnvelope.binding("legacy-token"))
    }

    @Test fun protectedSessionReadsAreReusedUntilAnAtomicReplacement() = runTest {
        val original = TraktSessionEnvelope("client", "old-access", "refresh", 1_000_000, "binding")
        val base = MemorySession(original.encode())
        val store = CoordinatedTrackingSessionStore(base)
        val authorizations = mutableListOf<String?>()
        val transport = ScriptedTransport { request -> authorizations += request.headers["Authorization"]; TrackingResponse(200, "{}") }
        val api = api(transport, store)
        api.request("users/settings", original.encode())
        api.request("users/settings", original.encode())
        assertEquals(1, base.reads)
        store.write(original.copy(accessToken = "new-access").encode())
        api.request("users/settings", original.encode())
        assertEquals(listOf("Bearer old-access", "Bearer old-access", "Bearer new-access"), authorizations)
        assertEquals(1, base.reads)
    }

    @Test fun refreshIsPersistedBeforeTheRequestAndPreservesBinding() = runTest {
        val original = TraktSessionEnvelope("client", "old", "refresh", testScheduler.currentTime + 1, "binding")
        val raw = original.encode()
        val store = CoordinatedTrackingSessionStore(MemorySession(raw))
        val transport = ScriptedTransport { request ->
            if (request.url == "https://auth.trakt.tv/oauth/token") TrackingResponse(200, """{"access_token":"new","refresh_token":"rotated","expires_in":3600}""")
            else TrackingResponse(200, "{}")
        }
        val api = TraktApi(transport, TraktConfigurationSource(transport, null, { 10_000 }, kotlinx.coroutines.Dispatchers.Unconfined), store,
            kotlinx.coroutines.Dispatchers.Unconfined, { 10_000 })
        api.request("users/settings", raw)
        val saved = TraktSessionEnvelope.decode(store.read())!!
        assertEquals("binding", saved.binding)
        assertEquals("new", saved.accessToken)
        assertEquals("new", transport.requests.last().headers["Authorization"]?.removePrefix("Bearer "))
        assertTrue(transport.requests.any { it.url == "https://auth.trakt.tv/oauth/token" })
    }

    @Test fun genericRefusalsDoNotQualifyForSessionRecovery() {
        assertNull(classifyTraktRefusal(401, ""))
        assertNull(classifyTraktRefusal(403, "invalid_token"))
        assertEquals(TrackingFailure.ACCESS, classifyTraktRefusal(401, "invalid_token"))
        assertEquals(TrackingFailure.CONFIGURATION, classifyBetaSeriesRefusal(1001))
        assertEquals(TrackingFailure.ACCESS, classifyBetaSeriesRefusal(2004))
        assertEquals(TrackingFailure.PROTOCOL, classifyBetaSeriesRefusal(9000))
    }

    @Test fun compareAndSetDoesNotRecreateLoggedOutSession() = runTest {
        val source = MemorySession("old")
        val store = CoordinatedTrackingSessionStore(source)
        source.value = null
        assertFalse(store.compareAndSet("old", "new"))
        assertNull(source.value)
    }

    @Test fun parallelExpiredRequestsShareOneRefresh() = runTest {
        val original = TraktSessionEnvelope("client", "old", "refresh", 1, "binding")
        val store = CoordinatedTrackingSessionStore(MemorySession(original.encode()))
        var refreshes = 0
        val transport = ScriptedTransport { request ->
            if (request.url == "https://auth.trakt.tv/oauth/token") {
                refreshes++
                TrackingResponse(200, """{"access_token":"new","refresh_token":"rotated","expires_in":3600}""")
            } else TrackingResponse(200, "{}")
        }
        val api = api(transport, store)
        List(8) { async { api.request("users/settings", original.encode()) } }.forEach { it.await() }
        assertEquals(1, refreshes)
    }

    @Test fun receivedRotatedPairCanRepairStorageWithoutExchangingAgain() = runTest {
        val original = TraktSessionEnvelope("client", "old", "refresh", 1, "binding")
        val base = MemorySession(original.encode()).apply { failWriteNumber = 2 }
        val store = CoordinatedTrackingSessionStore(base)
        var refreshes = 0
        val transport = ScriptedTransport { request ->
            if (request.url == "https://auth.trakt.tv/oauth/token") {
                refreshes++
                TrackingResponse(200, """{"access_token":"new","refresh_token":"rotated","expires_in":3600}""")
            } else TrackingResponse(200, "{}")
        }
        val api = api(transport, store)
        try { api.request("users/settings", original.encode()); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.STORAGE, error.failure) }
        base.failWriteNumber = null
        api.request("users/settings", original.encode())
        assertEquals(1, refreshes)
        assertEquals("new", TraktSessionEnvelope.decode(store.read())?.accessToken)
    }

    @Test fun logoutRevokesTheLatestPairEvenWhenControllerRetainsTheOriginalEnvelope() = runTest {
        val original = TraktSessionEnvelope("client", "old", "refresh", 1, "binding")
        val store = CoordinatedTrackingSessionStore(MemorySession(original.encode()))
        var revoked: String? = null
        val transport = ScriptedTransport { request ->
            if (request.url == "https://auth.trakt.tv/oauth/token") TrackingResponse(200, """{"access_token":"new","refresh_token":"rotated","expires_in":3600}""")
            else if (request.url == "https://auth.trakt.tv/oauth/revoke") {
                revoked = JSONObject(request.jsonBody!!).getString("token")
                TrackingResponse(200, "{}")
            }
            else TrackingResponse(200, "{}")
        }
        val api = api(transport, store)
        api.request("users/settings", original.encode())
        store.write(null)
        api.revoke(original.encode())
        assertEquals("new", revoked)
        assertNull(store.read())
    }

    @Test fun lostRefreshResponseLeavesDurableUncertaintyAndNeverReissuesToken() = runTest {
        val original = TraktSessionEnvelope("client", "old", "refresh", 1, "binding")
        val store = CoordinatedTrackingSessionStore(MemorySession(original.encode()))
        var refreshes = 0
        val transport = ScriptedTransport { request ->
            if (request.url == "https://auth.trakt.tv/oauth/token") { refreshes++; throw TrackingException(TrackingFailure.NETWORK) }
            TrackingResponse(200, "{}")
        }
        val api = api(transport, store)
        repeat(2) {
            try { api.request("users/settings", original.encode()); throw AssertionError() }
            catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        }
        assertEquals(1, refreshes)
        assertTrue(TraktSessionEnvelope.decode(store.read())!!.uncertain)
    }

    @Test fun sessionRotationKeepsIdentityWhileNewAssociationGetsDifferentBinding() {
        val first = TraktSessionEnvelope.create("client-a", "a", "r1", 100)
        val rotated = first.copy(accessToken = "b", refreshToken = "r2")
        val reassociated = TraktSessionEnvelope.create("client-b", "c", "r3", 100)
        assertEquals(first.binding, rotated.binding)
        assertNotEquals(first.binding, reassociated.binding)
        assertEquals("client-a", rotated.clientId)
    }

    @Test fun deviceProtocolUsesOneAttemptAndRecognizesPendingSlowDownAndExpiry() = runTest {
        val client = "a".repeat(64)
        var pollResult = "authorization_pending"
        val transport = ScriptedTransport { request ->
            when {
                request.url == "https://app.trakt.tv/" -> TrackingResponse(200, """import("https://app.trakt.tv/entry/app.one.js")""")
                request.url.endsWith("/entry/app.one.js") -> TrackingResponse(200, """"nodes/0.bundle.js"""")
                request.url.endsWith("/nodes/0.bundle.js") -> TrackingResponse(200, "trakt-api-key=$client")
                request.url == "https://auth.trakt.tv/oauth/device/code" -> TrackingResponse(200, """{"device_code":"private","user_code":"ABCD","verification_url":"https://trakt.tv/activate","expires_in":60,"interval":5}""")
                request.url == "https://auth.trakt.tv/oauth/device/token" -> TrackingResponse(400, """{"error":"$pollResult"}""")
                else -> throw AssertionError(request.url)
            }
        }
        var monotonic = 10L
        val source = TraktConfigurationSource(transport, null, { 1000 }, kotlinx.coroutines.Dispatchers.Unconfined)
        val protocol = TraktDeviceProtocol(TraktApi(transport, source, CoordinatedTrackingSessionStore(MemorySession(null)), kotlinx.coroutines.Dispatchers.Unconfined, { 1000 }), { monotonic }, { 1000 })
        val association = protocol.prepare()
        assertEquals("ABCD", association.userCode)
        assertEquals(TrackingDevicePoll.Pending, protocol.poll(association))
        pollResult = "slow_down"
        assertEquals(10_000L, (protocol.poll(association) as TrackingDevicePoll.SlowDown).intervalMillis)
        pollResult = "access_denied"
        try { protocol.poll(association); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.ACCESS, error.failure) }
        monotonic = association.expiresAt
        assertEquals(TrackingDevicePoll.Expired, protocol.poll(association))
        assertTrue(transport.requests.any { it.url == "https://auth.trakt.tv/oauth/device/token" && it.jsonBody?.contains("\"code\":\"private\"") == true })
    }

    @Test fun providerPaginatesBeyondCardLimitAndPreservesPartialMoviesOnLaterPageFailure() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val movies = (1..101).map { number ->
            JSONObject().put("movie", JSONObject().put("title", "Movie $number").put("year", 2020)
                .put("ids", JSONObject().put("trakt", number)))
        }
        var failSecondPage = false
        val transport = ScriptedTransport { request ->
            when {
                request.url.contains("sync/watchlist/movies?page=1") -> TrackingResponse(200, JSONArray(movies.take(100)).toString(), headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "2"))
                request.url.contains("sync/watchlist/movies?page=2") && failSecondPage -> throw TrackingException(TrackingFailure.NETWORK)
                request.url.contains("sync/watchlist/movies?page=2") -> TrackingResponse(200, JSONArray(movies.drop(100)).toString(), headers = mapOf("X-Pagination-Page" to "2", "X-Pagination-Page-Count" to "2"))
                else -> TrackingResponse(200, "[]", headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "1"))
            }
        }
        val store = CoordinatedTrackingSessionStore(MemorySession(envelope))
        val api = TraktApi(transport, TraktConfigurationSource(transport, null, { 1 }, kotlinx.coroutines.Dispatchers.Unconfined), store,
            kotlinx.coroutines.Dispatchers.Unconfined, { 1 })
        val provider = TraktProvider(api) { Instant.parse("2026-01-01T00:00:00Z").toEpochMilli() }
        assertEquals(101, provider.snapshot(envelope).contents.size)
        failSecondPage = true
        val partial = provider.snapshot(envelope)
        assertEquals(100, partial.contents.size)
        assertEquals(TrackingFailure.INCOMPLETE, partial.failure)
    }

    @Test fun snapshotRequestsArtworkAndExcludesMoviesAlreadySeenInTheirTargetedHistory() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        fun movieRow(id: Int) = JSONObject().put("movie", JSONObject().put("ids", JSONObject().put("trakt", id))
            .put("title", "Fixture $id").put("runtime", 100).put("images", JSONObject().put("fanart", JSONArray().put("cdn.trakt.tv/fixture.webp"))))
        val transport = ScriptedTransport { request ->
            when {
                "sync/watchlist/movies" in request.url -> {
                    assertTrue(request.url.contains("extended=full%2Cimages"))
                    TrackingResponse(200, JSONArray().put(movieRow(10)).put(movieRow(11)).toString(), headers = paginationSingle())
                }
                "sync/history/movies/10" in request.url -> TrackingResponse(200, """[{"id":1}]""")
                "sync/history/movies/11" in request.url -> TrackingResponse(200, "[]")
                else -> TrackingResponse(200, "[]", headers = paginationSingle())
            }
        }
        val snapshot = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope)))).snapshot(envelope)
        assertEquals(listOf("11"), snapshot.contents.map { it.identity.id })
        assertEquals("https://cdn.trakt.tv/fixture.webp", snapshot.contents.single().image)
        assertEquals(6_000_000L, snapshot.contents.single().durationMillis)
        assertNull(snapshot.failure)
    }

    @Test fun hiddenProgressIsExcludedAndStartedShowsCannotOfferRemove() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val show = JSONObject().put("title", "Synthetic Show").put("ids", JSONObject().put("trakt", 42))
        val episode = JSONObject().put("title", "Synthetic Episode").put("season", 1).put("number", 2)
            .put("first_aired", "2026-01-01T00:00:00.000Z").put("completed", false).put("ids", JSONObject().put("trakt", 420))
        val progressRow = JSONObject().put("show", show).put("progress", JSONObject().put("next_episode", episode).put("completed", 1)
            .put("seasons", JSONArray().put(JSONObject().put("number", 1).put("completed", 1).put("aired", 2))))
        val watchedProgress = JSONObject().put("seasons", JSONArray().put(JSONObject().put("number", 1).put("aired", 2).put("completed", 1)
            .put("episodes", JSONArray().put(JSONObject().put("number", 1).put("completed", true)).put(JSONObject().put("number", 2).put("completed", false)))))
        var hidden = true
        val transport = ScriptedTransport { request ->
            val body = when {
                request.url.contains("sync/progress/up_next") -> JSONArray().put(progressRow).toString()
                request.url.contains("users/hidden/progress_watched") && hidden -> JSONArray().put(JSONObject().put("show", show)).toString()
                request.url.contains("shows/42/progress/watched") -> watchedProgress.toString()
                request.url.contains("shows/42?extended=full") -> show.toString()
                request.url.contains("shows/42/seasons/1") -> JSONArray().put(episode).toString()
                else -> "[]"
            }
            TrackingResponse(200, body, headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "1"))
        }
        val store = CoordinatedTrackingSessionStore(MemorySession(envelope))
        val api = api(transport, store)
        val provider = TraktProvider(api) { Instant.parse("2026-02-01T00:00:00Z").toEpochMilli() }
        assertTrue(provider.snapshot(envelope).contents.isEmpty())
        hidden = false
        val visible = provider.snapshot(envelope).contents.single()
        assertEquals(TrackingType.EPISODE, visible.identity.type)
        assertFalse(visible.removableFromAccount)
        assertEquals(false, visible.archived)
    }

    @Test fun watchlistedUnstartedShowLoadsItsOwnProgressAndNestedUpNextProgress() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val started = showJson(42, "Started")
        val watchlisted = showJson(43, "Watchlisted")
        val nextStarted = episodeJson(420, 2, "2026-01-01T00:00:00Z")
        val nextWatchlisted = episodeJson(430, 1, "2026-01-02T00:00:00Z")
        val progressRow = JSONObject().put("show", started).put("progress", JSONObject().put("next_episode", nextStarted).put("completed", 1))
        fun progress() = JSONObject().put("seasons", JSONArray().put(JSONObject().put("number", 1).put("aired", 2).put("completed", 0)
            .put("episodes", JSONArray().put(JSONObject().put("number", 1).put("completed", false)).put(JSONObject().put("number", 2).put("completed", false)))))
        val metadata = JSONArray().put(episodeJson(420, 2, "2026-01-01T00:00:00Z"))
        var unstartedProgressRead = false
        val transport = ScriptedTransport { request ->
            val body = when {
                request.url.contains("sync/watchlist/shows") -> JSONArray().put(JSONObject().put("show", watchlisted)).toString()
                request.url.contains("sync/progress/up_next") -> JSONArray().put(progressRow).toString()
                request.url.contains("shows/43/progress/watched") -> { unstartedProgressRead = true; JSONObject().put("next_episode", nextWatchlisted).put("seasons", JSONArray().put(JSONObject().put("number", 1).put("aired", 1).put("completed", 0).put("episodes", JSONArray().put(JSONObject().put("number", 1).put("completed", false))))).toString() }
                request.url.contains("shows/42/progress/watched") -> progress().toString()
                request.url.contains("shows/42?extended=full") -> started.toString()
                request.url.contains("shows/43?extended=full") -> watchlisted.toString()
                request.url.contains("shows/42/seasons/1") -> metadata.toString()
                request.url.contains("shows/43/seasons/1") -> JSONArray().put(nextWatchlisted).toString()
                else -> "[]"
            }
            TrackingResponse(200, body, headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "1"))
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope)))) {
            Instant.parse("2026-02-01T00:00:00Z").toEpochMilli()
        }
        val snapshot = provider.snapshot(envelope)
        assertTrue(unstartedProgressRead)
        assertEquals(setOf("420", "430"), snapshot.contents.map { it.identity.id }.toSet())
    }

    @Test fun watchnowCountryObjectParsesServiceSourcesAndClearsStaleConfirmation() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val body = """{"fr":{"subscription":[{"source":"netflix","link":"https://example.test/watch","link_android":"intent://watch","prices":["9.99"],"currency":"EUR","uhd":true}],"free":[],"cable":[],"purchase":[],"cinema":[]}}"""
        val transport = ScriptedTransport { request ->
            assertTrue(request.url.endsWith("/movies/10/watchnow/fr?links=android"))
            TrackingResponse(200, body)
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope))))
        val movie = TrackingContent(TrackingIdentity("trakt", TrackingType.MOVIE, "10"), title = "Synthetic")
        val offers = provider.offers(envelope, movie, "FR")
        assertEquals("netflix", offers.offers.single().id)
        assertEquals("Netflix", offers.offers.single().name)
        assertEquals("fr", offers.offers.single().confirmedCountry)
        assertNull(provider.confirmOffers(offers.offers, RegionalPlatforms("DE", emptySet())).single().confirmedCountry)
    }

    @Test fun movieUnseenWorkerSequenceRepairsWatchlistAfterRemovingEveryTargetPlay() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        var seen = true
        var inWatchlist = false
        var historyRemovals = 0
        var watchlistAdds = 0
        val transport = ScriptedTransport { request ->
            when {
                request.url.contains("sync/history/movies/10") -> TrackingResponse(200, if (seen) "[{\"id\":1},{\"id\":2}]" else "[]")
                request.url.contains("sync/watchlist/movies") -> TrackingResponse(200, if (inWatchlist) "[{\"movie\":{\"ids\":{\"trakt\":10}}}]" else "[]",
                    headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "1"))
                request.url.endsWith("/sync/history/remove") -> { historyRemovals++; seen = false; throw TrackingException(TrackingFailure.NETWORK) }
                request.url.endsWith("/sync/watchlist") && request.method == "POST" -> { watchlistAdds++; inWatchlist = true; TrackingResponse(201, "{}") }
                else -> throw AssertionError(request.url)
            }
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope))))
        val movie = TrackingContent(TrackingIdentity("trakt", TrackingType.MOVIE, "10"), title = "Synthetic")
        provider.sendMutation(envelope, movie, TrackingAction.UNSEEN)
        val target = provider.readTarget(envelope, movie, TrackingAction.UNSEEN)
        assertTrue(provider.matches(target, TrackingAction.UNSEEN, movie))
        assertEquals(1, historyRemovals)
        assertEquals(1, watchlistAdds)
        provider.sendMutation(envelope, movie.copy(movieState = 0), TrackingAction.RESTORE_MOVIE_STATE)
        assertEquals(1, historyRemovals)
        assertEquals(1, watchlistAdds)
    }

    @Test fun reserveSkipsCompletedSeasonsAndBuildsSixAiredCandidatesAcrossSeasonBoundary() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val show = showJson(42, "Synthetic")
        val progress = JSONObject().put("seasons", JSONArray()
            .put(progressSeason(0, 2, 2, 2))
            .put(progressSeason(1, 3, 2, 3))
            .put(progressSeason(2, 6, 0, 6)))
        val seasonOne = JSONArray().put(episodeJson(103, 3, "2026-01-03T00:00:00Z"))
        val seasonTwo = JSONArray().apply {
            for (number in 1..6) put(episodeJson(200L + number, number, if (number == 5) "2026-10-28T13:00:00Z" else "2026-02-0${number}T00:00:00Z").put("season", 2))
        }
        val requestedSeasons = mutableListOf<Int>()
        val transport = ScriptedTransport { request ->
            when {
                request.url.contains("users/hidden/progress_watched") -> TrackingResponse(200, "[]", headers = paginationSingle())
                request.url.contains("shows/42/progress/watched") -> TrackingResponse(200, progress.toString())
                request.url.contains("shows/42?extended=full") -> TrackingResponse(200, show.toString())
                request.url.contains("shows/42/seasons/") -> {
                    val season = request.url.substringAfter("shows/42/seasons/").substringBefore('?').toInt()
                    requestedSeasons += season
                    TrackingResponse(200, (if (season == 1) seasonOne else seasonTwo).toString())
                }
                else -> TrackingResponse(200, "[]", headers = paginationSingle())
            }
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope)))) {
            Instant.parse("2026-10-28T12:00:00Z").toEpochMilli()
        }
        val showContent = TrackingContent(TrackingIdentity("trakt", TrackingType.SHOW, "42"), title = "Synthetic")
        val reserve = provider.reserve(envelope, showContent, emptyMap())
        assertEquals(6, reserve.size)
        assertEquals(listOf(103, 201, 202, 203, 204, 206).map(Int::toString), reserve.map { it.identity.id })
        assertEquals(listOf(1, 2), requestedSeasons)
    }

    @Test fun seasonUiOverlaysWatchedProgressOnUnpersonalizedSeasonMetadata() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val show = showJson(42, "Synthetic")
        val progress = JSONObject().put("seasons", JSONArray().put(JSONObject().put("number", 1).put("aired", 2).put("completed", 1)
            .put("episodes", JSONArray().put(JSONObject().put("number", 1).put("completed", true)).put(JSONObject().put("number", 2).put("completed", false)))))
        val metadata = JSONArray().put(episodeJson(101, 1, "2026-01-01T00:00:00Z")).put(episodeJson(102, 2, "2026-01-02T00:00:00Z"))
        val transport = ScriptedTransport { request ->
            when {
                request.url.contains("shows/42?extended=full") -> TrackingResponse(200, show.toString())
                request.url.contains("shows/42/progress/watched") -> TrackingResponse(200, progress.toString())
                request.url.contains("shows/42/seasons/1") -> {
                    assertTrue(request.url.contains("extended=full"))
                    TrackingResponse(200, metadata.toString())
                }
                else -> TrackingResponse(200, "[]")
            }
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope))))
        val content = TrackingContent(TrackingIdentity("trakt", TrackingType.EPISODE, "102"), parent = TrackingIdentity("trakt", TrackingType.SHOW, "42"), title = "Synthetic", season = 1)
        val episodes = provider.episodes(envelope, content)
        assertEquals(listOf(true, false), episodes.map { it.seen })
        assertEquals(listOf(101L, 102L), episodes.map { it.identity.id.toLong() })
    }

    @Test fun localNonSeenOverrideReopensOnlyItsCompletedSeason() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val show = showJson(42, "Synthetic")
        val progress = JSONObject().put("seasons", JSONArray().put(JSONObject().put("number", 1).put("aired", 1).put("completed", 1)
            .put("episodes", JSONArray().put(JSONObject().put("number", 1).put("completed", true).put("ids", JSONObject().put("trakt", 101))))))
        val metadata = JSONArray().put(episodeJson(101, 1, "2026-01-01T00:00:00Z"))
        var seasonReads = 0
        val transport = ScriptedTransport { request ->
            when {
                request.url.contains("users/hidden/progress_watched") -> TrackingResponse(200, "[]", headers = paginationSingle())
                request.url.contains("shows/42/progress/watched") -> TrackingResponse(200, progress.toString())
                request.url.contains("shows/42?extended=full") -> TrackingResponse(200, show.toString())
                request.url.contains("shows/42/seasons/1") -> { seasonReads++; TrackingResponse(200, metadata.toString()) }
                else -> throw AssertionError(request.url)
            }
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope)))) {
            Instant.parse("2026-10-28T12:00:00Z").toEpochMilli()
        }
        val content = TrackingContent(TrackingIdentity("trakt", TrackingType.SHOW, "42"), title = "Synthetic")
        val reserve = provider.reserve(envelope, content, mapOf("101" to 0))
        assertEquals(listOf("101"), reserve.map { it.identity.id })
        assertEquals(1, seasonReads)
        assertFalse(reserve.single().seen)
    }

    @Test fun identifyUsesDirectMovieObjectAndDoesNotConfuseCatalogWithMembership() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val directMovie = JSONObject().put("title", "Synthetic Movie").put("year", 2020).put("runtime", 123)
            .put("ids", JSONObject().put("trakt", 10).put("imdb", "tt-synthetic").put("tmdb", 77).put("tvdb", 88))
            .put("images", JSONObject().put("poster", JSONArray().put("images.example.test/poster.jpg")))
        val transport = ScriptedTransport { request ->
            when {
                request.url == "https://app.trakt.tv/" -> TrackingResponse(200, """import("https://app.trakt.tv/entry/app.one.js")""")
                request.url.endsWith("/entry/app.one.js") -> TrackingResponse(200, "\"nodes/0.bundle.js\"")
                request.url.endsWith("/nodes/0.bundle.js") -> TrackingResponse(200, "trakt-api-key=${"a".repeat(64)}")
                request.url.contains("search/movie") -> TrackingResponse(200, JSONArray().put(JSONObject().put("movie", directMovie)).toString(), headers = paginationSingle())
                request.url.contains("movies/10?extended=full") -> TrackingResponse(200, directMovie.toString())
                request.url.contains("sync/watchlist/movies") -> TrackingResponse(200, "[]", headers = paginationSingle())
                request.url.contains("sync/history/movies/10") -> TrackingResponse(200, "[]")
                else -> throw AssertionError(request.url)
            }
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope))))
        val local = UpNextItem(1, UpNextSourceEntry("synthetic.player"), UpNextContentType.MOVIE, "Synthetic Movie", year = 2020)
        val identified = provider.identify(envelope, local)!!
        assertEquals("10", identified.identity.id)
        assertEquals(AccountMembership.ABSENT, identified.membership)
        assertEquals(123L * 60_000L, identified.durationMillis)
        assertEquals("https://images.example.test/poster.jpg", identified.poster)
        assertEquals("tt-synthetic", identified.externalIds["imdb_id"])
        assertEquals("77", identified.externalIds["themoviedb_id"])
        assertEquals("88", identified.externalIds["thetvdb_id"])
    }

    @Test fun incompleteSearchPaginationCannotProveCatalogUniqueness() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val result = JSONObject().put("movie", JSONObject().put("title", "Synthetic Movie").put("ids", JSONObject().put("trakt", 10)))
        val transport = ScriptedTransport { request ->
            when {
                request.url == "https://app.trakt.tv/" -> TrackingResponse(200, """import("https://app.trakt.tv/entry/app.one.js")""")
                request.url.endsWith("/entry/app.one.js") -> TrackingResponse(200, "\"nodes/0.bundle.js\"")
                request.url.endsWith("/nodes/0.bundle.js") -> TrackingResponse(200, "trakt-api-key=${"a".repeat(64)}")
                request.url.contains("search/movie") -> TrackingResponse(200, JSONArray().put(result).toString(), headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "2"))
                else -> throw AssertionError(request.url)
            }
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope))))
        val local = UpNextItem(1, UpNextSourceEntry("synthetic.player"), UpNextContentType.MOVIE, "Synthetic Movie", year = 2020)
        assertNull(provider.identify(envelope, local))
        assertEquals(1, transport.requests.count { it.url.contains("search/movie") })
        assertFalse(transport.requests.any { it.url.contains("movies/10?") })
    }

    @Test fun startedShowRemainsAccountMemberWhenUpToDateAndAbsentFromWatchlist() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        val show = showJson(42, "Started")
        val transport = ScriptedTransport { request ->
            val body = when {
                request.url.contains("shows/42/progress/watched") -> """{"completed":12,"seasons":[]}"""
                else -> "[]"
            }
            TrackingResponse(200, body, headers = paginationSingle())
        }
        val provider = TraktProvider(api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope))))
        val content = TrackingContent(TrackingIdentity("trakt", TrackingType.SHOW, "42"), title = "Started")
        val target = provider.readTarget(envelope, content, TrackingAction.REMOVE)
        assertEquals(AccountMembership.PRESENT, target.membership)
        assertFalse(target.removableFromAccount)
        assertTrue(transport.requests.none { "sync/progress/up_next" in it.url })
    }

    @Test fun explicitInvalidClientRecoversOnlyForOnePublicGet() = runTest {
        var client = "a".repeat(64)
        var discoveries = 0
        val transport = ScriptedTransport { request ->
            when {
                request.url == "https://app.trakt.tv/" -> { discoveries++; TrackingResponse(200, """import("https://app.trakt.tv/entry/app.one.js")""") }
                request.url.endsWith("/entry/app.one.js") -> TrackingResponse(200, "\"nodes/0.bundle.js\"")
                request.url.endsWith("/nodes/0.bundle.js") -> TrackingResponse(200, "trakt-api-key=$client")
                request.url == "https://api.trakt.tv/public" && request.headers["trakt-api-key"] == "a".repeat(64) -> {
                    client = "b".repeat(64)
                    TrackingResponse(401, """{"error":"invalid_client"}""")
                }
                request.url == "https://api.trakt.tv/public" -> TrackingResponse(200, """{"ok":true}""")
                else -> throw AssertionError(request.url)
            }
        }
        val source = TraktConfigurationSource(transport, null, { 1 }, kotlinx.coroutines.Dispatchers.Unconfined)
        val api = TraktApi(transport, source, CoordinatedTrackingSessionStore(MemorySession(null)), kotlinx.coroutines.Dispatchers.Unconfined, { 1 })
        assertTrue(api.request("public").optBoolean("ok"))
        assertEquals(2, discoveries)
        assertEquals(2, transport.requests.count { it.url == "https://api.trakt.tv/public" })
    }

    @Test fun authenticatedInvalidClientRequiresReconnectWithoutTransplantingClient() = runTest {
        val originalClient = "a".repeat(64)
        val replacementClient = "b".repeat(64)
        val envelope = TraktSessionEnvelope(originalClient, "access", "refresh", 1_000_000, "binding").encode()
        var discoveries = 0
        val transport = ScriptedTransport { request ->
            when {
                request.url == "https://api.trakt.tv/sync/history" && request.method == "POST" -> TrackingResponse(403, """{"error":"invalid_client"}""")
                request.url == "https://app.trakt.tv/" -> { discoveries++; TrackingResponse(200, "import('/entry/app.fixture.js')") }
                request.url.endsWith("/entry/app.fixture.js") -> TrackingResponse(200, "'../nodes/0.fixture.js'")
                request.url.endsWith("/nodes/0.fixture.js") -> TrackingResponse(200, "?trakt-api-key=$replacementClient")
                else -> throw AssertionError(request.url)
            }
        }
        val source = TraktConfigurationSource(transport, null, { 1 }, kotlinx.coroutines.Dispatchers.Unconfined)
        val store = CoordinatedTrackingSessionStore(MemorySession(envelope))
        val api = TraktApi(transport, source, store, kotlinx.coroutines.Dispatchers.Unconfined, { 1 })
        try { api.request("sync/history", envelope, "POST", JSONObject()); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.ACCESS, error.failure) }
        assertEquals(1, discoveries)
        assertEquals(replacementClient, source.get().clientId)
        assertEquals(originalClient, transport.requests.single { it.url == "https://api.trakt.tv/sync/history" }.headers["trakt-api-key"])
        assertEquals(envelope, store.read())
    }

    @Test fun laterRequestsUseRefreshedTupleEvenWhenControllerPassesOldEnvelope() = runTest {
        val old = TraktSessionEnvelope("client", "old-access", "old-refresh", 1_000_000, "binding").encode()
        val store = CoordinatedTrackingSessionStore(MemorySession(old))
        var refreshes = 0
        var settingsCalls = 0
        val authorizations = mutableListOf<String?>()
        val transport = ScriptedTransport { request ->
            when (request.url) {
                "https://api.trakt.tv/users/settings" -> {
                    authorizations += request.headers["Authorization"]
                    settingsCalls++
                    if (settingsCalls == 1 || settingsCalls == 3) TrackingResponse(401, """{"error":"invalid_token"}""") else TrackingResponse(200, "{}")
                }
                "https://auth.trakt.tv/oauth/token" -> {
                    refreshes++
                    TrackingResponse(200, """{"access_token":"new-access-$refreshes","refresh_token":"new-refresh-$refreshes","expires_in":3600}""")
                }
                else -> throw AssertionError(request.url)
            }
        }
        val api = api(transport, store)
        api.request("users/settings", old)
        api.request("users/settings", old)
        assertEquals(2, refreshes)
        assertEquals(listOf("Bearer old-access", "Bearer new-access-1", "Bearer new-access-1", "Bearer new-access-2"), authorizations)
    }

    @Test fun explicitMutationTokenRefusalRefreshesButNeverReplaysTheMutation() = runTest {
        val old = TraktSessionEnvelope("client", "old-access", "old-refresh", 1_000_000, "binding").encode()
        val store = CoordinatedTrackingSessionStore(MemorySession(old))
        var mutations = 0
        var refreshes = 0
        val transport = ScriptedTransport { request ->
            when (request.url) {
                "https://api.trakt.tv/sync/history" -> { mutations++; TrackingResponse(401, """{"error":"invalid_token"}""") }
                "https://auth.trakt.tv/oauth/token" -> { refreshes++; TrackingResponse(200, """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600}""") }
                else -> throw AssertionError(request.url)
            }
        }
        val api = api(transport, store)
        try { api.request("sync/history", old, "POST", JSONObject()); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        assertEquals(1, refreshes)
        assertEquals(1, mutations)
        assertEquals("new-access", TraktSessionEnvelope.decode(store.read())?.accessToken)
    }

    @Test fun logoutDuringRefreshRevokesOrphanGrantWithoutRecreatingSession() = runTest {
        val old = TraktSessionEnvelope("client", "old-access", "old-refresh", 1, "binding").encode()
        val base = MemorySession(old)
        val store = CoordinatedTrackingSessionStore(base)
        var orphanRevocations = 0
        val transport = ScriptedTransport { request ->
            when (request.url) {
                "https://auth.trakt.tv/oauth/token" -> { base.value = null; TrackingResponse(200, """{"access_token":"orphan-access","refresh_token":"orphan-refresh","expires_in":3600}""") }
                "https://auth.trakt.tv/oauth/revoke" -> { orphanRevocations++; TrackingResponse(200, "{}") }
                else -> throw AssertionError(request.url)
            }
        }
        val api = api(transport, store)
        try { api.request("users/settings", old); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.ACCESS, error.failure) }
        assertNull(base.value)
        assertEquals(1, orphanRevocations)
        assertTrue(transport.requests.any { it.url == "https://auth.trakt.tv/oauth/revoke" && it.jsonBody?.contains("orphan-access") == true })
    }

    @Test fun genericHttpRefusalKeepsSessionAndNeverDiscoversReplacementConfiguration() = runTest {
        val envelope = TraktSessionEnvelope("original-client", "access", "refresh", 1_000_000, "binding").encode()
        val store = CoordinatedTrackingSessionStore(MemorySession(envelope))
        var calls = 0
        val transport = ScriptedTransport { request ->
            calls++
            TrackingResponse(403, "not-json")
        }
        val api = api(transport, store)
        try { api.request("users/settings", envelope); throw AssertionError() }
        catch (error: TraktResponseException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        assertEquals(envelope, store.read())
        assertEquals(1, calls)
    }

    @Test fun retryAfterBeyondMutationBudgetReturnsBoundedRetryWithoutHoldingARequest() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        var calls = 0
        val transport = ScriptedTransport { request ->
            calls++
            TrackingResponse(429, "{}", retryAfterMillis = 60_000)
        }
        val api = api(transport, CoordinatedTrackingSessionStore(MemorySession(envelope)))
        repeat(2) {
            try { api.request("sync/history", envelope, "POST", JSONObject()); throw AssertionError() }
            catch (error: TrackingRetryException) { assertTrue(error.retryAfterMillis >= 59_000) }
        }
        assertEquals(1, calls)
    }

    @Test fun malformedUnknownVersionEnvelopeFailsProtocolRatherThanSelectingBetaSeries() {
        try { TraktSessionEnvelope.binding("trakt:v2:unknown"); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.PROTOCOL, error.failure) }
    }

    @Test fun malformedTraktEnvelopeNeverRoutesAsLegacyBetaSeries() {
        try { TraktSessionEnvelope.provider("trakt:v7:broken"); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.PROTOCOL, error.failure) }
    }

    private fun showJson(id: Long, title: String) = JSONObject().put("title", title).put("ids", JSONObject().put("trakt", id))
    private fun episodeJson(id: Long, number: Int, aired: String) = JSONObject().put("title", "Episode $number").put("season", 1).put("number", number)
        .put("first_aired", aired).put("ids", JSONObject().put("trakt", id))
    private fun progressSeason(season: Int, aired: Int, completed: Int, episodes: Int) = JSONObject().put("number", season).put("aired", aired).put("completed", completed)
        .put("episodes", JSONArray().apply { for (number in 1..episodes) put(JSONObject().put("number", number).put("completed", number <= completed)) })
    private fun paginationSingle() = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "1")

    @Test fun seenRetryWaitsForAReadableTargetAndConvergesWithoutDuplicatingAnAppliedPlay() = runTest {
        val envelope = TraktSessionEnvelope("client", "access", "refresh", 1_000_000, "binding").encode()
        var seen = false
        var sends = 0
        var historyReadable = true
        val transport = ScriptedTransport { request ->
            when {
                request.url.contains("sync/history/movies/10") -> {
                    if (!historyReadable) throw TrackingException(TrackingFailure.NETWORK)
                    TrackingResponse(200, if (seen) "[{\"watched_at\":\"2026-01-01T00:00:00Z\"}]" else "[]")
                }
                request.url.contains("sync/watchlist/movies") -> TrackingResponse(200, "[]", headers = mapOf("X-Pagination-Page" to "1", "X-Pagination-Page-Count" to "1"))
                request.url.endsWith("/sync/history") && request.method == "POST" -> {
                    if (++sends == 1) throw TrackingException(TrackingFailure.NETWORK)
                    seen = true
                    TrackingResponse(201, "{}")
                }
                else -> throw AssertionError(request.url)
            }
        }
        val store = CoordinatedTrackingSessionStore(MemorySession(envelope))
        val api = api(transport, store)
        val provider = TraktProvider(api)
        val movie = TrackingContent(TrackingIdentity("trakt", TrackingType.MOVIE, "10"), title = "Synthetic")
        try { provider.sendMutation(envelope, movie, TrackingAction.SEEN); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        val restoredProvider = TraktProvider(api(transport, store))
        historyReadable = false
        try { restoredProvider.sendMutation(envelope, movie, TrackingAction.SEEN); throw AssertionError() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        assertEquals(1, sends)
        historyReadable = true
        restoredProvider.sendMutation(envelope, movie, TrackingAction.SEEN)
        assertEquals(2, sends)
        restoredProvider.sendMutation(envelope, movie, TrackingAction.SEEN)
        assertEquals(2, sends)
        assertEquals(envelope, store.read())
    }

    private fun api(transport: ScriptedTransport, store: CoordinatedTrackingSessionStore) = TraktApi(transport,
        TraktConfigurationSource(transport, null, { 10_000 }, kotlinx.coroutines.Dispatchers.Unconfined), store,
        kotlinx.coroutines.Dispatchers.Unconfined, { 10_000 })

    private class MemorySession(var value: String?) : TrackingSessionStore {
        var writes = 0
        var reads = 0
        var failWriteNumber: Int? = null
        override suspend fun read(): String? { reads++; return value }
        override suspend fun write(token: String?) {
            writes++
            if (writes == failWriteNumber) throw IllegalStateException()
            value = token
        }
    }

    private class ScriptedTransport(private val responder: (TrackingRequest) -> TrackingResponse) : TrackingTransport {
        val requests = mutableListOf<TrackingRequest>()
        override suspend fun execute(request: TrackingRequest): TrackingResponse {
            requests += request
            return responder(request)
        }
    }
}
