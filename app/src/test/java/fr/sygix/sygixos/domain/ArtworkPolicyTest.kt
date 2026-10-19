/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.*
import org.junit.Test

class ArtworkPolicyTest {
    @Test fun `destinations apply independent exact thresholds`() {
        for (width in listOf(395, 396, 397, 639, 640, 641, 1599, 1600, 1601)) {
            val dimensions = ArtworkDimensions(width, 200)
            assertEquals(width >= 640, ArtworkPolicy.Hero.accepts(dimensions))
            assertEquals(width >= 1600, ArtworkPolicy.Shelf(1600).accepts(dimensions))
            assertEquals(width >= 396, ArtworkPolicy.Card(396).accepts(dimensions))
        }
        val portrait = ArtworkDimensions(800, 1200)
        assertTrue(ArtworkPolicy.Card(396).accepts(portrait))
        assertFalse(ArtworkPolicy.Hero.accepts(portrait))
        assertFalse(ArtworkPolicy.Shelf(396).accepts(portrait))
        assertTrue(ArtworkPolicy.Hero.accepts(ArtworkDimensions(3840, 2160)))
    }

    @Test fun `reads never renew freshness or retention`() {
        val fetched = 1_000L
        assertTrue(ArtworkAge.fresh(fetched, fetched + ArtworkAge.FRESH_MS - 1))
        assertFalse(ArtworkAge.fresh(fetched, fetched + ArtworkAge.FRESH_MS))
        assertTrue(ArtworkAge.usable(fetched, fetched + ArtworkAge.RETAIN_MS - 1))
        assertFalse(ArtworkAge.usable(fetched, fetched + ArtworkAge.RETAIN_MS))
        assertFalse(ArtworkAge.usable(0, fetched))
        assertFalse(ArtworkAge.usable(fetched + 1, fetched))
    }
}
