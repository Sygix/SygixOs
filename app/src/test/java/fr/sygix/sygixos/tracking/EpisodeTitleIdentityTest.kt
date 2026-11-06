/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class EpisodeTitleIdentityTest {
    private val query = ContentQuery(TrackingType.SHOW, "Les Sorcières de Brumeval", season = 1, episode = 4, episodeTitle = "Une famille de fiction")
    private fun show(id: String, title: String) = TrackingIdentity("fixture", TrackingType.SHOW, id)
        .let { parent -> TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "$id-next"), parent, title, season = 2, episode = 1) }
    private fun episode(show: TrackingIdentity, title: String) =
        TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "${show.id}-14"), show, "", episodeTitle = title, season = 1, episode = 4)

    @Test fun localizedSeriesIsConfirmedByItsEpisodeTitleAndTheSeasonReadIsCached() = runTest {
        var now = 0L
        val reads = mutableListOf<Pair<String, Int>>()
        val identity = EpisodeTitleIdentity { now }
        val account = listOf(show("1", "Brumeval Witches"), show("2", "Unrelated Series"))
        val read: suspend (TrackingIdentity, Int) -> List<TrackingContent> = { show, season -> reads += show.id to season; listOf(episode(show, "Une famille de fiction !")) }
        val parent = TrackingIdentity("fixture", TrackingType.SHOW, "1")
        assertEquals(LocalTrackingIdentity(parent, parent), identity.resolve(query, account, read))
        assertEquals(listOf("1" to 1), reads)
        identity.resolve(query, account, read)
        assertEquals(1, reads.size)
        now = IDENTITY_RETRY_MS
        identity.resolve(query, account, read)
        assertEquals(2, reads.size)
    }

    @Test fun differentEpisodeTitleDoesNotConfirmTheSeries() = runTest {
        assertNull(EpisodeTitleIdentity().resolve(query, listOf(show("1", "Brumeval Witches"))) { show, _ -> listOf(episode(show, "Another title")) })
    }

    @Test fun twoMatchingCandidatesAreAmbiguous() = runTest {
        val account = listOf(show("1", "Brumeval Witches"), show("2", "Brumeval Files"))
        assertNull(EpisodeTitleIdentity().resolve(query, account) { show, _ -> listOf(episode(show, "Une famille de fiction")) })
    }

    @Test fun moreThanThreeCandidatesAreNotRead() = runTest {
        var reads = 0
        val account = (1..4).map { show("$it", "Brumeval $it") }
        assertNull(EpisodeTitleIdentity().resolve(query, account) { show, _ -> reads++; listOf(episode(show, "Une famille de fiction")) })
        assertEquals(0, reads)
    }

    @Test fun stopWordsAndShortTokensDoNotMakeCandidates() = runTest {
        var reads = 0
        val account = listOf(show("1", "Les Sorcières"), show("2", "De La"))
        val french = query.copy(title = "Les Sorcières de Brumeval")
        EpisodeTitleIdentity().resolve(french, listOf(show("3", "Les Fixtures de la nuit"), show("4", "De La")), { show, _ -> reads++; listOf(episode(show, "Une famille de fiction")) })
        assertEquals(0, reads)
        assertNotNull(EpisodeTitleIdentity().resolve(french, account) { show, _ -> listOf(episode(show, "Une famille de fiction")) })
    }

    @Test fun genericPilotTitleDoesNotConfirmASingleTokenSharingShow() = runTest {
        val pilot = query.copy(episodeTitle = "Pilot")
        assertNull(EpisodeTitleIdentity().resolve(pilot, listOf(show("1", "Brumeval Witches"))) { show, _ -> listOf(episode(show, "Pilot")) })
    }

    @Test fun onlyDistinctiveEpisodeTitlesAreAccepted() {
        listOf(null, "", "!!", "Ombre", "12", "1x04", "Pilot", "Pilote", "Episodio piloto", "Épisode 4", "Episode 12", "Folge 3", "Episodio 7",
            "Chapter One", "Chapitre 2", "Kapitel Drei", "Capítulo uno", "Capitolo 5", "Part 2", "Partie II", "Teil 1", "Parte tres", "TBA", "TBD", "To Be Announced")
            .forEach { assertNull(it, DistinctiveEpisodeTitle.of(it)) }
        assertEquals("une famille de fiction", DistinctiveEpisodeTitle.of("Une famille de fiction !"))
        assertEquals("chapter one the vanishing", DistinctiveEpisodeTitle.of("Chapter One: The Vanishing"))
        assertEquals("partie de chasse", DistinctiveEpisodeTitle.of("Partie de chasse"))
    }
}
