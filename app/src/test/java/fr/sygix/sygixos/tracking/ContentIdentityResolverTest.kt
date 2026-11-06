/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class ContentIdentityResolverTest {
    private val show = TrackingIdentity("fixture", TrackingType.SHOW, "1")
    private val localized = UpNextItem(7, UpNextSourceEntry("fixture.player"), UpNextContentType.EPISODE, "Les Sorcières de Brumeval - S1 E4",
        seriesTitle = "Les Sorcières de Brumeval", season = "1", episode = "4", year = 2023)

    @Test fun ambiguousRemakesAndDifferentTypesDoNotResolve() {
        val one = CatalogCandidate(TrackingIdentity("fixture", TrackingType.MOVIE, "1"), "Fixture", 1982)
        val two = one.copy(identity = one.identity.copy(id = "2"), year = 2011)
        assertNull(ExactContentIdentity.resolve(ContentQuery(TrackingType.MOVIE, "Fixture"), listOf(one, two)))
        assertEquals(two, ExactContentIdentity.resolve(ContentQuery(TrackingType.MOVIE, "Fixture", 2011), listOf(one, two)))
        assertNull(ExactContentIdentity.resolve(ContentQuery(TrackingType.SHOW, "Fixture", 1982), listOf(one)))
    }

    @Test fun conflictingExternalIdsNeverMergeEvenWithTheSameTitle() {
        val item = CatalogCandidate(show, "Original title", externalIds = mapOf("tvdb" to "42"))
        assertNull(ExactContentIdentity.resolve(ContentQuery(TrackingType.SHOW, "Original title", externalIds = mapOf("tvdb" to "9")), listOf(item)))
    }

    @Test fun failedIdentificationIsSearchedAgainOnlyAfterItsRetryDelay() = runTest {
        var now = 0L
        var searches = 0
        val match = CatalogCandidate(show, "Fixture")
        val resolver = BoundedIdentityResolver({ searches++; if (searches == 1) emptyList() else listOf(match) }, { now })
        val query = ContentQuery(TrackingType.SHOW, "Fixture")
        assertNull(resolver.resolve(query))
        now = IDENTITY_RETRY_MS - 1
        assertNull(resolver.resolve(query))
        assertEquals(1, searches)
        now = IDENTITY_RETRY_MS
        assertEquals(match, resolver.resolve(query))
        now = 10 * IDENTITY_RETRY_MS
        assertEquals(match, resolver.resolve(query))
        assertEquals(2, searches)
    }

    @Test fun localizedAppTitleWithoutExternalIdResolvesThroughTheAccountAliases() {
        val episode = TrackingContent(TrackingIdentity("fixture", TrackingType.EPISODE, "21"), show, "Brumeval Witches", season = 2, episode = 1,
            aliases = setOf("Les Sorcières de Brumeval"))
        assertEquals(LocalTrackingIdentity(show, show), AccountContentIdentity.resolve(localized, listOf(episode)))
        assertNull(AccountContentIdentity.resolve(localized, listOf(episode.copy(aliases = emptySet()))))
        assertNull(AccountContentIdentity.resolve(localized, listOf(episode.copy(membership = AccountMembership.ABSENT))))
        val other = episode.copy(identity = episode.identity.copy(id = "31"), parent = show.copy(id = "2"))
        assertNull(AccountContentIdentity.resolve(localized, listOf(episode, other)))
    }
}
