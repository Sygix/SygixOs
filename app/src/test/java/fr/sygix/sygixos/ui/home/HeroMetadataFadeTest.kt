/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.input.key.Key
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
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class ReadyImageLoader : ImageLoader {
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
class HeroMetadataFadeTest {

    @get:Rule
    val compose = createComposeRule()

    @After
    fun resetImageLoader() = Coil.reset()

    private fun program(id: String, title: String) =
        HeroItem(id = id, title = title, imageUrl = "https://example.invalid/$id.jpg", sourcePackage = "com.source", sourceLabel = "Source")

    @Test
    fun `changing program fades the texts in turn without moving them`() {
        val short = program("h1", "Court")
        val long = program("h2", "Un titre de programme beaucoup plus long qui tient sur deux lignes au moins")
        Coil.setImageLoader(ReadyImageLoader())
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = listOf(short, long), validated = setOfNotNull(short.imageUrl, long.imageUrl)),
            )
        }
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onNodeWithTag("hero-open").assertIsFocused()
        val button = compose.onNodeWithTag("hero-open").fetchSemanticsNode().boundsInRoot
        val anchor = compose.onNodeWithTag("hero-metadata").fetchSemanticsNode().boundsInRoot.bottom

        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        var sawBoth = false
        repeat((Motion.HERO_CROSSFADE_MS + 400) / 50) {
            compose.mainClock.advanceTimeBy(50)
            compose.waitForIdle()
            val nodes = compose.onAllNodesWithTag("hero-metadata").fetchSemanticsNodes()
            if (nodes.size > 1) sawBoth = true
            nodes.forEach { assertEquals(anchor, it.boundsInRoot.bottom, 0.5f) }
            assertEquals(button, compose.onNodeWithTag("hero-open").fetchSemanticsNode().boundsInRoot)
        }
        assertTrue(sawBoth)
        assertEquals(1, compose.onAllNodesWithTag("hero-metadata").fetchSemanticsNodes().size)
        compose.onNodeWithText(long.title, useUnmergedTree = true).assertExists()
        compose.onAllNodesWithText(short.title, useUnmergedTree = true).assertCountEquals(0)
    }
}
