/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentityRetryBoundsTest {
    private val origin = 7 * IDENTITY_RETRY_MS + 123
    private val query = ContentQuery(TrackingType.SHOW, "Brumeval", season = 1, episode = 4, episodeTitle = "Une famille de fiction")

    private fun show(id: String, title: String) = TrackingIdentity("fixture", TrackingType.SHOW, id)
        .let { parent -> TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "$id-next"), parent, title, season = 2, episode = 1) }

    private fun episode(show: TrackingIdentity, title: String) =
        TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "${show.id}-14"), show, "", episodeTitle = title, season = 1, episode = 4)

    @Test fun `failed identification waits its retry delay from the failure whatever the clock origin`() = runTest {
        var now = origin
        var searches = 0
        val resolver = BoundedIdentityResolver({ searches++; emptyList() }, { now })
        val failed = ContentQuery(TrackingType.MOVIE, "Brumeval")
        assertNull(resolver.resolve(failed))
        now = origin + IDENTITY_RETRY_MS - 1
        assertNull(resolver.resolve(failed))
        assertEquals(1, searches)
        now = origin + IDENTITY_RETRY_MS
        assertNull(resolver.resolve(failed))
        assertEquals(2, searches)
    }

    @Test fun `season read stays cached for its delay whatever the clock origin`() = runTest {
        var now = origin
        var reads = 0
        val identity = EpisodeTitleIdentity { now }
        val account = listOf(show("1", "Brumeval"))
        val read: suspend (TrackingIdentity, Int) -> List<TrackingContent> = { show, _ -> reads++; listOf(episode(show, "Une famille de fiction")) }
        identity.resolve(query, account, read)
        now = origin + IDENTITY_RETRY_MS - 1
        identity.resolve(query, account, read)
        assertEquals(1, reads)
        now = origin + IDENTITY_RETRY_MS
        identity.resolve(query, account, read)
        assertEquals(2, reads)
    }

    @Test fun `three candidates are still read and the single confirmed one wins`() = runTest {
        val account = listOf(show("1", "Brumeval Nord"), show("2", "Brumeval Sud"), show("3", "Brumeval Est"))
        val expected = TrackingIdentity("fixture", TrackingType.SHOW, "2")
        val read: suspend (TrackingIdentity, Int) -> List<TrackingContent> = { show, _ ->
            listOf(episode(show, if (show == expected) "Une famille de fiction" else "Autre chose"))
        }
        assertEquals(LocalTrackingIdentity(expected, expected), EpisodeTitleIdentity().resolve(query, account, read))
    }

    @Test fun `three letter words are enough to make a candidate`() = runTest {
        val short = query.copy(title = "Ozo")
        val account = listOf(show("1", "Ozo"))
        val parent = TrackingIdentity("fixture", TrackingType.SHOW, "1")
        assertEquals(LocalTrackingIdentity(parent, parent), EpisodeTitleIdentity().resolve(short, account) { show, _ -> listOf(episode(show, "Une famille de fiction")) })
    }

    @Test fun `episode title made only of punctuation never confirms a series`() = runTest {
        val blank = query.copy(episodeTitle = "?!")
        assertNull(EpisodeTitleIdentity().resolve(blank, listOf(show("1", "Brumeval"))) { show, _ -> listOf(episode(show, "...")) })
    }
}
