/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.time.LocalDate
import org.json.JSONObject

class BetaSeriesProvider(private val api: BetaSeriesApi, private val today: () -> LocalDate) : WatchTrackingProvider {
    private val resolver = BoundedIdentityResolver(::search)

    private suspend fun search(query: ContentQuery): List<CatalogCandidate> {
        val movie = query.type == TrackingType.MOVIE
        val json = api.request(if (movie) "movies/search" else "shows/search", fields = mapOf("title" to query.title, "limit" to "5"))
        val values = json.optJSONArray(if (movie) "movies" else "shows") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        if (json.optInt("total", values.length()) > values.length()) return emptyList()
        return (0 until values.length()).map { index ->
            val value = values.getJSONObject(index)
            CatalogCandidate(TrackingIdentity("betaseries", query.type, value.requiredId()), value.text("title").orEmpty(),
                value.optInt(if (movie) "production_year" else "creation").takeIf { it > 0 }, externalIds = externalIds(value))
        }
    }

    suspend fun identify(token: String, item: fr.sygix.sygixos.model.UpNextItem, known: LocalTrackingIdentity? = null): TrackingContent? =
        kotlinx.coroutines.withContext(api.dispatcher) { identifyContent(token, item, known) }

    private suspend fun identifyContent(token: String, item: fr.sygix.sygixos.model.UpNextItem, known: LocalTrackingIdentity?): TrackingContent? {
        val movie = item.type == fr.sygix.sygixos.model.UpNextContentType.MOVIE
        val title = if (movie) item.displayTitle else seriesTitle(item)
        val candidate = known?.let { CatalogCandidate(it.parent, title, item.year, externalIds = item.externalIds) }
            ?: resolver.resolve(ContentQuery(if (movie) TrackingType.MOVIE else TrackingType.SHOW, title, item.year,
                item.season?.toIntOrNull(), item.episode?.toIntOrNull(), item.externalIds)) ?: return null
        if (movie) {
            val value = api.request("movies/movie", token, fields = mapOf("id" to candidate.identity.id)).optJSONObject("movie") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            return movie(value).copy(membership = membership(value))
        }
        val show = api.request("shows/display", token, fields = mapOf("id" to candidate.identity.id)).optJSONObject("show") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val membership = membership(show)
        if (known?.identity?.type == TrackingType.EPISODE) {
            val exact = api.request("episodes/display", token, fields = mapOf("id" to known.identity.id)).optJSONObject("episode") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            return episode(show, exact).copy(membership = membership)
        }
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

    suspend fun platforms(country: String): RegionalPlatforms {
        val groups = api.request("platforms/list", fields = mapOf("country" to country.lowercase(java.util.Locale.ROOT))).optJSONObject("platforms")
            ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val ids = groups.keys().asSequence().flatMap { key ->
            val values = groups.optJSONArray(key)
            (0 until (values?.length() ?: 0)).asSequence().mapNotNull { values?.optJSONObject(it)?.text("id") }
        }.toSet()
        return RegionalPlatforms(country, ids)
    }

    override suspend fun snapshot(token: String): TrackingSnapshot = kotlinx.coroutines.withContext(api.dispatcher) { kotlinx.coroutines.withTimeoutOrNull(90_000) {
        var failure: TrackingFailure? = null
        suspend fun pages(path: String, key: String, fields: Map<String, String> = emptyMap()): List<JSONObject> = try { api.pages(path, key, token, fields) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (error.trackingFailure() == TrackingFailure.ACCESS) throw error
            failure = TrackingFailure.INCOMPLETE
            (error as? TrackingPagesException)?.items.orEmpty()
        }
        val shows = pages("shows/member", "shows").associateBy { it.requiredId() }
        val movies = pages("movies/member", "movies", mapOf("state" to "0"))
        val episodes = pages("episodes/list", "shows", mapOf("released" to "1", "showsLimit" to "100", "limit" to "6"))
        val contents = episodes.flatMap { group ->
            val show = shows[group.requiredId()]
                ?: return@flatMap listOf(TrackingContent(TrackingIdentity("betaseries", TrackingType.SHOW, group.requiredId()), title = group.text("title").orEmpty(), membership = AccountMembership.UNKNOWN, failure = TrackingFailure.INCOMPLETE))
            when (archiveState(show)) {
                true -> return@flatMap emptyList()
                null -> return@flatMap listOf(TrackingContent(TrackingIdentity("betaseries", TrackingType.SHOW, show.requiredId()), title = show.text("title").orEmpty(), membership = AccountMembership.UNKNOWN, failure = TrackingFailure.INCOMPLETE))
                false -> Unit
            }
            val unseen = group.optJSONArray("unseen") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            (0 until unseen.length()).map { unseen.getJSONObject(it) }.filter {
                val date = it.text("date")?.let { value -> runCatching { LocalDate.parse(value.take(10)) }.getOrNull() }
                if (date == null) failure = TrackingFailure.INCOMPLETE
                date != null && !date.isAfter(today()) && it.optJSONObject("user")?.optBoolean("seen") != true
            }.sortedWith(compareBy<JSONObject> { it.optInt("season") }.thenBy { it.optInt("episode") }).take(6).map {
                val content = episode(show, it)
                if (content.season == null || content.episode == null) { failure = TrackingFailure.INCOMPLETE; content.copy(failure = TrackingFailure.INCOMPLETE) }
                else content
            }
        } + movies.filter { it.optJSONObject("user")?.optInt("status", -1) == 0 }.map(::movie)
        TrackingSnapshot(contents.distinctBy { it.identity }, failure)
    } ?: throw TrackingException(TrackingFailure.INCOMPLETE) }

    suspend fun episodes(token: String, content: TrackingContent): List<TrackingContent> = kotlinx.coroutines.withContext(api.dispatcher) {
        val show = api.request("shows/display", token, fields = mapOf("id" to content.parent.id)).getJSONObject("show")
        showEpisodes(token, content.parent.id, content.season ?: 1).filter { value ->
            value.text("date")?.let { runCatching { !LocalDate.parse(it.take(10)).isAfter(today()) }.getOrDefault(false) } == true
        }.map { episode(show, it) }
    }

    suspend fun seasons(token: String, content: TrackingContent): List<Int> {
        val values = api.request("shows/seasons", token, fields = mapOf("id" to content.parent.id)).optJSONArray("seasons")
            ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return (0 until values.length()).mapNotNull { values.optJSONObject(it)?.optInt("number", -1)?.takeIf { number -> number >= 0 } }.distinct().sorted()
    }

    private suspend fun showEpisodes(token: String, id: String, season: Int): List<JSONObject> {
        val values = api.request("shows/episodes", token, fields = mapOf("id" to id, "season" to season.toString())).optJSONArray("episodes")
            ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return (0 until values.length()).map { values.getJSONObject(it) }
    }

    private fun seriesTitle(item: fr.sygix.sygixos.model.UpNextItem): String = item.displayTitle
        .substringBefore(episodeMarker.find(item.displayTitle)?.value ?: "\u0000").trim().trimEnd('-', '–', '—', ':', '·').trim()

    private val episodeMarker = Regex("""(?i)\bS\d{1,3}\s*E\d{1,4}\b""")

    suspend fun mutate(token: String, content: TrackingContent, action: TrackingAction): List<TrackingContent> =
        kotlinx.coroutines.withTimeoutOrNull(90_000) {
            var failure: Exception? = null
            try { sendMutation(token, content, action) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { failure = error }
            try { verify(token, content, action) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { throw (failure ?: error) }
        } ?: throw TrackingException(TrackingFailure.NETWORK)

    internal suspend fun sendMutation(token: String, content: TrackingContent, action: TrackingAction) {
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

    suspend fun verify(token: String, content: TrackingContent, action: TrackingAction): List<TrackingContent> {
        val entry = readTarget(token, content, action)
        if (!matches(entry, action, content)) throw TrackingException(TrackingFailure.INCOMPLETE)
        val next = if (content.identity.type != TrackingType.MOVIE && action == TrackingAction.SEEN && content.parent.type == TrackingType.SHOW) next(token, content) else null
        return listOfNotNull(entry.copy(sessionGeneration = content.sessionGeneration), next?.takeIf { it.identity != entry.identity })
    }

    internal suspend fun readTarget(token: String, content: TrackingContent, action: TrackingAction): TrackingContent {
        val movie = content.identity.type == TrackingType.MOVIE
        val showAction = !movie && action in setOf(TrackingAction.ADD, TrackingAction.REMOVE, TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE)
        val path = if (movie) "movies/movie" else if (showAction) "shows/display" else "episodes/display"
        val key = if (movie) "movie" else if (showAction) "show" else "episode"
        val value = api.request(path, token, fields = mapOf("id" to if (showAction) content.parent.id else content.identity.id))
            .optJSONObject(key) ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val user = value.optJSONObject("user")
        val entry = when {
            movie -> content.copy(seen = user?.optInt("status", -1) == 1, movieState = user?.optInt("status", -1) ?: -1,
                membership = if (action in setOf(TrackingAction.ADD, TrackingAction.REMOVE)) membership(value) else content.membership)
            showAction -> content.copy(archived = archiveState(value), membership = if (action in setOf(TrackingAction.ADD, TrackingAction.REMOVE)) membership(value) else content.membership)
            else -> {
                if (user?.opt("seen") !is Boolean) throw TrackingException(TrackingFailure.PROTOCOL)
                content.copy(seen = user.getBoolean("seen"))
            }
        }
        if (movie && action in setOf(TrackingAction.SEEN, TrackingAction.UNSEEN, TrackingAction.RESTORE_MOVIE_STATE) && user?.opt("status") !is Number) throw TrackingException(TrackingFailure.PROTOCOL)
        return entry
    }

    internal fun matches(entry: TrackingContent, action: TrackingAction, original: TrackingContent): Boolean = when (action) {
            TrackingAction.ADD -> entry.membership == AccountMembership.PRESENT
            TrackingAction.REMOVE -> entry.membership == AccountMembership.ABSENT
            TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE -> entry.archived == (action == TrackingAction.ARCHIVE)
            TrackingAction.SEEN, TrackingAction.UNSEEN -> if (entry.identity.type == TrackingType.MOVIE) entry.movieState == if (action == TrackingAction.SEEN) 1 else 0
                else entry.seen == (action == TrackingAction.SEEN)
            TrackingAction.RESTORE_MOVIE_STATE -> entry.movieState == original.movieState
    }

    internal suspend fun reserve(token: String, content: TrackingContent, desired: Map<String, Int>): List<TrackingContent> = kotlinx.coroutines.withTimeoutOrNull(90_000) {
        require(content.parent.type == TrackingType.SHOW)
        val response = api.request("episodes/list", token, fields = mapOf("showId" to content.parent.id, "limit" to "6", "showsLimit" to "1", "released" to "1"))
        if (response.optInt("totalMissingEpisodes") > 0 || response.optInt("totalMissingShows") > 0) throw TrackingException(TrackingFailure.INCOMPLETE)
        val groups = response.getJSONArray("shows")
        val group = (0 until groups.length()).map { groups.getJSONObject(it) }.singleOrNull { it.requiredId() == content.parent.id }
        val values = group?.getJSONArray("unseen")
        val show = api.request("shows/display", token, fields = mapOf("id" to content.parent.id)).getJSONObject("show")
        if (archiveState(show) != false) throw TrackingException(TrackingFailure.INCOMPLETE)
        fun candidates(values: List<JSONObject>) = values.filter { value ->
            val date = value.text("date")?.let { runCatching { LocalDate.parse(it.take(10)) }.getOrNull() }
            if (date == null) throw TrackingException(TrackingFailure.INCOMPLETE)
            if (value.optInt("season", -1) < 0 || value.optInt("episode", -1) < 1 || value.optJSONObject("user")?.opt("seen") !is Boolean) throw TrackingException(TrackingFailure.INCOMPLETE)
            !date.isAfter(today()) && (desired[value.requiredId()]?.let { it != 1 } ?: (value.optJSONObject("user")?.opt("seen") == false))
        }.map { episode(show, it) }.sortedWith(compareBy<TrackingContent> { it.season }.thenBy { it.episode })
        val initial = candidates((0 until (values?.length() ?: 0)).map { values!!.getJSONObject(it) })
        if (initial.size >= 6) return@withTimeoutOrNull initial.take(6)
        val result = linkedMapOf<String, TrackingContent>()
        initial.forEach { result[it.identity.id] = it }
        val numbers = seasons(token, content)
        var requests = 0
        for (number in numbers) {
            if (++requests > 20) throw TrackingException(TrackingFailure.INCOMPLETE)
            candidates(showEpisodes(token, content.parent.id, number)).forEach { result[it.identity.id] = it }
            if (result.size >= 6) return@withTimeoutOrNull result.values.sortedWith(compareBy<TrackingContent> { it.season }.thenBy { it.episode }).take(6)
        }
        result.values.sortedWith(compareBy<TrackingContent> { it.season }.thenBy { it.episode })
    } ?: throw TrackingException(TrackingFailure.NETWORK)

    private suspend fun next(token: String, content: TrackingContent): TrackingContent? {
        val groups = api.request("episodes/list", token, fields = mapOf("showId" to content.parent.id, "limit" to "1", "showsLimit" to "1", "released" to "1")).optJSONArray("shows")
            ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val show = (0 until groups.length()).map { groups.getJSONObject(it) }.firstOrNull { it.requiredId() == content.parent.id } ?: return null
        val value = show.optJSONArray("unseen")?.optJSONObject(0) ?: return null
        if (value.optJSONObject("user")?.optBoolean("seen") == true) return null
        return content.copy(identity = content.identity.copy(id = value.requiredId()), episodeTitle = value.text("title"),
            season = value.optInt("season"), episode = value.optInt("episode"), seen = false)
    }

    private fun membership(value: JSONObject): AccountMembership {
        val user = value.optJSONObject("user") ?: return AccountMembership.UNKNOWN
        return when (val present = user.opt("in_account")) {
            true, 1, "1", "true" -> AccountMembership.PRESENT
            false, 0, "0", "false" -> AccountMembership.ABSENT
            else -> if (value.has("auth_user") && value.optBoolean("auth_user") && user.optInt("status", -1) in 0..2) AccountMembership.PRESENT else AccountMembership.UNKNOWN
        }
    }

    private fun episode(show: JSONObject, value: JSONObject): TrackingContent {
        val images = show.optJSONObject("images")
        return TrackingContent(
            identity = TrackingIdentity("betaseries", TrackingType.EPISODE, value.requiredId()),
            airDate = value.text("date"),
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
