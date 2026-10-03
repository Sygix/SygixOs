/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.Recomposer
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class InstantImageLoader : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult =
        SuccessResult(ColorDrawable(Color.WHITE), request, DataSource.MEMORY)
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class IdleFrameTest {

    @get:Rule
    val compose = createComposeRule()

    private val dock = listOf(app("com.dock", "Dock"))
    private val grid = (0 until 8).map { app("com.g$it", "Grille $it") }

    @After
    fun resetImageLoader() = Coil.reset()

    private fun show(dock: List<fr.sygix.sygixos.model.TvApp>, hero: HeroState) {
        compose.mainClock.autoAdvance = false
        compose.setContent { TestHome(catalog = catalogOf(dock = dock, grid = grid), hero = hero) }
        settle()
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(20_000)
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        settle()
    }

    private fun stateWritesDuring(millis: Long): Int {
        var writes = 0
        val handle = Snapshot.registerApplyObserver { changed, _ -> writes += changed.size }
        try {
            compose.mainClock.advanceTimeBy(millis)
            compose.waitForIdle()
        } finally {
            handle.dispose()
        }
        return writes
    }

    private fun recompositionsDuring(millis: Long): Long {
        fun count() = Recomposer.runningRecomposers.value.sumOf { it.changeCount }
        val before = count()
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
        return count() - before
    }

    @Test
    fun `animated fallback gradient on the hero never recomposes`() {
        show(dock, heroStateOf(items = emptyList()))
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        assertTrue(stateWritesDuring(1_000) > 0)
        assertEquals(0L, recompositionsDuring(5_000))
    }

    @Test
    fun `focused dock over the animated hero never recomposes`() {
        show(dock, heroStateOf(items = emptyList()))
        val beforePress = Recomposer.runningRecomposers.value.sumOf { it.changeCount }
        press(Key.DirectionDown)
        assertTrue(Recomposer.runningRecomposers.value.sumOf { it.changeCount } > beforePress)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
        assertEquals(0L, recompositionsDuring(5_000))
    }

    @Test
    fun `focused grid is idle`() {
        show(dock, heroStateOf(items = emptyList()))
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.g0").assertIsFocused()
        assertEquals(0, stateWritesDuring(5_000))
    }

    @Test
    fun `hero poster stops its ken burns once the grid is shown`() {
        Coil.setImageLoader(InstantImageLoader())
        val poster = heroItem("h1", "Programme", imageUrl = "https://example.invalid/poster.jpg", sourcePackage = "com.source")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = grid),
                hero = heroStateOf(items = listOf(poster), validated = setOf(poster.imageUrl!!)),
            )
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithTag("hero-poster").assertExists()
        assertTrue(stateWritesDuring(1_000) > 0)
        assertEquals(0L, recompositionsDuring(2_000))

        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        compose.mainClock.advanceTimeBy(2_000)
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.g0").assertIsFocused()
        assertEquals(0, stateWritesDuring(5_000))
    }
}
