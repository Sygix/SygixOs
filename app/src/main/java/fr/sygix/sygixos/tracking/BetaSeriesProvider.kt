/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.time.LocalDate
import org.json.JSONObject

class BetaSeriesProvider(private val api: BetaSeriesApi, private val today: () -> LocalDate = LocalDate::now) : WatchTrackingProvider {
    override suspend fun account(token: String): TrackingAccount {
        val member = api.request("members/infos", token).optJSONObject("member") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return TrackingAccount(member.requiredId(), member.text("login") ?: throw TrackingException(TrackingFailure.PROTOCOL))
    }

    override suspend fun revoke(token: String) { api.request("members/destroy", token, "POST") }

    override suspend fun snapshot(token: String): TrackingSnapshot {
        val shows = api.pages("shows/member", "shows", token).associateBy { it.requiredId() }
        val episodes = api.pages("episodes/list", "shows", token, mapOf("released" to "1", "showsLimit" to "100", "limit" to "100"))
        val contents = episodes.mapNotNull { group ->
            val show = shows[group.requiredId()] ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            if (show.optJSONObject("user")?.optBoolean("archived") == true) return@mapNotNull null
            val unseen = group.optJSONArray("unseen") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            val next = (0 until unseen.length()).map { unseen.getJSONObject(it) }.filter {
                val date = it.text("date")?.let { value -> runCatching { LocalDate.parse(value.take(10)) }.getOrNull() }
                date != null && !date.isAfter(today()) && it.optJSONObject("user")?.optBoolean("seen") != true
            }.minWithOrNull(compareBy<JSONObject> { it.optInt("season") }.thenBy { it.optInt("episode") }) ?: return@mapNotNull null
            episode(show, next)
        } + api.pages("movies/member", "movies", token, mapOf("state" to "0")).map(::movie)
        return TrackingSnapshot(contents.distinctBy { it.parent })
    }

    suspend fun episodes(token: String, content: TrackingContent): List<TrackingContent> {
        val show = api.request("shows/display", token, fields = mapOf("id" to content.parent.id)).getJSONObject("show")
        return api.pages("shows/episodes", "episodes", token, mapOf("id" to content.parent.id)).filter { value ->
            value.text("date")?.let { runCatching { !LocalDate.parse(it.take(10)).isAfter(today()) }.getOrDefault(false) } == true
        }.map { episode(show, it) }
    }

    suspend fun mutate(token: String, content: TrackingContent, action: TrackingAction) {
        val movie = content.identity.type == TrackingType.MOVIE
        val target = content.identity.id
        val parent = content.parent.id
        when (action) {
            TrackingAction.SEEN -> if (movie) api.request("movies/movie", token, "POST", mapOf("id" to target, "state" to "1"))
                else api.request("episodes/watched", token, "POST", mapOf("id" to target, "bulk" to "false"))
            TrackingAction.UNSEEN -> if (movie) api.request("movies/movie", token, "POST", mapOf("id" to target, "state" to "0"))
                else api.request("episodes/watched", token, "DELETE", mapOf("id" to target))
            TrackingAction.ARCHIVE -> api.request("shows/archive", token, "POST", mapOf("id" to parent))
            TrackingAction.UNARCHIVE -> api.request("shows/archive", token, "DELETE", mapOf("id" to parent))
            TrackingAction.REMOVE -> api.request(if (movie) "movies/movie" else "shows/show", token, "DELETE", mapOf("id" to parent))
            TrackingAction.ADD -> api.request(if (movie) "movies/movie" else "shows/show", token, "POST", mapOf("id" to parent) + if (movie) mapOf("state" to "0") else emptyMap())
        }
    }

    private fun episode(show: JSONObject, value: JSONObject): TrackingContent {
        val images = show.optJSONObject("images")
        return TrackingContent(
            identity = TrackingIdentity("betaseries", TrackingType.EPISODE, value.requiredId()),
            parent = TrackingIdentity("betaseries", TrackingType.SHOW, show.requiredId()),
            title = show.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL),
            episodeTitle = value.text("title"), season = value.optInt("season").takeIf { it >= 0 }, episode = value.optInt("episode").takeIf { it > 0 },
            image = images?.text("show"), poster = images?.text("poster"),
            durationMillis = value.optLong("duration").takeIf { it > 0 }?.times(60_000),
            externalIds = externalIds(show), seen = value.optJSONObject("user")?.optBoolean("seen") == true,
        )
    }

    private fun movie(value: JSONObject): TrackingContent = TrackingContent(
        identity = TrackingIdentity("betaseries", TrackingType.MOVIE, value.requiredId()),
        title = value.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL),
        year = value.optInt("production_year").takeIf { it > 0 }, image = value.text("backdrop"), poster = value.text("poster"),
        durationMillis = value.optLong("length").takeIf { it > 0 }?.times(60_000), externalIds = externalIds(value),
        movieState = value.optJSONObject("user")?.optInt("status") ?: 0,
    )

    private fun externalIds(value: JSONObject): Map<String, String> = listOf("thetvdb_id", "imdb_id", "themoviedb_id").mapNotNull { key ->
        value.text(key)?.takeUnless { it == "0" }?.let { key to it }
    }.toMap()
}

enum class TrackingAction { ADD, SEEN, UNSEEN, ARCHIVE, UNARCHIVE, REMOVE }
