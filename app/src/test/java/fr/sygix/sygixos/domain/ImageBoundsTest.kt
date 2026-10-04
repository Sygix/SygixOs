/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class ImageBoundsTest {

    @Test
    fun `a 4K screen is capped to 1080p`() {
        assertEquals(PixelSize(1920, 1080), ImageBounds.screen(3840, 2160))
    }

    @Test
    fun `a 1080p screen keeps its size`() {
        assertEquals(PixelSize(1920, 1080), ImageBounds.screen(1920, 1080))
    }

    @Test
    fun `a 4K banner is decoded at tile size`() {
        assertEquals(PixelSize(480, 270), ImageBounds.artwork(3840, 2160))
    }

    @Test
    fun `small artwork is never enlarged`() {
        assertEquals(PixelSize(320, 180), ImageBounds.artwork(320, 180))
    }

    @Test
    fun `aspect ratio is kept when one side limits`() {
        assertEquals(PixelSize(216, 270), ImageBounds.artwork(432, 540))
    }
}
