/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import coil.size.Size
import fr.sygix.sygixos.core.designsystem.GlassBackdrop
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.model.HeroItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

private class TransformingImageLoader : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult {
        var bitmap = Bitmap.createBitmap(1920, 1080, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.WHITE) }
        request.transformations.forEach { bitmap = it.transform(bitmap, Size.ORIGINAL) }
        if (bitmap.config == Bitmap.Config.HARDWARE) bitmap = bitmap.copy(Bitmap.Config.ARGB_8888, false)
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        return SuccessResult(BitmapDrawable(resources, bitmap), request, DataSource.MEMORY)
    }
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroPosterLayersTest {

    @get:Rule
    val compose = createComposeRule()

    private val backdrop = GlassBackdrop()

    @After
    fun resetImageLoader() = Coil.reset()

    private fun poster(id: String, title: String = "") =
        HeroItem(id = id, title = title, imageUrl = "https://example.invalid/$id.jpg", sourcePackage = "com.source")

    private fun show(vararg items: HeroItem) {
        Coil.setImageLoader(TransformingImageLoader())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            HeroStage(
                items = items.toList(),
                validatedVisuals = items.mapNotNull { it.imageUrl }.toSet(),
                active = true,
                visible = true,
                focusRequester = FocusRequester(),
                onOpen = {},
                backdrop = backdrop,
            )
        }
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun cornerLuma(): Int {
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val pixel = bitmap.getPixel(bitmap.width - 40, 16)
        return (Color.red(pixel) + Color.green(pixel) + Color.blue(pixel)) / 3
    }

    @Test
    @GraphicsMode(GraphicsMode.Mode.NATIVE)
    fun `corner veil behind the capsule keeps its strength through the ken burns`() {
        show(poster("h1"))
        advance(Motion.HERO_VIDEO_FADE_MS + 300L)
        val early = cornerLuma()
        assertTrue("coin voilé : $early", early < 200)
        advance(Motion.HERO_KEN_BURNS_MS.toLong())
        assertEquals(1.08f, backdrop.layers.single().zoom.value, 0.001f)
        val late = cornerLuma()
        assertTrue("début $early, fin $late", late <= early + 2)
    }

    @Test
    fun `going back during a crossfade fades out from the current opacity`() {
        val first = poster("h1", "Premier")
        val second = poster("h2", "Second")
        show(first, second)
        advance(2_000)
        assertEquals(listOf(first.imageUrl), backdrop.layers.map { it.key })
        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        advance(Motion.HERO_CROSSFADE_MS / 2L)
        val upper = backdrop.layers.last()
        assertEquals(second.imageUrl, upper.key)
        val before = upper.fade.value
        assertTrue(before > 0.2f && before < 0.95f)
        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionLeft)
            keyUp(Key.DirectionLeft)
        }
        advance(32)
        assertEquals(second.imageUrl, backdrop.layers.last().key)
        assertEquals(1f, backdrop.layers.first().fade.value, 0.001f)
        val after = backdrop.layers.last().fade.value
        assertTrue("avant $before, après $after", after in (before - 0.15f)..(before + 0.05f))
        advance(Motion.HERO_CROSSFADE_MS.toLong())
        assertEquals(listOf(first.imageUrl), backdrop.layers.map { it.key })
        assertEquals(1f, backdrop.layers.single().fade.value, 0.001f)
    }
}
