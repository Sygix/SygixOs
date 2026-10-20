/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.*
import org.junit.Test
import kotlin.random.Random

class ArtworkFingerprintTest {
    private val item = HeroItem("preview-1", "Fiction", imageUrl = "content://fixture/poster", sourcePackage = "fixture.app", launchUri = "fixture://film/one")

    @Test fun progressAndRepublishedRowsKeepImageIdentityAndOrder() {
        val republished = item.copy(id = "preview-20", positionMillis = 200, durationMillis = 1_000, engagement = 400)
        assertEquals(item.artwork.identity, republished.artwork.identity)
        assertNotEquals(item.fullFingerprint, republished.fullFingerprint)
        val other = item.copy(id = "preview-2", title = "Other fiction", launchUri = "fixture://film/two")
        val order = HeroSessionOrder(Random(17))
        val original = order.arrange(listOf(item, other)).map { it.artwork.identity }
        assertEquals(original, order.arrange(listOf(other, republished)).map { it.artwork.identity })
    }

    @Test fun publishedUriChangesIdentityButChosenCandidateDoesNot() {
        assertNotEquals(item.artwork.identity, item.copy(posterUrl = "content://fixture/changed").artwork.identity)
        assertEquals(item.artwork.identity, item.copy(imageUrl = "content://fixture/selected").artwork.identity)
    }
}
