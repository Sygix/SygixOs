/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.UpNextFeed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class ContentQuery(val type: TrackingType, val title: String, val year: Int? = null, val season: Int? = null, val episode: Int? = null, val externalIds: Map<String, String> = emptyMap())
data class CatalogCandidate(val identity: TrackingIdentity, val title: String, val year: Int? = null, val aliases: Set<String> = emptySet(), val externalIds: Map<String, String> = emptyMap())

interface ContentIdentityResolver {
    suspend fun resolve(query: ContentQuery): CatalogCandidate?
}

object ExactContentIdentity {
    fun resolve(query: ContentQuery, candidates: List<CatalogCandidate>): CatalogCandidate? {
        val compatible = candidates.filter { candidate ->
            candidate.identity.type == query.type &&
                (query.year == null || candidate.year == query.year) &&
                query.externalIds.none { (namespace, id) -> candidate.externalIds[namespace]?.let { it != id } == true }
        }
        val identified = compatible.filter { candidate -> query.externalIds.any { (namespace, id) -> candidate.externalIds[namespace] == id } }
        if (identified.isNotEmpty()) return identified.distinctBy { it.identity }.singleOrNull()
        val title = UpNextFeed.normalize(query.title)
        return compatible.filter { candidate -> (candidate.aliases + candidate.title).any { UpNextFeed.normalize(it) == title } }.distinctBy { it.identity }.singleOrNull()
    }
}

class BoundedIdentityResolver(private val search: suspend (ContentQuery) -> List<CatalogCandidate>, private val maximum: Int = 256) : ContentIdentityResolver {
    private val mutex = Mutex()
    private val cache = linkedMapOf<ContentQuery, CatalogCandidate?>()
    init { require(maximum > 0) }
    override suspend fun resolve(query: ContentQuery): CatalogCandidate? = mutex.withLock {
        if (cache.containsKey(query)) return@withLock cache[query]
        val result = ExactContentIdentity.resolve(query, search(query))
        cache[query] = result
        if (cache.size > maximum) cache.remove(cache.keys.first())
        result
    }
}
