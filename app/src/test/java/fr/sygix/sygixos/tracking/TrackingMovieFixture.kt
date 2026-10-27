/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.testDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.map

internal class TrackingMovieFixture(scope: CoroutineScope, savedToken: String? = "fixture-token", onArtwork: suspend (List<Pair<fr.sygix.sygixos.model.ProgramArtwork, String>>) -> Unit = {}, clock: () -> Long) {
    private val context: android.content.Context = ApplicationProvider.getApplicationContext()
    var removed = false
    var state = 0
    var removals = 0
    var writes = 0
    var lists = 0
    var incomplete = false
    var mutationFailure: TrackingFailure? = null
    var storageFailure: TrackingFailure? = null
    var accountFailure: TrackingFailure? = null
    var movieListResponse: TrackingResponse? = null
    var sessionWrites = 0
    var revocations = 0
    val requests = mutableListOf<TrackingRequest>()
    val artworkCalls = mutableListOf<List<Pair<fr.sygix.sygixos.model.ProgramArtwork, String>>>()
    val playbackApps = kotlinx.coroutines.flow.MutableStateFlow(listOf(PlaybackApp("org.jellyfin.androidtv", "Jellyfin", true)))
    val launches = mutableListOf<PlaybackRequest>()
    val projectionDispatcher = kotlinx.coroutines.test.StandardTestDispatcher(scope.coroutineContext[kotlinx.coroutines.test.TestCoroutineScheduler])
    val platformCountries = mutableListOf<String>()
    var platformGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
    private fun movie() = "{\"id\":42,\"title\":\"Fixture movie\",\"production_year\":2020,\"user\":{\"status\":$state,\"in_account\":${!removed}},\"platforms_svod\":[{\"id\":1,\"name\":\"Fixture service\"}]}"
    val runtime = TrackingRuntime(context, AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(testDataStore(scope))), scope,
        onArtwork = { artworkCalls += it; onArtwork(it) },
        transport = TrackingTransport { request ->
            requests += request
            val uri = java.net.URI(request.url)
            val query = uri.rawQuery.orEmpty().split('&').mapNotNull { field -> field.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
            when (uri.path) {
                "/" -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                "/members/infos" -> { accountFailure?.let { throw TrackingException(it) }; TrackingResponse(200, "{\"member\":{\"id\":1,\"login\":\"Fixture\"}}") }
                "/members/destroy" -> { ++revocations; TrackingResponse(200, "{}") }
                "/shows/member", "/episodes/list" -> { ++lists; if (incomplete && uri.path == "/episodes/list") throw TrackingException(TrackingFailure.INCOMPLETE); TrackingResponse(200, "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}") }
                "/platforms/list" -> {
                    val country = query.getValue("country")
                    platformCountries += country
                    platformGate?.await()
                    TrackingResponse(200, "{\"platforms\":{\"svod\":[{\"id\":${if (country == "fr") 1 else 2}}]}}")
                }
                "/movies/member" -> {
                    val movies = if (!removed && query["state"]?.toIntOrNull() == state) listOf(movie()) else emptyList()
                    movieListResponse ?: TrackingResponse(200, "{\"movies\":[${movies.joinToString(",")}],\"total\":${movies.size},\"totalMissingMovies\":0}")
                }
                "/movies/movie" -> when (request.method) {
                    "DELETE" -> { mutationFailure?.let { throw TrackingException(it) }; removed = true; ++removals; TrackingResponse(200, "{}") }
                    "POST" -> { mutationFailure?.let { throw TrackingException(it) }; ++writes; removed = false; state = requireNotNull(request.fields["state"]).toInt(); TrackingResponse(200, "{}") }
                    else -> TrackingResponse(200, "{\"movie\":${movie()}}")
                }
                else -> error("Unexpected movie fixture endpoint")
            }
        }, sessionStore = object : TrackingSessionStore {
            override suspend fun read(): String? { storageFailure?.let { throw TrackingException(it) }; return savedToken }
            override suspend fun write(token: String?) { ++sessionWrites }
        }, preferences = TrackingPreferencesFixture(), playbackApps = object : PlaybackAppResolver {
            override suspend fun installed() = playbackApps.value
            override fun changes() = playbackApps.map { Unit }
        }, playbackLauncher = object : PlaybackLauncher {
            override fun launch(app: PlaybackApp, request: PlaybackRequest): PlaybackPrecision { launches += request; return PlaybackPrecision.APPLICATION }
        }, clock = clock, nanoTime = { clock() * 1_000_000 }, wallClock = { 1_800_000_000_000L },
        parserDispatcher = projectionDispatcher, configurationFile = null, identityFile = null)
    val content: TrackingContent get() = runtime.controller.state.value.contents.single()
}
