/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
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
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.CompletableDeferred
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class GatedImageLoader(private val gated: String) : ImageLoader {
    val gate = CompletableDeferred<Unit>()
    private val counts = mutableMapOf<Any?, Int>()
    fun requests(data: Any?): Int = counts[data] ?: 0
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult {
        counts[request.data] = (counts[request.data] ?: 0) + 1
        if (request.data == gated) gate.await()
        return SuccessResult(ColorDrawable(Color.DKGRAY), request, DataSource.MEMORY)
    }
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroVisualSyncTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun resetImageLoader() = Coil.reset()

    private fun program(id: String, title: String) = HeroItem(
        id = id,
        title = title,
        imageUrl = "https://example.invalid/$id.jpg",
        sourcePackage = "com.example.player",
        sourceLabel = "Appli fictive",
        launchUri = "intent://example/$id",
    )

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        advance(100)
    }

    @Test
    fun `title and button wait for the next visual, and a slow visual gives way to the next program`() {
        val first = program("h1", "Premier titre fictif")
        val slow = program("h2", "Titre fictif lent")
        val third = program("h3", "Troisième titre fictif")
        val loader = GatedImageLoader(slow.imageUrl!!)
        Coil.setImageLoader(loader)
        val opened = mutableListOf<String>()
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = listOf(first, slow, third), validated = setOfNotNull(first.imageUrl, slow.imageUrl, third.imageUrl)),
                onOpenHero = { opened += it.id },
            )
        }
        advance(1_000)
        compose.onNodeWithTag("hero-open").assertIsFocused()
        compose.onNodeWithText(first.title, useUnmergedTree = true).assertExists()

        press(Key.DirectionRight)
        advance(Motion.HERO_VISUAL_TIMEOUT_MS / 2)
        compose.onNodeWithText(first.title, useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(slow.title, useUnmergedTree = true).assertCountEquals(0)
        press(Key.DirectionCenter)
        assertEquals(listOf("h1"), opened)

        advance(Motion.HERO_VISUAL_TIMEOUT_MS + Motion.HERO_CROSSFADE_MS + 500L)
        compose.onNodeWithText(third.title, useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(slow.title, useUnmergedTree = true).assertCountEquals(0)
        compose.onAllNodesWithText(first.title, useUnmergedTree = true).assertCountEquals(0)
        press(Key.DirectionCenter)
        assertEquals(listOf("h1", "h3"), opened)

        loader.gate.complete(Unit)
        advance(500)
        press(Key.DirectionLeft)
        advance(Motion.HERO_CROSSFADE_MS + 500L)
        compose.onNodeWithText(slow.title, useUnmergedTree = true).assertExists()
        press(Key.DirectionCenter)
        assertEquals(listOf("h1", "h3", "h2"), opened)
        assertEquals(2, loader.requests(slow.imageUrl!!))
    }

    @Test
    fun `program without a visual shows its texts at once`() {
        val plain = HeroItem(id = "t1", title = "Titre sans visuel", sourcePackage = "com.example.player", launchUri = "intent://example/t1")
        compose.setContent {
            TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = heroStateOf(items = listOf(plain)))
        }
        compose.waitForIdle()
        compose.onNodeWithText(plain.title, useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("hero-open").assertIsFocused()
    }
}
