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

    @Test fun providerPresentationIsDistinctAndTheSelectedReaderKeepsPriority() {
        val heroes = mutableListOf<String>()
        for (provider in TrackingProviderKind.entries) {
            val content = next.copy(identity = next.identity.copy(provider = provider.id), parent = parent.copy(provider = provider.id))
            val card = TrackingProjection.card(content)
            val hero = TrackingProjection.hero(content)
            assertEquals(provider.displayName, card.source.label)
            assertEquals(provider.displayName, hero.sourceLabel)
            assertEquals("", card.source.packageName)
            assertNull(card.source.icon)
            heroes += hero.id
            val selected = content.copy(readerPackage = "fixture.player", readerName = "Preferred player")
            assertEquals("Preferred player", TrackingProjection.card(selected).source.label)
            assertEquals("Preferred player", TrackingProjection.hero(selected).sourceLabel)
            assertEquals("fixture.player", TrackingProjection.hero(selected).sourcePackage)
        }
        assertEquals(2, heroes.distinct().size)
    }

    @Test fun accountMoviesAndEpisodesKeepTheirOwnCardShape() {
        val movie = TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "slug-7"), title = "Brumeval", poster = "https://fixture/poster.jpg")
        val movieCard = TrackingProjection.card(movie)
        assertEquals(UpNextContentType.MOVIE, movieCard.type)
        assertEquals(ProgramKind.WATCHLIST, movieCard.watchNextType)
        assertNull(movieCard.seriesTitle)
        assertTrue(movieCard.portrait)
        assertEquals("https://fixture/poster.jpg", movieCard.imageUrl)
        assertEquals(movie.identity.hashCode().toLong(), movieCard.id)
        assertEquals(ProgramKind.WATCHLIST, TrackingProjection.hero(movie).kind)
        val episodeCard = TrackingProjection.card(next.copy(image = "https://fixture/still.jpg", poster = "https://fixture/poster.jpg"))
        assertEquals(UpNextContentType.EPISODE, episodeCard.type)
        assertEquals(ProgramKind.NEXT, episodeCard.watchNextType)
        assertEquals("Fixture", episodeCard.seriesTitle)
        assertFalse(episodeCard.portrait)
        assertEquals("https://fixture/still.jpg", episodeCard.imageUrl)
        assertEquals(3L, episodeCard.id)
        assertEquals(ProgramKind.NEXT, TrackingProjection.hero(next).kind)
    }

    @Test fun watchedMoviesAreNoLongerAccountCandidates() {
        val movie = TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "7"), title = "Brumeval")
        assertEquals(listOf(movie), TrackingProjection.candidates(listOf(movie)))
        assertEquals(emptyList<TrackingContent>(), TrackingProjection.candidates(listOf(movie.copy(movieState = 1))))
    }

    @Test fun seenAccountEpisodePublishedByAnAppLeavesTheRow() {
        val seen = next.copy(seen = true)
        val local = UpNextItem(9, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Fixture", tracking = seen)
        assertEquals(emptyList<UpNextItem>(), TrackingProjection.merge(listOf(local), emptyList()))
        val unknown = local.copy(tracking = seen.copy(membership = AccountMembership.UNKNOWN))
        assertEquals(listOf(unknown), TrackingProjection.merge(listOf(unknown), emptyList()))
    }

    @Test fun unidentifiedResumeTakesTheAccountIdentityOnlyForTheSameEpisode() {
        val tracked = next.copy(externalIds = mapOf("tvdb" to "42"))
        fun resume(episode: String) = UpNextItem(1, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Fixture", season = "2", episode = episode,
            watchNextType = ProgramKind.CONTINUE, externalIds = mapOf("tvdb" to "42"))
        val same = TrackingProjection.merge(listOf(resume("3")), listOf(tracked)).single().tracking
        assertEquals(tracked.identity, same?.identity)
        assertEquals(AccountMembership.PRESENT, same?.membership)
        assertNull(TrackingProjection.merge(listOf(resume("4")), listOf(tracked)).single().tracking)
    }

    @Test fun unrelatedAppItemStaysBesideTheAccountContent() {
        val other = UpNextItem(9, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Autre fiction", externalIds = mapOf("tvdb" to "99"))
        assertEquals(listOf(other, TrackingProjection.card(next)), TrackingProjection.merge(listOf(other), listOf(next)))
    }

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

    @Test fun identifiedAppItemKeepsItsRankAndSourcesWhileShowingTheAccountEpisode() {
        val local = UpNextItem(41, UpNextSourceEntry("fixture.player", intentUri = "fixture://episode3"), UpNextContentType.EPISODE, "Titre localisé",
            season = "2", episode = "3", watchNextType = ProgramKind.WATCH_NEXT, engagement = 5_000, tracking = next)
        val merged = TrackingProjection.merge(listOf(local), listOf(next)).single()
        assertEquals(41L, merged.id)
        assertEquals(5_000L, merged.engagement)
        assertEquals(ProgramKind.WATCH_NEXT, merged.watchNextType)
        assertEquals(local.sources, merged.sources)
        assertEquals(next.identity, merged.tracking?.identity)
    }

    @Test fun seenLocalResumeLeavesTheAccountNextEpisodeWithItsActivityButNotAsInProgress() {
        val watched = next.copy(identity = next.identity.copy(id = "14"), season = 1, episode = 4, seen = true)
        val accountNext = next.copy(identity = next.identity.copy(id = "21"), season = 2, episode = 1)
        val resume = UpNextItem(3, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Titre localisé", season = "1", episode = "4",
            positionMillis = 600_000, watchNextType = ProgramKind.CONTINUE, engagement = 9_000, tracking = watched)
        val merged = TrackingProjection.merge(listOf(resume), listOf(accountNext)).single()
        assertEquals(accountNext.identity, merged.tracking?.identity)
        assertEquals(ProgramKind.NEXT, merged.watchNextType)
        assertEquals(9_000L, merged.engagement)
        assertNull(merged.positionMillis)
    }

    @Test fun specialsAndPlaceholderEpisodesAreNeverTheNextEpisode() {
        val special = next.copy(identity = next.identity.copy(id = "1"), season = 0, episode = 1)
        val placeholder = next.copy(identity = next.identity.copy(id = "2"), season = 2, episode = 1, episodeTitle = "TBA")
        assertEquals(listOf(next.identity), TrackingProjection.candidates(listOf(special, placeholder, next)).map { it.identity })
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

    @Test fun samePlayerKeepsTheExactNextEpisodeIntentAlongsideSeenResumeIntent() {
        val seen = next.copy(identity = next.identity.copy(id = "5"), episode = 5, seen = true)
        val oldSource = UpNextSourceEntry("fixture.player", intentUri = "fixture://episode5", targetIdentity = seen.identity)
        val exactSource = UpNextSourceEntry("fixture.player", intentUri = "fixture://episode3", targetIdentity = next.identity)
        val old = UpNextItem(1, oldSource, UpNextContentType.EPISODE, "Fixture", season = "2", episode = "5", watchNextType = ProgramKind.CONTINUE, tracking = seen)
        val fresh = UpNextItem(2, exactSource, UpNextContentType.EPISODE, "Fixture", season = "2", episode = "3", tracking = next)
        val selected = UpNextFeed.build(TrackingProjection.merge(listOf(old, fresh), listOf(next))).single()
        assertEquals(next.identity, selected.tracking?.identity)
        assertEquals("fixture://episode3", selected.sources.firstOrNull { it.targetIdentity == selected.tracking?.identity }?.intentUri)
    }
}
