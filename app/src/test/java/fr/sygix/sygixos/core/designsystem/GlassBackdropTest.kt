/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import kotlin.math.roundToInt

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class GlassBackdropTest {

    @get:Rule
    val compose = createComposeRule()

    private fun backdropOf(color: Int): GlassBackdrop = GlassBackdrop().apply {
        val layer = layer("poster")
        layer.image = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }.asImageBitmap()
        layer.blurred = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }.asImageBitmap()
        runBlocking { layer.fade.snapTo(1f) }
    }

    private fun showGlassOver(backdrop: GlassBackdrop) {
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red).glassBackdropOrigin(backdrop)) {
                CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                    GlassSurface(Modifier.testTag("glass").offset(300.dp, 300.dp).size(200.dp, 120.dp), look = GlassLook.Dock) {}
                }
            }
        }
        compose.waitForIdle()
    }

    private fun pixelAt(xDp: Int, yDp: Int): Int {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val density = compose.density.density
        return bitmap.getPixel((xDp * density).roundToInt(), (yDp * density).roundToInt())
    }

    private fun expectedOver(backdrop: Int): Int {
        val tint = SygixColors.GlassTint
        fun mix(channel: Int, tintChannel: Float) = (channel * (1f - tint.alpha) + tintChannel * 255f * tint.alpha).roundToInt()
        return android.graphics.Color.rgb(
            mix(android.graphics.Color.red(backdrop), tint.red),
            mix(android.graphics.Color.green(backdrop), tint.green),
            mix(android.graphics.Color.blue(backdrop), tint.blue),
        )
    }

    @Test
    fun `dock glass shows the blurred hero visual under the dark tint of the mockup`() {
        val gray = android.graphics.Color.rgb(128, 128, 128)
        showGlassOver(backdropOf(gray))
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassActive, true))
        val inside = pixelAt(400, 360)
        val expected = expectedOver(gray)
        listOf(android.graphics.Color::red, android.graphics.Color::green, android.graphics.Color::blue).forEach { channel ->
            assertEquals(channel(expected).toFloat(), channel(inside).toFloat(), 3f)
        }
        assertTrue(android.graphics.Color.red(inside) < 100)
        assertEquals(Color.Red.toArgb(), pixelAt(100, 100))
    }

    @Test
    fun `dark poster gives a dark glass instead of a uniform grey`() {
        val dark = android.graphics.Color.rgb(20, 20, 24)
        showGlassOver(backdropOf(dark))
        val inside = pixelAt(400, 360)
        assertTrue("verre : ${android.graphics.Color.red(inside)}", android.graphics.Color.red(inside) < 40)
    }

    @Test
    fun `glass mockup values`() {
        assertEquals(22, (SygixColors.GlassTint.red * 255).roundToInt())
        assertEquals(22, (SygixColors.GlassTint.green * 255).roundToInt())
        assertEquals(28, (SygixColors.GlassTint.blue * 255).roundToInt())
        assertEquals(0.36f, SygixColors.GlassTint.alpha, 0.01f)
        assertEquals(0.16f, SygixColors.GlassBorder.alpha, 0.01f)
        assertEquals(0.32f, SygixColors.GlassHighlight.alpha, 0.01f)
    }

    private fun quadrants(): Bitmap = Bitmap.createBitmap(400, 224, Bitmap.Config.ARGB_8888).apply {
        eraseColor(android.graphics.Color.BLACK)
        val bright = IntArray(300 * 168) { android.graphics.Color.WHITE }
        setPixels(bright, 0, 300, 100, 56, 300, 168)
    }

    @Test
    fun `glass follows the zoom and the fade of the poster under it`() {
        val gray = android.graphics.Color.rgb(128, 128, 128)
        val backdrop = backdropOf(gray)
        val top = backdrop.layer("next")
        top.image = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888).asImageBitmap()
        top.blurred = quadrants().asImageBitmap()
        runBlocking {
            top.fade.snapTo(0.5f)
            top.zoom.snapTo(1.08f)
        }
        compose.setContent {
            Box(Modifier.fillMaxSize().background(Color.Red).glassBackdropOrigin(backdrop)) {
                CompositionLocalProvider(LocalGlassBackdrop provides backdrop) {
                    GlassSurface(Modifier.offset(150.dp, 60.dp).size(160.dp, 130.dp), look = GlassLook.Dock) {}
                }
            }
        }
        compose.waitForIdle()
        val edgeX = 480f - 0.27f * 960f
        val edgeY = 270f - 0.27f * 540f
        val dark = expectedOver(android.graphics.Color.rgb(64, 64, 64))
        val bright = expectedOver(android.graphics.Color.rgb(191, 191, 191))
        fun assertLevel(expected: Int, xDp: Float, yDp: Float) {
            val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
            val density = compose.density.density
            val actual = bitmap.getPixel((xDp * density).roundToInt(), (yDp * density).roundToInt())
            assertEquals("($xDp, $yDp)", android.graphics.Color.green(expected).toFloat(), android.graphics.Color.green(actual).toFloat(), 8f)
        }
        assertLevel(dark, edgeX - 8f, edgeY + 20f)
        assertLevel(bright, edgeX + 8f, edgeY + 20f)
        assertLevel(dark, edgeX + 20f, edgeY - 8f)
        assertLevel(bright, edgeX + 20f, edgeY + 8f)
    }

    @Test
    fun `missing blurred copy falls back to the live glass`() {
        val backdrop = GlassBackdrop()
        val layer = backdrop.layer("poster")
        layer.image = Bitmap.createBitmap(64, 36, Bitmap.Config.ARGB_8888).asImageBitmap()
        runBlocking { layer.fade.snapTo(1f) }
        compose.setContent {
            val haze = rememberHazeState()
            CompositionLocalProvider(LocalHazeState provides haze, LocalGlassBackdrop provides backdrop) {
                Box(Modifier.fillMaxSize().hazeSource(haze).glassBackdropOrigin(backdrop)) {
                    GlassSurface(Modifier.testTag("glass").size(100.dp)) {}
                }
            }
        }
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassActive, true))
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassPrecomputed, false))
        compose.runOnIdle { layer.blurred = Bitmap.createBitmap(8, 4, Bitmap.Config.ARGB_8888).asImageBitmap() }
        compose.onNodeWithTag("glass").assert(SemanticsMatcher.expectValue(GlassPrecomputed, true))
    }
}
