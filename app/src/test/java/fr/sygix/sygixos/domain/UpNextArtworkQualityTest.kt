/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.*
import org.junit.Test

class UpNextArtworkQualityTest {
    @Test fun `quality boundary is twice displayed width and not hero minimum`() {
        assertFalse(UpNextArtworkQuality.usable(395, 198))
        assertTrue(UpNextArtworkQuality.usable(396, 198))
        assertTrue(UpNextArtworkQuality.usable(800, 400))
        assertFalse(UpNextArtworkQuality.usable(1080, 600))
    }
    @Test fun `missing decoded or displayed dimensions fail closed`() {
        assertFalse(UpNextArtworkQuality.usable(0, 198))
        assertFalse(UpNextArtworkQuality.usable(-1, 198))
        assertFalse(UpNextArtworkQuality.usable(400, 0))
        assertFalse(UpNextArtworkQuality.usable(400, -1))
    }
    @Test fun `portrait means taller than wide`() {
        assertTrue(UpNextArtworkQuality.portrait(400, 600))
        assertFalse(UpNextArtworkQuality.portrait(600, 400))
        assertFalse(UpNextArtworkQuality.portrait(400, 400))
    }
    @Test fun `placeholder icon keeps its size on cards and fits the upper half of a thumbnail`() {
        assertEquals(32f, UpNextArtworkQuality.placeholderIcon(32f, 111.375f), 0f)
        assertEquals(15.75f, UpNextArtworkQuality.placeholderIcon(32f, 31.5f), 0f)
    }
}
