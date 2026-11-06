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
}
