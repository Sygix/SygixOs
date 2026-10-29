/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.util.Locale
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import org.json.JSONObject

class TraktProvider(private val api: TraktApi, private val wallClock: () -> Long = System::currentTimeMillis) : TrackingContentProvider {
    override val id = "trakt"
    private val resolver = BoundedIdentityResolver(::search)
    private val targetedMutationMutex = Mutex()
    private data class ShowMembership(val membership: AccountMembership, val started: Boolean?, val archived: Boolean?)
    private data class MovieAccountState(val membership: AccountMembership, val seen: Boolean, val statusFailure: TrackingFailure?)

    override suspend fun account(token: String): TrackingAccount = accountFrom(api.request("users/settings", token))
    override suspend fun validateSession(token: String): TrackingAccount = accountFrom(api.validateCandidate(token))

    private fun accountFrom(response: JSONObject): TrackingAccount {
        val user = response.optJSONObject("user") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val ids = user.optJSONObject("ids") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val id = ids.text("uuid") ?: ids.optLong("trakt", -1).takeIf { it > 0 }?.toString() ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return TrackingAccount("trakt:$id", user.text("name") ?: user.text("username") ?: id)
    }

    override suspend fun revoke(token: String) { api.revoke(token) }

    override suspend fun snapshot(token: String): TrackingSnapshot = withContext(api.dispatcher) {
        val content = mutableListOf<TrackingContent>()
        withTimeoutOrNull(90_000) {
            var incomplete = false
            suspend fun pages(path: String, extended: String? = null): List<JSONObject> = try { apiPages(path, token, extended) }
            catch (error: kotlinx.coroutines.CancellationException) { throw error }
            catch (error: Exception) {
                if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error
                incomplete = true
                (error as? TraktPagesException)?.items.orEmpty()
            }
            val movies = pages("sync/watchlist/movies", "full,images")
            val showsWatchlisted = pages("sync/watchlist/shows")
            var progressComplete = true
            val progress = try { apiPages("sync/progress/up_next", token) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error
                incomplete = true
                progressComplete = false
                (error as? TraktPagesException)?.items.orEmpty()
            }
            var hiddenComplete = true
            val hiddenRows = try { apiPages("users/hidden/progress_watched", token) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error
                incomplete = true
                hiddenComplete = false
                (error as? TraktPagesException)?.items.orEmpty()
            }
            val hidden = hiddenRows.mapNotNull { it.optJSONObject("show")?.traktId() }.toSet()
            val started = progress.mapNotNull { it.optJSONObject("show")?.traktId() }.toSet()
            val showRows = linkedMapOf<String, JSONObject>()
            showsWatchlisted.forEach { it.optJSONObject("show")?.let { show -> showRows[show.traktId()] = show } }
            progress.forEach { it.optJSONObject("show")?.let { show -> showRows[show.traktId()] = show } }
            for (item in movies) try {
                val movie = movie(item.optJSONObject("movie") ?: throw TrackingException(TrackingFailure.PROTOCOL))
                val history = api.request("sync/history/movies/${movie.identity.id}?limit=1", token).optJSONArray("items")
                    ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                if (history.length() == 0) content += movie.copy(membership = AccountMembership.PRESENT)
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error
                incomplete = true
            }
            if (hiddenComplete) for ((showId, show) in showRows) {
                if (showId in hidden) continue
                val isStarted = showId in started
                try {
                    val canRemove = progressComplete && !isStarted
                    val showContent = TrackingContent(TrackingIdentity(id, TrackingType.SHOW, showId), title = show.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL),
                        externalIds = externalIds(show), membership = AccountMembership.PRESENT, removableFromAccount = canRemove)
                    content += reserveKnownVisible(token, showContent, emptyMap()).map { it.copy(removableFromAccount = canRemove) }
                } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                catch (error: Exception) {
                    if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error
                    incomplete = true
                }
            }
            TrackingSnapshot(content.distinctBy { it.identity }, if (incomplete) TrackingFailure.INCOMPLETE else null)
        } ?: TrackingSnapshot(content.distinctBy { it.identity }, TrackingFailure.INCOMPLETE)
    }

    private suspend fun apiPages(path: String, token: String, extended: String? = null): List<JSONObject> {
        val result = linkedMapOf<String, JSONObject>()
        for (page in 1..200) {
            val response = try { api.request("$path?page=$page&limit=100" + extended?.let { "&extended=${java.net.URLEncoder.encode(it, "UTF-8")}" }.orEmpty(), token) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) {
                if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE) || result.isEmpty()) throw error
                throw TraktPagesException(result.values.toList(), error.trackingFailure())
            }
            val batch = response.optJSONArray("items") ?: throw TrackingException(TrackingFailure.PROTOCOL)
            val previousSize = result.size
            batch.objects().forEach { value ->
                val id = value.optJSONObject("movie")?.traktId() ?: value.optJSONObject("show")?.traktId() ?: value.traktId()
                result[id] = value
            }
            val paginationObject = response.optJSONObject("__pagination")
            val pagination = paginationObject?.keys()?.asSequence()?.associateWith { paginationObject.optString(it) }
            val currentPage = pagination?.get("page")?.toIntOrNull()
            val pageCount = pagination?.get("page-count")?.toIntOrNull()
            if (!pagination.isNullOrEmpty() && (currentPage == null || pageCount == null)) throw TraktPagesException(result.values.toList(), TrackingFailure.INCOMPLETE)
            if (batch.length() > 0 && result.size == previousSize) throw TraktPagesException(result.values.toList(), TrackingFailure.INCOMPLETE)
            if (currentPage != null && pageCount != null && currentPage >= pageCount) return result.values.toList()
            if (pagination.isNullOrEmpty() && batch.length() < 100) return result.values.toList()
            if (batch.length() == 0) throw TraktPagesException(result.values.toList(), TrackingFailure.INCOMPLETE)
        }
        throw TraktPagesException(result.values.toList(), TrackingFailure.INCOMPLETE)
    }

    override suspend fun identify(token: String, item: UpNextItem, known: LocalTrackingIdentity?): TrackingContent? = withContext(api.dispatcher) {
        val movie = item.type == UpNextContentType.MOVIE
        val title = if (movie) item.displayTitle else item.seriesTitle ?: item.displayTitle.substringBefore(Regex("(?i)\\bS\\d{1,3}E\\d{1,4}\\b").find(item.displayTitle)?.value ?: "\u0000").trim()
        val candidate = known?.let { CatalogCandidate(it.parent, title, item.year, externalIds = item.externalIds) }
            ?: resolver.resolve(ContentQuery(if (movie) TrackingType.MOVIE else TrackingType.SHOW, title, item.year, item.season?.toIntOrNull(), item.episode?.toIntOrNull(), item.externalIds)) ?: return@withContext null
        val detail = api.request("${if (movie) "movies" else "shows"}/${candidate.identity.id}?extended=full%2Cimages", token)
        if (movie) {
            val result = movie(detail.optJSONObject("movie") ?: detail)
            val account = movieAccountState(token, result.identity.id)
            result.copy(membership = account.membership, seen = account.seen, movieState = if (account.seen) 1 else 0, failure = account.statusFailure)
        } else {
            val show = detail.optJSONObject("show") ?: detail
            val membership = showMembership(token, candidate.identity.id)
            val exact = known?.identity?.takeIf { it.type == TrackingType.EPISODE }?.id
            if (exact != null) episodeDetail(token, show, exact).copy(membership = membership.membership, archived = membership.archived,
                removableFromAccount = membership.started == false && membership.archived != true) else {
                val season = item.season?.toIntOrNull() ?: return@withContext TrackingContent(candidate.identity, title = show.text("title") ?: title,
                    externalIds = externalIds(show), membership = membership.membership, archived = membership.archived,
                    removableFromAccount = membership.started == false && membership.archived != true)
                val number = item.episode?.toIntOrNull() ?: return@withContext null
                val seasonData = api.request("shows/${candidate.identity.id}/seasons/$season?extended=full", token)
                val episodes = seasonData.optJSONArray("items") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                (0 until episodes.length()).mapNotNull { episodes.optJSONObject(it) }.singleOrNull { it.optInt("number") == number }
                    ?.let { value ->
                        val episodeId = value.optJSONObject("ids")?.optLong("trakt", -1)?.takeIf { it > 0 }?.toString()
                            ?: throw TrackingException(TrackingFailure.PROTOCOL)
                        episodeDetail(token, show, episodeId).copy(membership = membership.membership, archived = membership.archived,
                            removableFromAccount = membership.started == false && membership.archived != true)
                    }
            }
        }
    }

    private suspend fun search(query: ContentQuery): List<CatalogCandidate> {
        val path = if (query.type == TrackingType.MOVIE) "search/movie" else "search/show"
        val response = api.request("$path?query=${java.net.URLEncoder.encode(query.title, "UTF-8")}&limit=10&page=1")
        val results = response.optJSONArray("items") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
        val pagination = response.optJSONObject("__pagination")
        val pageCount = pagination?.optInt("page-count", -1) ?: -1
        if (pageCount > 1 || pagination != null && pageCount < 1 || pageCount < 0 && results.length() >= 10) return emptyList()
        return results.objects().mapNotNull { row ->
            val key = if (query.type == TrackingType.MOVIE) "movie" else "show"
            row.optJSONObject(key)?.let { value -> CatalogCandidate(TrackingIdentity(id, query.type, value.traktId()), value.text("title").orEmpty(), value.optInt("year").takeIf { it > 0 }, externalIds = externalIds(value)) }
        }
    }

    override suspend fun platforms(country: String): RegionalPlatforms = RegionalPlatforms(country.uppercase(Locale.ROOT), emptySet())

    override suspend fun offers(token: String, content: TrackingContent, country: String): TrackingOfferResult = try {
        val kind = if (content.identity.type == TrackingType.MOVIE) "movies" else "shows"
        val requestedCountry = country.lowercase(Locale.ROOT)
        val response = api.request("$kind/${content.parent.id}/watchnow/$requestedCountry?links=android", token)
        val countryKey = response.keys().asSequence().firstOrNull { it.equals(requestedCountry, true) }
            ?: return TrackingOfferResult(emptyList())
        val regional = response.optJSONObject(countryKey) ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val offers = listOf("free", "subscription", "cable", "purchase", "cinema").flatMap { category ->
            regional.optJSONArray(category)?.objects().orEmpty().mapNotNull { service ->
                val source = service.text("source") ?: return@mapNotNull null
                PlaybackOffer(source, displayServiceName(source), requestedCountry)
            }
        }
        TrackingOfferResult(offers.distinctBy { it.id })
    } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
    catch (error: Exception) { TrackingOfferResult(emptyList(), error.trackingFailure()) }

    override fun confirmOffers(offers: List<PlaybackOffer>, region: RegionalPlatforms?): List<PlaybackOffer> = offers.map { offer ->
        offer.copy(confirmedCountry = offer.confirmedCountry?.takeIf { it.equals(region?.country, true) })
    }

    override suspend fun seasons(token: String, content: TrackingContent): List<Int> {
        val response = api.request("shows/${content.parent.id}/seasons", token)
        val values = response.optJSONArray("items") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
        return values.objects().mapNotNull { it.optInt("number", -1).takeIf { number -> number >= 0 } }.distinct().sorted()
    }

    override suspend fun episodes(token: String, content: TrackingContent): List<TrackingContent> {
        val season = content.season ?: 1
        val show = showDetails(token, content.parent.id)
        val progress = showProgress(token, content.parent.id)
        val statuses = progressStatuses(progress)
        val items = seasonDetails(token, content.parent.id, season)
        return items.map { value ->
            val number = value.optInt("number", -1).takeIf { it > 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            val completed = statuses[season to number] ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            val aired = airedAt(value.text("first_aired")) ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            episode(show, value, completed, season).copy(membership = content.membership, archived = content.archived,
                removableFromAccount = content.removableFromAccount) to aired
        }.filter { (_, aired) -> aired <= wallClock() }.map { it.first }
    }

    override suspend fun reserve(token: String, content: TrackingContent, desired: Map<String, Int>): List<TrackingContent> = withTimeoutOrNull(90_000) {
        if (desired.values.any { it !in 0..1 }) throw TrackingException(TrackingFailure.INCOMPLETE)
        if (apiPages("users/hidden/progress_watched", token).any { it.optJSONObject("show")?.traktId() == content.parent.id }) return@withTimeoutOrNull emptyList()
        reserveKnownVisible(token, content, desired)
    } ?: throw TrackingException(TrackingFailure.INCOMPLETE)

    private suspend fun reserveKnownVisible(token: String, content: TrackingContent, desired: Map<String, Int>): List<TrackingContent> {
        val progress = showProgress(token, content.parent.id)
        val seasons = progress.optJSONArray("seasons")?.objects() ?: throw TrackingException(TrackingFailure.INCOMPLETE)
        val statuses = progressStatuses(progress)
        val desiredSeasons = desiredSeasons(token, progress, desired)
        val show = showDetails(token, content.parent.id)
        val candidates = mutableListOf<TrackingContent>()
        var requests = 0
        for (season in seasons.sortedBy { it.optInt("number", -1) }) {
            val number = season.optInt("number", -1).takeIf { it >= 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            val aired = season.optInt("aired", -1)
            val completedCount = season.optInt("completed", -1)
            if (aired < 0 || completedCount < 0) throw TrackingException(TrackingFailure.INCOMPLETE)
            val seasonComplete = season.opt("completed") is Boolean && season.optBoolean("completed") || aired > 0 && completedCount >= aired
            val includeForDesired = number in desiredSeasons
            if (seasonComplete && !includeForDesired) continue
            if (requests >= 20) throw TrackingException(TrackingFailure.INCOMPLETE)
            requests++
            val seasonItems = seasonDetails(token, content.parent.id, number)
            for (value in seasonItems) {
                val episodeNumber = value.optInt("number", -1).takeIf { it > 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                val episodeId = value.optJSONObject("ids")?.optLong("trakt", -1)?.takeIf { it > 0 }?.toString() ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                val airedAt = airedAt(value.text("first_aired")) ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                val targetSeen = desired[episodeId]?.let { it == 1 } ?: statuses[number to episodeNumber] ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                if (targetSeen) continue
                if (airedAt <= wallClock()) candidates += episode(show, value, false, number).copy(membership = AccountMembership.PRESENT, archived = false,
                    removableFromAccount = content.removableFromAccount)
                if (candidates.size >= 6) return candidates.sortedWith(compareBy<TrackingContent> { it.season }.thenBy { it.episode }).take(6)
            }
        }
        return candidates.sortedWith(compareBy<TrackingContent> { it.season }.thenBy { it.episode }).take(6)
    }

    private suspend fun showProgress(token: String, showId: String): JSONObject = api.request("shows/$showId/progress/watched?hidden=false&specials=true", token)

    private suspend fun showDetails(token: String, showId: String): JSONObject {
        val response = api.request("shows/$showId?extended=full%2Cimages", token)
        return response.optJSONObject("show") ?: response
    }

    private suspend fun seasonDetails(token: String, showId: String, season: Int): List<JSONObject> {
        val response = api.request("shows/$showId/seasons/$season?extended=full", token)
        return response.optJSONArray("items")?.objects() ?: throw TrackingException(TrackingFailure.INCOMPLETE)
    }

    private fun progressStatuses(progress: JSONObject): Map<Pair<Int, Int>, Boolean> {
        val seasons = progress.optJSONArray("seasons") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
        val result = linkedMapOf<Pair<Int, Int>, Boolean>()
        for (season in seasons.objects()) {
            val seasonNumber = season.optInt("number", -1).takeIf { it >= 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            val episodes = season.optJSONArray("episodes") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            for (episode in episodes.objects()) {
                val episodeNumber = episode.optInt("number", -1).takeIf { it > 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                val completed = episode.opt("completed") as? Boolean ?: throw TrackingException(TrackingFailure.INCOMPLETE)
                result[seasonNumber to episodeNumber] = completed
            }
        }
        return result
    }

    private suspend fun desiredSeasons(token: String, progress: JSONObject, desired: Map<String, Int>): Set<Int> {
        val pendingIds = desired.filterValues { it == 0 }.keys
        if (pendingIds.size > 20) throw TrackingException(TrackingFailure.INCOMPLETE)
        val known = linkedMapOf<String, Int>()
        progress.optJSONArray("seasons")?.objects().orEmpty().forEach { season ->
            val number = season.optInt("number", -1).takeIf { it >= 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            season.optJSONArray("episodes")?.objects().orEmpty().forEach { episode ->
                episode.optJSONObject("ids")?.optLong("trakt", -1)?.takeIf { it > 0 }?.toString()?.let { known[it] = number }
            }
        }
        return pendingIds.map { id ->
            known[id] ?: run {
                val response = api.request("episodes/${id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)}?extended=full", token)
                val episode = response.optJSONObject("episode") ?: response
                episode.optInt("season", -1).takeIf { it >= 0 } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            }
        }.toSet()
    }

    override suspend fun mutate(token: String, content: TrackingContent, action: TrackingAction): List<TrackingContent> {
        var mutationError: Exception? = null
        try { sendMutation(token, content, action) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { mutationError = error }
        return try { verify(token, content, action) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { throw mutationError ?: error }
    }

    override suspend fun sendMutation(token: String, content: TrackingContent, action: TrackingAction) = targetedMutationMutex.withLock {
        if (action == TrackingAction.SEEN || action == TrackingAction.RESTORE_MOVIE_STATE && content.movieState == 1) {
            if (readTarget(token, content, action).seen) return@withLock
            var mutationFailure: Exception? = null
            try { sendMutationLocked(token, content, action) }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { mutationFailure = error }
            val confirmed = try { readTarget(token, content, action).seen }
            catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { throw mutationFailure ?: error }
            if (confirmed) return@withLock
            throw mutationFailure ?: TrackingException(TrackingFailure.INCOMPLETE)
        } else sendMutationLocked(token, content, action)
    }

    private suspend fun sendMutationLocked(token: String, content: TrackingContent, action: TrackingAction) {
        val movie = content.identity.type == TrackingType.MOVIE
        val id = if (movie) content.identity.id else content.identity.id
        val parent = content.parent.id
        if (action == TrackingAction.REMOVE && !movie && !content.removableFromAccount) throw TrackingException(TrackingFailure.PROTOCOL)
        if (action == TrackingAction.UNSEEN && movie || action == TrackingAction.RESTORE_MOVIE_STATE && movie && content.movieState == 0) {
            reconcileMovieUnseen(token, content)
            return
        }
        if (action == TrackingAction.UNSEEN && !readTarget(token, content, action).seen) return
        if (action == TrackingAction.RESTORE_MOVIE_STATE) {
            if (!movie || content.movieState !in 0..1) throw TrackingException(TrackingFailure.PROTOCOL)
            val current = readTarget(token, content, action)
            if (current.movieState == content.movieState) return
            val requestBody = JSONObject().put("movies", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))
            api.request("sync/history", token, "POST", requestBody)
            return
        }
        val body = when (action) {
            TrackingAction.ADD -> JSONObject().put(if (movie) "movies" else "shows", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", parent.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))
            TrackingAction.REMOVE -> JSONObject().put(if (movie) "movies" else "shows", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", parent.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))
            TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE -> JSONObject().put("shows", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", parent.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))
            TrackingAction.SEEN -> JSONObject().put(if (movie) "movies" else "episodes", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))
            TrackingAction.UNSEEN -> JSONObject().put(if (movie) "movies" else "episodes", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))
            TrackingAction.RESTORE_MOVIE_STATE -> throw TrackingException(TrackingFailure.PROTOCOL)
        }
        when (action) {
            TrackingAction.ADD -> api.request("sync/watchlist", token, "POST", body)
            TrackingAction.REMOVE -> api.request("sync/watchlist/remove", token, "POST", body)
            TrackingAction.ARCHIVE -> api.request("users/hidden/progress_watched", token, "POST", body)
            TrackingAction.UNARCHIVE -> api.request("users/hidden/progress_watched/remove", token, "POST", body)
            TrackingAction.SEEN -> api.request("sync/history", token, "POST", body)
            TrackingAction.UNSEEN -> api.request("sync/history/remove", token, "POST", body)
            TrackingAction.RESTORE_MOVIE_STATE -> Unit
        }
    }

    private suspend fun reconcileMovieUnseen(token: String, content: TrackingContent) {
        var state = readTarget(token, content, TrackingAction.UNSEEN)
        if (!state.seen && state.membership == AccountMembership.PRESENT) return
        var mutationFailure: Exception? = null
        if (state.seen) {
            try {
                api.request("sync/history/remove", token, "POST", moviePayload(content.identity.id))
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { mutationFailure = error }
            state = readTarget(token, content, TrackingAction.UNSEEN)
            if (state.seen) throw mutationFailure ?: TrackingException(TrackingFailure.INCOMPLETE)
        }
        if (state.membership != AccountMembership.PRESENT) {
            try {
                api.request("sync/watchlist", token, "POST", moviePayload(content.identity.id))
            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
            catch (error: Exception) { if (mutationFailure == null) mutationFailure = error }
            state = readTarget(token, content, TrackingAction.UNSEEN)
        }
        if (!matches(state, TrackingAction.UNSEEN, content)) throw mutationFailure ?: TrackingException(TrackingFailure.INCOMPLETE)
    }

    private fun moviePayload(id: String): JSONObject = JSONObject().put("movies", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)))))

    override suspend fun readTarget(token: String, content: TrackingContent, action: TrackingAction): TrackingContent {
        val movie = content.identity.type == TrackingType.MOVIE
        val id = content.identity.id
        val show = content.parent.id
        if (movie) {
            val statusRequired = action in setOf(TrackingAction.SEEN, TrackingAction.UNSEEN, TrackingAction.RESTORE_MOVIE_STATE)
            val history = if (statusRequired) api.request("sync/history/movies/${id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)}?limit=1", token) else null
            val events = history?.optJSONArray("items")
            val seen = if (statusRequired) (events ?: throw TrackingException(TrackingFailure.INCOMPLETE)).length() > 0 else content.seen
            val membership = if (action == TrackingAction.SEEN || action == TrackingAction.RESTORE_MOVIE_STATE && content.movieState == 1) {
                if (seen) AccountMembership.PRESENT else content.membership
            } else if (apiPages("sync/watchlist/movies", token).any { it.optJSONObject("movie")?.traktId() == id }) AccountMembership.PRESENT else AccountMembership.ABSENT
            return content.copy(seen = seen, movieState = if (seen) 1 else 0, membership = membership)
        }
        if (action in setOf(TrackingAction.ADD, TrackingAction.REMOVE, TrackingAction.ARCHIVE, TrackingAction.UNARCHIVE)) {
            val state = showMembership(token, show)
            return content.copy(archived = state.archived, membership = state.membership,
                removableFromAccount = state.started == false && state.archived != true)
        }
        val history = api.request("sync/history/episodes/${id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)}?limit=1", token)
        val rows = history.optJSONArray("items") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
        return content.copy(seen = rows.length() > 0)
    }

    override suspend fun verify(token: String, content: TrackingContent, action: TrackingAction): List<TrackingContent> {
        var target = readTarget(token, content, action).copy(sessionGeneration = content.sessionGeneration)
        if (content.identity.type == TrackingType.MOVIE && action in setOf(TrackingAction.UNSEEN, TrackingAction.RESTORE_MOVIE_STATE)) {
            val shouldBeSeen = if (action == TrackingAction.UNSEEN) false else content.movieState == 1
            if (target.seen != shouldBeSeen) throw TrackingException(TrackingFailure.INCOMPLETE)
            if (!shouldBeSeen && target.membership != AccountMembership.PRESENT) {
                api.request("sync/watchlist", token, "POST", JSONObject().put("movies", JSONArray().put(JSONObject().put("ids", JSONObject().put("trakt", content.identity.id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL))))))
                target = readTarget(token, content, action).copy(sessionGeneration = content.sessionGeneration)
                if (target.membership != AccountMembership.PRESENT || target.seen != shouldBeSeen) throw TrackingException(TrackingFailure.INCOMPLETE)
            }
        }
        if (!matches(target, action, content)) throw TrackingException(TrackingFailure.INCOMPLETE)
        return listOf(target)
    }

    override fun matches(entry: TrackingContent, action: TrackingAction, original: TrackingContent): Boolean = when (action) {
        TrackingAction.ADD -> entry.membership == AccountMembership.PRESENT
        TrackingAction.REMOVE -> entry.membership == AccountMembership.ABSENT
        TrackingAction.ARCHIVE -> entry.archived == true
        TrackingAction.UNARCHIVE -> entry.archived == false
        TrackingAction.SEEN -> entry.seen
        TrackingAction.UNSEEN -> !entry.seen && (entry.identity.type != TrackingType.MOVIE || entry.membership == AccountMembership.PRESENT)
        TrackingAction.RESTORE_MOVIE_STATE -> entry.movieState == original.movieState &&
            (entry.identity.type != TrackingType.MOVIE || original.movieState == 1 || entry.membership == AccountMembership.PRESENT)
    }

    private suspend fun episodeDetail(token: String, show: JSONObject, id: String): TrackingContent {
        val response = api.request("episodes/${id.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)}?extended=full", token)
        val episode = response.optJSONObject("episode") ?: response
        val history = api.request("sync/history/episodes/${id.toLongOrNull()}", token).optJSONArray("items")
            ?: throw TrackingException(TrackingFailure.INCOMPLETE)
        return episode(show, episode, history.length() > 0)
    }

    private suspend fun movieAccountState(token: String, movieId: String): MovieAccountState {
        val watchlist = try { apiPages("sync/watchlist/movies", token) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error else return MovieAccountState(AccountMembership.UNKNOWN, false, TrackingFailure.INCOMPLETE) }
        val watchlisted = watchlist.any { it.optJSONObject("movie")?.traktId() == movieId }
        val history = try {
            val history = api.request("sync/history/movies/${movieId.toLongOrNull() ?: throw TrackingException(TrackingFailure.PROTOCOL)}", token)
            val events = history.optJSONArray("items") ?: throw TrackingException(TrackingFailure.INCOMPLETE)
            events.length() > 0
        } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) {
            if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error
            return MovieAccountState(if (watchlisted) AccountMembership.PRESENT else AccountMembership.UNKNOWN, false, TrackingFailure.INCOMPLETE)
        }
        val membership = when {
            watchlisted || history -> AccountMembership.PRESENT
            else -> AccountMembership.ABSENT
        }
        return MovieAccountState(membership, history, null)
    }

    private suspend fun showMembership(token: String, showId: String): ShowMembership {
        var watchlistComplete = true
        val watchlist = try { apiPages("sync/watchlist/shows", token) }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error; watchlistComplete = false; (error as? TraktPagesException)?.items.orEmpty() }
        val started = try {
            val progress = showProgress(token, showId)
            val completed = (progress.opt("completed") as? Number)?.toLong()
            if (completed != null && completed >= 0) completed > 0 else progressStatuses(progress).values.any { it }
        }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error; null }
        val archived = try { apiPages("users/hidden/progress_watched", token).any { it.optJSONObject("show")?.traktId() == showId } }
        catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
        catch (error: Exception) { if (error.trackingFailure() in setOf(TrackingFailure.ACCESS, TrackingFailure.STORAGE)) throw error; null }
        val watchlisted = watchlist.any { it.optJSONObject("show")?.traktId() == showId }
        val membership = when {
            watchlisted || started == true || archived == true -> AccountMembership.PRESENT
            watchlistComplete && started != null && archived != null -> AccountMembership.ABSENT
            else -> AccountMembership.UNKNOWN
        }
        return ShowMembership(membership, started, archived)
    }

    private fun movie(value: JSONObject): TrackingContent {
        val movie = value.optJSONObject("movie") ?: value
        return TrackingContent(TrackingIdentity(id, TrackingType.MOVIE, movie.traktId()), title = movie.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL),
            year = movie.optInt("year").takeIf { it > 0 }, image = image(movie, "fanart"), poster = image(movie, "poster"),
            durationMillis = movie.optLong("runtime", -1).takeIf { it > 0 }?.times(60_000), externalIds = externalIds(movie), membership = AccountMembership.UNKNOWN)
    }

    private fun episode(show: JSONObject, value: JSONObject, seen: Boolean?, seasonOverride: Int? = null): TrackingContent {
        val data = value.optJSONObject("episode") ?: value
        val ids = data.optJSONObject("ids") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val number = ids.optLong("trakt", -1).takeIf { it > 0 }?.toString() ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return TrackingContent(TrackingIdentity(id, TrackingType.EPISODE, number), parent = TrackingIdentity(id, TrackingType.SHOW, show.traktId()),
            title = show.text("title") ?: throw TrackingException(TrackingFailure.PROTOCOL), episodeTitle = data.text("title"),
            season = data.optInt("season", -1).takeIf { it >= 0 } ?: seasonOverride, episode = data.optInt("number", -1).takeIf { it > 0 }, airDate = data.text("first_aired"),
            durationMillis = data.optLong("runtime", show.optLong("runtime", -1)).takeIf { it > 0 }?.times(60_000),
            externalIds = externalIds(show), seen = seen ?: throw TrackingException(TrackingFailure.INCOMPLETE), membership = AccountMembership.UNKNOWN,
            image = image(show, "fanart"), poster = image(show, "poster"))
    }

    private fun contentFromProgress(progress: JSONObject, fallback: TrackingContent): JSONObject = progress.optJSONObject("show")
        ?: JSONObject().put("title", fallback.title).put("ids", JSONObject().put("trakt", fallback.parent.id.toLongOrNull()))

    private fun externalIds(value: JSONObject): Map<String, String> {
        val ids = value.optJSONObject("ids") ?: return emptyMap()
        return linkedMapOf<String, String>().apply {
            listOf("imdb" to "imdb_id", "tmdb" to "themoviedb_id", "tvdb" to "thetvdb_id").forEach { (canonical, alias) ->
                ids.text(canonical)?.let { put(canonical, it); put(alias, it) }
            }
        }
    }

    private fun image(value: JSONObject, key: String): String? = value.optJSONObject("images")?.optJSONArray(key)?.optString(0)?.takeIf { it.isNotBlank() && it != "null" }?.let { url ->
        when {
            url.startsWith("https://", true) -> url
            url.startsWith("http://", true) -> "https://${url.drop(7)}"
            url.startsWith("//") -> "https:$url"
            else -> "https://$url"
        }
    }

    private fun displayServiceName(source: String): String = when (source.lowercase(Locale.ROOT)) {
        "amazon_prime_video" -> "Prime Video"
        "apple_tv_plus" -> "Apple TV+"
        "disney_plus" -> "Disney+"
        "paramount_plus" -> "Paramount+"
        "youtube" -> "YouTube"
        "google_play_movies" -> "Google Play Movies"
        else -> source.split(Regex("[_-]+"))
            .filter { it.isNotBlank() }.joinToString(" ") { part -> part.replaceFirstChar { it.uppercase(Locale.ROOT) } }
    }

    private fun airedAt(value: String?): Long? {
        if (value.isNullOrBlank()) return null
        return runCatching { Instant.parse(value).toEpochMilli() }.getOrNull()
            ?: runCatching { OffsetDateTime.parse(value).toInstant().toEpochMilli() }.getOrNull()
            ?: runCatching { LocalDateTime.parse(value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli() }.getOrNull()
            ?: runCatching { LocalDate.parse(value).atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull()
    }

    private fun JSONObject.traktId(): String = optJSONObject("ids")?.optLong("trakt", -1)?.takeIf { it > 0 }?.toString() ?: throw TrackingException(TrackingFailure.PROTOCOL)
    private fun JSONArray.objects(): List<JSONObject> = (0 until length()).map { optJSONObject(it) ?: throw TrackingException(TrackingFailure.PROTOCOL) }
}

class TraktPagesException(val items: List<JSONObject>, failure: TrackingFailure) : TrackingException(failure)
