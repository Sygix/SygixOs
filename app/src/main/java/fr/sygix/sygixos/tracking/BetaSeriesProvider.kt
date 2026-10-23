/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.time.LocalDate
import org.json.JSONObject

class BetaSeriesProvider(private val api: BetaSeriesApi, private val today: () -> LocalDate = LocalDate::now) : WatchTrackingProvider {
    private val resolver = BoundedIdentityResolver(::search)

    private suspend fun search(query: ContentQuery): List<CatalogCandidate> {
        val movie = query.type == TrackingType.MOVIE
        val json = api.request(if (movie) "movies/search" else "shows/search", fields = mapOf("title" to query.title, "limit" to "100"))
        val values = json.optJSONArray(if (movie) "movies" else "shows") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        if (json.optInt("total", values.length()) > values.length()) return emptyList()
        return (0 until values.length()).map { index ->
            val value = values.getJSONObject(index)
            CatalogCandidate(TrackingIdentity("betaseries", query.type, value.requiredId()), value.text("title").orEmpty(),
                value.optInt(if (movie) "production_year" else "creation").takeIf { it > 0 }, externalIds = externalIds(value))
        }
    }

    suspend fun identify(token: String, item: fr.sygix.sygixos.model.UpNextItem, memberships: Set<TrackingIdentity>): TrackingContent? {
        val movie = item.type == fr.sygix.sygixos.model.UpNextContentType.MOVIE
        val candidate = resolver.resolve(ContentQuery(if (movie) TrackingType.MOVIE else TrackingType.SHOW, item.displayTitle, item.year,
            item.season?.toIntOrNull(), item.episode?.toIntOrNull(), item.externalIds)) ?: return null
        val membership = if (candidate.identity in memberships) AccountMembership.PRESENT else AccountMembership.ABSENT
        if (movie) {
            val value = api.request("movies/movie", token, fields = mapOf("id" to candidate.identity.id)).optJSONObject("movie") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            return movie(value).copy(membership = membership)
        }
        val show = api.request("shows/display", token, fields = mapOf("id" to candidate.identity.id)).optJSONObject("show") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val season = item.season?.toIntOrNull()
        val episode = item.episode?.toIntOrNull()
        if (season == null || episode == null) return if (membership == AccountMembership.ABSENT)
            TrackingContent(candidate.identity, title = candidate.title, externalIds = candidate.externalIds, membership = membership) else null
        val response = api.request("shows/episodes", token, fields = mapOf("id" to candidate.identity.id, "season" to season.toString(), "episode" to episode.toString()))
        val values = response.optJSONArray("episodes") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val selected = (0 until values.length()).map { values.getJSONObject(it) }.filter { it.optInt("season", -1) == season && it.optInt("episode", -1) == episode }.singleOrNull() ?: return null
        val exact = api.request("episodes/display", token, fields = mapOf("id" to selected.requiredId())).optJSONObject("episode") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return episode(show, exact).copy(membership = membership)
    }
    override suspend fun account(token: String): TrackingAccount {
        val member = api.request("members/infos", token).optJSONObject("member") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return TrackingAccount(member.requiredId(), member.text("login") ?: throw TrackingException(TrackingFailure.PROTOCOL))
    }

    override suspend fun revoke(token: String) { api.request("members/destroy", token, "POST") }

    override suspend fun snapshot(token: String): TrackingSnapshot = kotlinx.coroutines.withTimeoutOrNull(90_000) {
        val shows = api.pages("shows/member", "shows", token).associateBy { it.requiredId() }
        val movies = (0..2).flatMap { state -> api.pages("movies/member", "movies", token, mapOf("state" to state.toString())) }.distinctBy { it.requiredId() }
        val episodes = api.pages("episodes/list", "shows", token, mapOf("released" to "1", "showsLimit" to "100", "limit" to "100"))
        val contents = episodes.mapNotNull { group ->
            val show = shows[group.requiredId()] ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            when (archiveState(show)) {
                true -> return@mapNotNull null
                null -> throw TrackingException(TrackingFailure.INCOMPLETE)
                false -> Unit
            }
            val unseen = group.optJSONArray("unseen") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            val next = (0 until unseen.length()).map { unseen.getJSONObject(it) }.filter {
                val date = it.text("date")?.let { value -> runCatching { LocalDate.parse(value.take(10)) }.getOrNull() }
                date != null && !date.isAfter(today()) && it.optJSONObject("user")?.optBoolean("seen") != true
            }.minWithOrNull(compareBy<JSONObject> { it.optInt("season") }.thenBy { it.optInt("episode") }) ?: return@mapNotNull null
            episode(show, next)
        } + movies.filter { it.optJSONObject("user")?.optInt("status", -1) == 0 }.map(::movie)
        val memberships = shows.keys.map { TrackingIdentity("betaseries", TrackingType.SHOW, it) } + movies.map { TrackingIdentity("betaseries", TrackingType.MOVIE, it.requiredId()) }
        TrackingSnapshot(contents.distinctBy { it.parent }, memberships.toSet())
    } ?: throw TrackingException(TrackingFailure.INCOMPLETE)

    suspend fun episodes(token: String, content: TrackingContent): List<TrackingContent> {
        val show = api.request("shows/display", token, fields = mapOf("id" to content.parent.id)).getJSONObject("show")
        return api.pages("shows/episodes", "episodes", token, mapOf("id" to content.parent.id)).filter { value ->
            value.text("date")?.let { runCatching { !LocalDate.parse(it.take(10)).isAfter(today()) }.getOrDefault(false) } == true
        }.map { episode(show, it) }
    }

    suspend fun mutate(token: String, content: TrackingContent, action: TrackingAction) {
        kotlinx.coroutines.withTimeoutOrNull(90_000) {
            var failure: Exception? = null
            try { sendMutation(token, content, action) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { failure = error }
            try { verify(token, content, action) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { throw (failure ?: error) }
        } ?: throw TrackingException(TrackingFailure.NETWORK)
    }

    private suspend fun sendMutation(token: String, content: TrackingContent, action: TrackingAction) {
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
            TrackingAction.RESTORE_MOVIE_STATE -> api.request("movies/movie", token, "POST", mapOf("id" to target, "state" to content.movieState.toString()))
        }
    }

    private suspend fun verify(token: String, content: TrackingContent, action: TrackingAction) {
        if (!statusMatches(token, content, action)) throw TrackingException(TrackingFailure.PROTOCOL)
    }

    suspend fun statusMatches(token: String, content: TrackingContent, action: TrackingAction): Boolean {
        val movie = content.identity.type == TrackingType.MOVIE
        val confirmed = when (action) {
            TrackingAction.ADD, TrackingAction.REMOVE -> {
                val members = if (movie) (0..2).flatMap { state -> api.pages("movies/member", "movies", token, mapOf("state" to state.toString())) }
                    else api.pages("shows/member", "shows", token)
                members.any { it.requiredId() == content.parent.id } == (action == TrackingAction.ADD)
            }
            TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE -> {
                val show = api.request("shows/display", token, fields = mapOf("id" to content.parent.id)).optJSONObject("show")
                show?.let(::archiveState) == (action == TrackingAction.ARCHIVE)
            }
            TrackingAction.SEEN, TrackingAction.UNSEEN, TrackingAction.RESTORE_MOVIE_STATE -> {
                val user = api.request(if (movie) "movies/movie" else "episodes/display", token, fields = mapOf("id" to content.identity.id))
                    .optJSONObject(if (movie) "movie" else "episode")?.optJSONObject("user")
                if (movie) user?.has("status") == true && user.optInt("status", -1) == when (action) {
                    TrackingAction.SEEN -> 1
                    TrackingAction.UNSEEN -> 0
                    else -> content.movieState
                } else user?.has("seen") == true && user.optBoolean("seen") == (action == TrackingAction.SEEN)
            }
        }
        return confirmed
    }

    private fun episode(show: JSONObject, value: JSONObject): TrackingContent {
        val images = show.optJSONObject("images")
        return TrackingContent(
            identity = TrackingIdentity("betaseries", TrackingType.EPISODE, value.requiredId()),
            parent = TrackingIdentity("betaseries", TrackingType.SHOW, show.requiredId()),
            title = show.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL),
            episodeTitle = value.text("title"), season = value.optInt("season", -1).takeIf { it >= 0 }, episode = value.optInt("episode", -1).takeIf { it > 0 },
            image = images?.text("show"), poster = images?.text("poster"),
            durationMillis = value.optLong("duration").takeIf { it > 0 }?.times(60_000),
            externalIds = externalIds(show), seen = value.optJSONObject("user")?.optBoolean("seen") == true,
            archived = archiveState(show),
            offers = BetaSeriesOffers.show(show),
        )
    }

    private fun movie(value: JSONObject): TrackingContent = TrackingContent(
        identity = TrackingIdentity("betaseries", TrackingType.MOVIE, value.requiredId()),
        title = value.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL),
        year = value.optInt("production_year").takeIf { it > 0 }, image = value.text("backdrop"), poster = value.text("poster"),
        durationMillis = value.optLong("length").takeIf { it > 0 }?.times(60_000), externalIds = externalIds(value),
        movieState = value.optJSONObject("user")?.optInt("status") ?: 0,
        seen = value.optJSONObject("user")?.optInt("status") == 1,
        offers = BetaSeriesOffers.movie(value),
    )

    private fun externalIds(value: JSONObject): Map<String, String> = listOf("thetvdb_id", "imdb_id", "themoviedb_id").mapNotNull { key ->
        value.text(key)?.takeUnless { it == "0" }?.let { key to it }
    }.toMap()

    private fun archiveState(show: JSONObject): Boolean? = when (val value = show.optJSONObject("user")?.opt("archived")) {
        is Boolean -> value
        is Number -> when (value.toInt()) { 0 -> false; 1 -> true; else -> null }
        else -> value?.toString()?.toBooleanStrictOrNull()
    }
}
