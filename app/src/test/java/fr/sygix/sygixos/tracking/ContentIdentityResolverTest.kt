/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.junit.Assert.*
import org.junit.Test

class ContentIdentityResolverTest {
    @Test fun ambiguousRemakesAndDifferentTypesDoNotResolve() {
        val one = CatalogCandidate(TrackingIdentity("fixture", TrackingType.MOVIE, "1"), "Fixture", 1982)
        val two = one.copy(identity = one.identity.copy(id = "2"), year = 2011)
        assertNull(ExactContentIdentity.resolve(ContentQuery(TrackingType.MOVIE, "Fixture"), listOf(one, two)))
        assertEquals(two, ExactContentIdentity.resolve(ContentQuery(TrackingType.MOVIE, "Fixture", 2011), listOf(one, two)))
        assertNull(ExactContentIdentity.resolve(ContentQuery(TrackingType.SHOW, "Fixture", 1982), listOf(one)))
    }

    @Test fun verifiedIdsResolveLocalizedTitlesButConflictingIdsNeverMerge() {
        val item = CatalogCandidate(TrackingIdentity("fixture", TrackingType.SHOW, "1"), "Original title", externalIds = mapOf("tvdb" to "42"))
        assertEquals(item, ExactContentIdentity.resolve(ContentQuery(TrackingType.SHOW, "Titre localisé", externalIds = mapOf("tvdb" to "42")), listOf(item)))
        assertNull(ExactContentIdentity.resolve(ContentQuery(TrackingType.SHOW, "Original title", externalIds = mapOf("tvdb" to "9")), listOf(item)))
    }
}
