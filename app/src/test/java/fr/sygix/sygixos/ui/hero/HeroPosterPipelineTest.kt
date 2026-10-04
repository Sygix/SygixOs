/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.size.Size
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class HeroPosterPipelineTest {

    private val spec = PosterSpec.of(1920, 1080, 640f, 300f)

    private fun solid(width: Int, height: Int, color: Int): Bitmap =
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }

    private fun luma(color: Int): Int = (Color.red(color) + Color.green(color) + Color.blue(color)) / 3

    private fun Bitmap.software(): Bitmap = if (config == Bitmap.Config.HARDWARE) copy(Bitmap.Config.ARGB_8888, false) else this

    @Test
    fun `poster size follows the window and never exceeds 1080p`() {
        val uhd = PosterSpec.of(3840, 2160, 1280f, 600f)
        assertEquals(1920, uhd.width)
        assertEquals(1080, uhd.height)
        assertEquals(640f, uhd.cornerWidth, 0.01f)
        assertEquals(1920, spec.width)
        assertEquals(240, spec.backdrop.width)
        assertEquals(135, spec.backdrop.height)
    }

    @Test
    fun `a 4K poster is turned into an opaque 1080p image with its bottom and left veils drawn once`() = runBlocking {
        val output = VeiledPosterTransformation(spec).transform(solid(3840, 2160, Color.WHITE), Size.ORIGINAL).software()
        assertEquals(1920, output.width)
        assertEquals(1080, output.height)
        assertFalse(output.hasAlpha())
        assertEquals(255, luma(output.getPixel(1400, 300)))
        assertTrue(luma(output.getPixel(20, 1070)) < 40)
        assertEquals(255, luma(output.getPixel(1900, 4)))
        assertTrue(luma(output.getPixel(960, 1070)) < 70)
    }

    @Test
    fun `glass backdrop is a small, blurred and veiled copy of the poster`() = runBlocking {
        val input = solid(1920, 1080, Color.WHITE)
        Canvas(input).drawRect(0f, 0f, 960f, 1080f, Paint().apply { color = Color.BLACK })
        val output = GlassBackdropTransformation(spec).transform(input, Size.ORIGINAL).software()
        assertEquals(240, output.width)
        assertEquals(135, output.height)
        assertFalse(output.hasAlpha())
        val left = luma(output.getPixel(116, 40))
        val right = luma(output.getPixel(124, 40))
        assertTrue("bord adouci : $left -> $right", left > 10 && right < 245 && right > left)
        assertTrue(luma(output.getPixel(200, 132)) < luma(output.getPixel(200, 40)))
    }

    @Test
    fun `box blur radius matches the 28 px blur of the mockup at backdrop scale`() {
        assertEquals(3, BoxBlur.radiusFor(28f / PosterSpec.BACKDROP_DIVISOR))
    }
}
