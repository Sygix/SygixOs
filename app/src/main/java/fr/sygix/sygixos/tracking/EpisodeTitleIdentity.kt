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
        if (query.type != TrackingType.SHOW) return null
        val wanted = tokens(query.title)
        val candidates = contents.filter { it.membership == AccountMembership.PRESENT && it.parent.type == TrackingType.SHOW }
            .groupBy { it.parent }.filterValues { entries -> entries.any { entry -> (entry.aliases + entry.title).any { tokens(it).any(wanted::contains) } } }.keys
        return confirm(query, candidates, read)
    }

    suspend fun confirm(query: ContentQuery, shows: Collection<TrackingIdentity>, read: suspend (TrackingIdentity, Int) -> List<TrackingContent>): LocalTrackingIdentity? {
        val season = query.season ?: return null
        val number = query.episode ?: return null
        val episodeTitle = DistinctiveEpisodeTitle.of(query.episodeTitle) ?: return null
        if (query.type != TrackingType.SHOW || shows.isEmpty() || shows.size > MAX_CANDIDATES || shows.any { it.type != TrackingType.SHOW }) return null
        val confirmed = shows.filter { show ->
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

object DistinctiveEpisodeTitle {
    private const val MIN_LETTERS = 6
    private const val NUMBER = """(\d+|[ivxlc]+|one|two|three|four|five|six|seven|eight|nine|ten|un|une|deux|trois|quatre|cinq|sept|huit|neuf|dix|eins|zwei|drei|vier|funf|sechs|sieben|acht|neun|zehn|uno|dos|tres|cuatro|cinco|seis|siete|ocho|nueve|diez|due|tre|quattro|cinque|sei|sette|otto|nove|dieci)"""
    private val Generic = listOf(
        Regex("""^((episode|episodio) )?(pilot|pilote|piloto|pilota|pilotfolge)( (episode|folge))?$"""),
        Regex("""^(episode|episodio|episodi|folge|ep) $NUMBER$"""),
        Regex("""^(chapter|chapitre|kapitel|capitulo|capitolo) \S+$"""),
        Regex("""^(part|partie|teil|parte) $NUMBER$"""),
        Regex("""^(tba|tbd|to be announced|to be determined)$"""),
    )

    fun of(title: String?): String? = title?.let(UpNextFeed::normalize)?.takeIf { normalized ->
        normalized.count(Char::isLetter) >= MIN_LETTERS && Generic.none { it.matches(normalized) }
    }
}
