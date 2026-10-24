/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.data.AppArtwork
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.Dispatchers
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import fr.sygix.sygixos.data.ProgramArtworkInterceptor
import fr.sygix.sygixos.domain.ArtworkPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class CountingArtworkSource(context: Context) : AppArtworkSource(context.packageManager, Dispatchers.Unconfined) {
    val tiles = mutableListOf<String>()
    var recording = false
    override fun cached(app: TvApp): AppArtwork? {
        if (recording) tiles += app.packageName
        return super.cached(app)
    }
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class GridRecompositionTest {

    @get:Rule
    val compose = createComposeRule()

    private val artwork = CountingArtworkSource(ApplicationProvider.getApplicationContext())
    private var validated by mutableStateOf(emptySet<String>())
    private val focusedApps = mutableListOf<String>()

    private val programs = listOf(
        heroItem("p1", "Programme un", imageUrl = "https://example.invalid/p1.jpg", sourcePackage = "com.g7"),
        heroItem("p2", "Programme deux", imageUrl = "https://example.invalid/p2.jpg", sourcePackage = "com.g12"),
    )

    private fun showGrid() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = (0 until 25).map { app("com.g$it", "Grille $it") }),
                hero = heroStateOf(items = programs, validated = validated, checked = validated),
                initialZone = Zone.GRID,
                onAppFocused = { focusedApps += it },
                artworkSource = artwork,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.g0").assertIsFocused()
    }

    private fun recomposedTiles(action: () -> Unit): Set<String> {
        compose.waitForIdle()
        artwork.tiles.clear()
        artwork.recording = true
        action()
        compose.waitForIdle()
        artwork.recording = false
        return artwork.tiles.toSet()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }

    private fun row(index: Int): Set<String> = (index * 5 until index * 5 + 5).map { "com.g$it" }.toSet()

    @Test
    fun `moving the focus recomposes no tile outside the rows that change focus`() {
        showGrid()
        val right = recomposedTiles { press(Key.DirectionRight) }
        compose.onNodeWithTag("app-tile-com.g1").assertIsFocused()
        assertTrue("$right", right.isNotEmpty() && row(0).containsAll(right))
        val down = recomposedTiles { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.g6").assertIsFocused()
        assertTrue("$down", down.isNotEmpty() && (row(0) + row(1)).containsAll(down))
    }

    @Test
    fun `a checked visual of another app recomposes no tile`() {
        showGrid()
        compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS + 100)
        assertEquals(emptySet<String>(), recomposedTiles { validated = setOf("https://example.invalid/p2.jpg") })
    }

    @Test
    fun `a focus passing over tiles prepares only the tile where it rests`() {
        showGrid()
        compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS + 100)
        compose.waitForIdle()
        focusedApps.clear()
        compose.mainClock.autoAdvance = false
        repeat(3) {
            press(Key.DirectionRight)
            compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS / 3)
        }
        assertEquals(emptyList<String>(), focusedApps)
        compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS + 100)
        compose.waitForIdle()
        assertEquals(listOf("com.g3"), focusedApps)
    }

    @Test
    fun `a focus passing over tiles decodes no shelf poster and a settled focus decodes one`() {
        val decodes = mutableListOf<Any>()
        Coil.setImageLoader(object : ImageLoader {
            override val defaults = DefaultRequestOptions()
            override val components = ComponentRegistry()
            override val memoryCache: MemoryCache? = null
            override val diskCache: DiskCache? = null
            override fun enqueue(request: ImageRequest): Disposable = error("unused")
            override suspend fun execute(request: ImageRequest): ImageResult {
                if (request.parameters.value<ArtworkPolicy>(ProgramArtworkInterceptor.POLICY) is ArtworkPolicy.Shelf) decodes += request.data
                return ErrorResult(null, request, IllegalStateException("offline"))
            }
            override fun shutdown() = Unit
            override fun newBuilder(): ImageLoader.Builder = error("unused")
        })
        val posters = (1..3).map { heroItem("s$it", "Programme $it", imageUrl = "https://example.invalid/s$it.jpg", sourcePackage = "com.g$it") }
        val uris = posters.mapNotNull { it.imageUrl }.toSet()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = (0 until 10).map { app("com.g$it", "Grille $it") }),
                hero = heroStateOf(items = posters, validated = uris, checked = uris),
                initialZone = Zone.GRID,
                artworkSource = artwork,
            )
        }
        compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS / 3)
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.g0").assertIsFocused()
        repeat(3) {
            press(Key.DirectionRight)
            compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS / 3)
        }
        compose.onNodeWithTag("app-tile-com.g3").assertIsFocused()
        assertEquals(emptyList<Any>(), decodes)
        compose.mainClock.advanceTimeBy(Motion.SHELF_PREPARE_DELAY_MS)
        compose.waitForIdle()
        assertEquals(1, decodes.size)
        assertTrue(decodes.single().toString().contains("s3"))
    }

    @After
    fun resetImageLoader() = Coil.reset()
}
