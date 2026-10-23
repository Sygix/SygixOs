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

internal class TrackingMovieFixture(scope: CoroutineScope, clock: () -> Long) {
    private val context: android.content.Context = ApplicationProvider.getApplicationContext()
    var removed = false
    var state = 0
    var removals = 0
    var writes = 0
    private fun movie() = "{\"id\":42,\"title\":\"Fixture movie\",\"production_year\":2020,\"user\":{\"status\":$state}}"
    val runtime = TrackingRuntime(context, AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(testDataStore(scope))), scope,
        transport = TrackingTransport { request ->
            val uri = java.net.URI(request.url)
            val query = uri.rawQuery.orEmpty().split('&').mapNotNull { field -> field.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
            when (uri.path) {
                "/" -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                "/members/infos" -> TrackingResponse(200, "{\"member\":{\"id\":1,\"login\":\"Fixture\"}}")
                "/shows/member", "/episodes/list" -> TrackingResponse(200, "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}")
                "/movies/member" -> {
                    val movies = if (!removed && query["state"]?.toIntOrNull() == state) listOf(movie()) else emptyList()
                    TrackingResponse(200, "{\"movies\":[${movies.joinToString(",")}],\"total\":${movies.size},\"totalMissingMovies\":0}")
                }
                "/movies/movie" -> when (request.method) {
                    "DELETE" -> { removed = true; ++removals; TrackingResponse(200, "{}") }
                    "POST" -> { ++writes; removed = false; state = requireNotNull(request.fields["state"]).toInt(); TrackingResponse(200, "{}") }
                    else -> TrackingResponse(200, "{\"movie\":${movie()}}")
                }
                else -> error("Unexpected movie fixture endpoint")
            }
        }, sessionStore = object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) {}
        }, preferences = TrackingPreferencesFixture(), clock = clock, nanoTime = { clock() * 1_000_000 })
    val content: TrackingContent get() = runtime.controller.state.value.contents.single()
}
