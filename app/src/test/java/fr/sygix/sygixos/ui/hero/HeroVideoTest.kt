/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.TextureView
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
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
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.model.HeroItem
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class FakeVideo : HeroVideo {
    override var firstFrameRendered by mutableStateOf(false)
    override var renderedItem by mutableStateOf<String?>(null)
    override var readyItem by mutableStateOf<String?>(null)
    override var loadedItem by mutableStateOf<String?>(null)
    override val videoAspect: Float = 16f / 9f
    var playing = false
    val prepared = mutableListOf<String>()
    var parks = 0
    var releases = 0

    override fun show(id: String, url: String, play: Boolean, loop: Boolean) {
        if (id != loadedItem) {
            loadedItem = id
            prepared += id
            firstFrameRendered = false
            renderedItem = null
            readyItem = null
        }
        playing = play
    }

    override fun park() {
        parks++
        playing = false
    }

    override fun clear() {
        loadedItem = null
        playing = false
        firstFrameRendered = false
        renderedItem = null
        readyItem = null
    }

    fun render(id: String) {
        readyItem = id
        if (loadedItem == id) {
            firstFrameRendered = true
            renderedItem = id
        }
    }

    override fun attach(textureView: TextureView) = Unit
    override fun release() {
        releases++
        loadedItem = null
        playing = false
    }
}

private class ReadyLoader : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult =
        SuccessResult(ColorDrawable(Color.DKGRAY), request, DataSource.MEMORY)
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroVideoTest {

    @get:Rule
    val compose = createComposeRule()

    private val video = FakeVideo()
    private var motion by mutableStateOf(true)
    private var visible by mutableStateOf(true)
    private val owner = object : LifecycleOwner {
        val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
        override val lifecycle: Lifecycle get() = registry
    }

    @After
    fun resetImageLoader() = Coil.reset()

    private fun show(items: List<HeroItem>) {
        Coil.setImageLoader(ReadyLoader())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(
                LocalHeroVideoFactory provides HeroVideoFactory { _: Context, _, _ -> video },
                LocalLifecycleOwner provides owner,
            ) {
                HeroStage(
                    items = items,
                    validatedVisuals = items.mapNotNull { it.imageUrl }.toSet(),
                    active = true,
                    visible = visible,
                    focusRequester = FocusRequester(),
                    onOpen = {},
                    motion = motion,
                )
            }
        }
        advance(500)
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun background(millis: Long) {
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        advance(millis)
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        advance(100)
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        advance(100)
    }

    private fun clip(id: String, title: String) =
        HeroItem(id = id, title = title, videoUrl = "https://example.invalid/$id.mp4", sourcePackage = "com.example.player")

    private fun picture(id: String, title: String) =
        HeroItem(id = id, title = title, imageUrl = "https://example.invalid/$id.jpg", sourcePackage = "com.example.player")

    @Test
    fun `preview video is prepared but not played under the startup screen`() {
        motion = false
        show(listOf(clip("v1", "Vidéo fictive")))
        assertEquals(listOf("v1"), video.prepared)
        assertFalse(video.playing)
        compose.runOnIdle {
            motion = true
            Snapshot.sendApplyNotifications()
        }
        advance(100)
        assertTrue(video.playing)
        assertEquals(listOf("v1"), video.prepared)
    }

    @Test
    fun `video whose first frame is late gives way to the next program and keeps preparing`() {
        val first = picture("p1", "Premier titre fictif")
        val slow = clip("v1", "Vidéo fictive lente")
        val third = picture("p3", "Troisième titre fictif")
        show(listOf(first, slow, third))
        compose.onNodeWithText(first.title, useUnmergedTree = true).assertExists()

        press(Key.DirectionRight)
        assertEquals(listOf("v1"), video.prepared)
        advance(Motion.HERO_VISUAL_TIMEOUT_MS / 2)
        compose.onNodeWithText(first.title, useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(slow.title, useUnmergedTree = true).assertCountEquals(0)

        advance(Motion.HERO_VISUAL_TIMEOUT_MS + Motion.HERO_CROSSFADE_MS + 500L)
        compose.onNodeWithText(third.title, useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(slow.title, useUnmergedTree = true).assertCountEquals(0)
        assertEquals("v1", video.loadedItem)
        assertTrue(video.parks > 0)
        assertFalse(video.playing)

        compose.runOnIdle {
            video.render("v1")
            Snapshot.sendApplyNotifications()
        }
        advance(100)
        press(Key.DirectionLeft)
        advance(Motion.HERO_CROSSFADE_MS + 500L)
        compose.onNodeWithText(slow.title, useUnmergedTree = true).assertExists()
        assertTrue(video.playing)
        assertEquals(listOf("v1"), video.prepared)
    }

    @Test
    fun `the player is released only in the background and the video loads again on return`() {
        show(listOf(clip("v1", "Vidéo fictive")))
        compose.runOnIdle {
            visible = false
            Snapshot.sendApplyNotifications()
        }
        advance(100)
        assertEquals(null, video.loadedItem)
        compose.runOnIdle {
            visible = true
            Snapshot.sendApplyNotifications()
        }
        advance(100)
        assertEquals(0, video.releases)
        background(Motion.HERO_VIDEO_START_TIMEOUT_MS * 2)
        assertEquals(1, video.releases)
        assertEquals(listOf("v1", "v1", "v1"), video.prepared)
        assertTrue(video.playing)
    }

    @Test
    fun `the slideshow does not advance in the background`() {
        val first = picture("p1", "Premier titre fictif")
        val second = picture("p2", "Second titre fictif")
        show(listOf(first, second))
        background(Motion.HERO_DWELL_MS * 3 / 2)
        compose.onNodeWithText(first.title, useUnmergedTree = true).assertExists()
        advance(Motion.HERO_DWELL_MS + Motion.HERO_CROSSFADE_MS + 500L)
        compose.onNodeWithText(second.title, useUnmergedTree = true).assertExists()
    }

    @Test
    fun `a parked slow video becomes eligible after its preparation is cancelled while hidden`() {
        val first = picture("p1", "First")
        val slow = clip("v1", "Slow")
        val third = picture("p3", "Third")
        show(listOf(first, slow, third))
        press(Key.DirectionRight)
        advance(Motion.HERO_VISUAL_TIMEOUT_MS + Motion.HERO_CROSSFADE_MS + 500L)
        assertEquals("v1", video.loadedItem)
        compose.onNodeWithText(third.title, useUnmergedTree = true).assertExists()
        compose.runOnIdle { visible = false }
        advance(100)
        assertEquals(null, video.loadedItem)
        compose.runOnIdle { visible = true }
        advance(100)
        press(Key.DirectionLeft)
        assertEquals(listOf("v1", "v1"), video.prepared)
        compose.runOnIdle { video.render("v1") }
        advance(Motion.HERO_CROSSFADE_MS + 100L)
        compose.onNodeWithText(slow.title, useUnmergedTree = true).assertExists()
        assertTrue(video.playing)
    }
}
