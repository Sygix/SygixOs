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
    fun `a 1080p TV decodes banners at the focused tile size`() {
        assertEquals(PixelSize(332, 187), ImageBounds.tile(332))
        assertEquals(PixelSize(332, 187), ImageBounds.fit(3840, 2160, 332, 187))
    }

    @Test
    fun `a denser screen keeps the banner cap`() {
        assertEquals(PixelSize(480, 270), ImageBounds.tile(664))
    }

    @Test
    fun `small artwork is never enlarged`() {
        assertEquals(PixelSize(320, 180), ImageBounds.fit(320, 180, 332, 187))
    }

    @Test
    fun `aspect ratio is kept when one side limits`() {
        assertEquals(PixelSize(150, 187), ImageBounds.fit(432, 540, 332, 187))
    }

    @Test
    fun `sample size keeps the decoded image at least as wide as its target`() {
        assertEquals(2, ImageBounds.sampleSize(360, 128))
        assertEquals(1, ImageBounds.sampleSize(360, 200))
        assertEquals(4, ImageBounds.sampleSize(360, 64))
        assertEquals(1, ImageBounds.sampleSize(100, 128))
        assertEquals(1, ImageBounds.sampleSize(360, 0))
    }
}
