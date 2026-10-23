/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.domain.UpNextFeed
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import org.junit.Assert.*
import org.junit.Test

class TrackingProjectionTest {
    private val parent = TrackingIdentity("fixture", TrackingType.SHOW, "1")
    private val next = TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "3"), parent, "Fixture", season = 2, episode = 3)

    @Test fun unseenResumeWinsWithoutReusingAnIntentForAnotherEpisode() {
        val resumed = next.copy(identity = next.identity.copy(id = "5"), episode = 5)
        val source = UpNextSourceEntry("fixture.player", intentUri = "fixture://episode5", targetIdentity = resumed.identity)
        val local = UpNextItem(1, source, UpNextContentType.EPISODE, "Fixture", season = "2", episode = "5", watchNextType = ProgramKind.CONTINUE, tracking = resumed)
        val merged = TrackingProjection.merge(listOf(local), listOf(next)).single()
        assertEquals(5, merged.tracking?.episode)
        assertEquals(resumed.identity, merged.sources.single().targetIdentity)
        val seen = TrackingProjection.merge(listOf(local.copy(tracking = resumed.copy(seen = true))), listOf(next)).single()
        assertEquals(3, seen.tracking?.episode)
        assertNotEquals(seen.tracking?.identity, seen.sources.single().targetIdentity)
    }

    @Test fun enrichmentPreservesLegacyMemoryAndIdentitySurvivesTitleChanges() {
        val item = TrackingProjection.card(next)
        val key = UpNextContentKey.of(item)
        assertEquals(key, UpNextContentKey.parse(key.serialized))
        assertTrue(UpNextContentKey(UpNextContentType.EPISODE, "fixture").matches(key))
        assertTrue(key.matches(UpNextContentKey.of(TrackingProjection.card(next.copy(title = "Titre localisé")))))
        assertFalse(key.matches(UpNextContentKey.of(TrackingProjection.card(next.copy(parent = parent.copy(id = "2"))))))
    }

    @Test fun identifiedRemakesRemainDistinctAndLimitAppliesAfterSorting() {
        val items = (1..25).map { id -> TrackingProjection.card(TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, id.toString()), title = "Same title")) }
        assertEquals(20, UpNextFeed.build(items).size)
        assertEquals(25, items.map { TrackingProjection.hero(requireNotNull(it.tracking)) }.size)
        assertEquals(25, items.map { it.key }.distinct().size)
    }
}
