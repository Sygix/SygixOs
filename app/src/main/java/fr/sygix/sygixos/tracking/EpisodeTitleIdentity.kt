/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.UpNextFeed
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class EpisodeTitleIdentity(private val clock: () -> Long = { System.nanoTime() / 1_000_000 }) {
    private class Season(val episodes: List<TrackingContent>, val at: Long)
    private val mutex = Mutex()
    private val seasons = linkedMapOf<Pair<TrackingIdentity, Int>, Season>()

    suspend fun resolve(query: ContentQuery, contents: List<TrackingContent>, read: suspend (TrackingIdentity, Int) -> List<TrackingContent>): LocalTrackingIdentity? {
        val season = query.season ?: return null
        val number = query.episode ?: return null
        val episodeTitle = query.episodeTitle?.let(UpNextFeed::normalize)?.takeIf { it.isNotEmpty() } ?: return null
        if (query.type != TrackingType.SHOW) return null
        val wanted = tokens(query.title)
        val candidates = contents.filter { it.membership == AccountMembership.PRESENT && it.parent.type == TrackingType.SHOW }
            .groupBy { it.parent }.filterValues { entries -> entries.any { entry -> (entry.aliases + entry.title).any { tokens(it).any(wanted::contains) } } }.keys
        if (candidates.isEmpty() || candidates.size > MAX_CANDIDATES) return null
        val confirmed = candidates.filter { show ->
            episodes(show, season, read).any { it.season == season && it.episode == number && it.episodeTitle?.let(UpNextFeed::normalize) == episodeTitle }
        }
        return confirmed.singleOrNull()?.let { LocalTrackingIdentity(it, it) }
    }

    private suspend fun episodes(show: TrackingIdentity, season: Int, read: suspend (TrackingIdentity, Int) -> List<TrackingContent>): List<TrackingContent> {
        val key = show to season
        mutex.withLock { seasons[key]?.takeIf { clock() - it.at < IDENTITY_RETRY_MS }?.let { return it.episodes } }
        val episodes = read(show, season)
        mutex.withLock {
            seasons.remove(key)
            seasons[key] = Season(episodes, clock())
            if (seasons.size > MAX_SEASONS) seasons.remove(seasons.keys.first())
        }
        return episodes
    }

    private fun tokens(title: String): Set<String> = UpNextFeed.normalize(title).split(' ').filter { it.length >= 3 && it !in StopWords }.toSet()

    private companion object {
        const val MAX_CANDIDATES = 3
        const val MAX_SEASONS = 64
        val StopWords = setOf(
            "the", "and", "for", "with", "from", "les", "des", "une", "aux", "sur", "dans", "pour", "avec", "par",
            "der", "die", "das", "den", "dem", "und", "ein", "eine", "einer", "von", "mit", "los", "las", "del", "una", "uno", "con", "por",
            "gli", "della", "delle", "dei", "degli", "nel", "nella", "per",
        )
    }
}
