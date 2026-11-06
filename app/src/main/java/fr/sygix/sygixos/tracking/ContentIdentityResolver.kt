/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.UpNextFeed
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

const val IDENTITY_RETRY_MS = 15 * 60_000L

data class ContentQuery(val type: TrackingType, val title: String, val year: Int? = null, val season: Int? = null, val episode: Int? = null, val externalIds: Map<String, String> = emptyMap(), val episodeTitle: String? = null) {
    companion object {
        private val EpisodeMarker = Regex("""(?i)\bS\d{1,3}\s*E\d{1,4}\b""")

        fun of(item: UpNextItem): ContentQuery {
            val movie = item.type == UpNextContentType.MOVIE
            val title = if (movie) item.displayTitle else item.displayTitle.substringBefore(EpisodeMarker.find(item.displayTitle)?.value ?: "\u0000")
                .trim().trimEnd('-', '–', '—', ':', '·').trim()
            return ContentQuery(if (movie) TrackingType.MOVIE else TrackingType.SHOW, title, item.year.takeIf { movie }, item.season?.toIntOrNull(), item.episode?.toIntOrNull(), item.externalIds,
                item.episodeTitle.takeUnless { movie })
        }
    }
}

data class CatalogCandidate(val identity: TrackingIdentity, val title: String, val year: Int? = null, val aliases: Set<String> = emptySet(), val externalIds: Map<String, String> = emptyMap())

data class CatalogSearch(val candidates: List<CatalogCandidate>, val complete: Boolean = true)

data class CatalogMatch(val candidate: CatalogCandidate?, val titled: List<CatalogCandidate>, val complete: Boolean) {
    val missing: Boolean get() = complete && titled.isEmpty()
    val ambiguous: Boolean get() = candidate == null && titled.size > 1
}

interface ContentIdentityResolver {
    suspend fun resolve(query: ContentQuery): CatalogCandidate?
}

object ExactContentIdentity {
    fun compatible(query: ContentQuery, candidate: CatalogCandidate): Boolean = candidate.identity.type == query.type &&
        (query.year == null || candidate.year == query.year) &&
        query.externalIds.none { (namespace, id) -> candidate.externalIds[namespace]?.let { it != id } == true }

    fun resolve(query: ContentQuery, candidates: List<CatalogCandidate>): CatalogCandidate? {
        val compatible = candidates.filter { compatible(query, it) }
        val identified = compatible.filter { candidate -> query.externalIds.any { (namespace, id) -> candidate.externalIds[namespace] == id } }
        if (identified.isNotEmpty()) return identified.distinctBy { it.identity }.singleOrNull()
        return titled(query, compatible).singleOrNull()
    }

    fun titled(query: ContentQuery, candidates: List<CatalogCandidate>): List<CatalogCandidate> {
        val title = UpNextFeed.normalize(query.title)
        return candidates.filter { candidate -> candidate.identity.type == query.type && (candidate.aliases + candidate.title).any { UpNextFeed.normalize(it) == title } }
            .distinctBy { it.identity }
    }

    fun match(query: ContentQuery, search: CatalogSearch): CatalogMatch {
        val candidates = search.candidates.takeIf { search.complete }.orEmpty()
        return CatalogMatch(resolve(query, candidates), titled(query, candidates), search.complete)
    }
}

object AccountContentIdentity {
    fun resolve(item: UpNextItem, contents: List<TrackingContent>): LocalTrackingIdentity? {
        val query = ContentQuery.of(item).let { if (it.type == TrackingType.SHOW) it.copy(year = null) else it }
        val candidates = contents.filter { it.membership == AccountMembership.PRESENT }.map { content ->
            CatalogCandidate(content.parent, content.title, content.year.takeIf { content.parent.type == TrackingType.MOVIE }, content.aliases, content.externalIds)
        }
        return ExactContentIdentity.resolve(query, candidates)?.identity?.let { LocalTrackingIdentity(it, it) }
    }
}

class BoundedIdentityResolver(
    private val search: suspend (ContentQuery) -> CatalogSearch,
    private val clock: () -> Long = { System.nanoTime() / 1_000_000 },
    private val maximum: Int = 256,
) : ContentIdentityResolver {
    private class Resolution(val match: CatalogMatch, val at: Long)
    private val mutex = Mutex()
    private val cache = linkedMapOf<ContentQuery, Resolution>()
    init { require(maximum > 0) }
    override suspend fun resolve(query: ContentQuery): CatalogCandidate? = match(query).candidate

    suspend fun match(query: ContentQuery): CatalogMatch = mutex.withLock {
        cache[query]?.takeIf { it.match.candidate != null || clock() - it.at < IDENTITY_RETRY_MS }?.let { return@withLock it.match }
        val result = ExactContentIdentity.match(query, search(query))
        cache.remove(query)
        cache[query] = Resolution(result, clock())
        if (cache.size > maximum) cache.remove(cache.keys.first())
        result
    }
}
