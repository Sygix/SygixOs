/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import coil.ComponentRegistry
import coil.ImageLoader
import coil.decode.DataSource
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.DefaultRequestOptions
import coil.request.Disposable
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import fr.sygix.sygixos.core.designsystem.Motion
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class PosterImageLoader(private val succeed: Boolean) : ImageLoader {
    override val defaults = DefaultRequestOptions()
    override val components = ComponentRegistry()
    override val memoryCache: MemoryCache? = null
    override val diskCache: DiskCache? = null
    override fun enqueue(request: ImageRequest): Disposable = error("unused")
    override suspend fun execute(request: ImageRequest): ImageResult =
        if (succeed) {
            SuccessResult(ColorDrawable(Color.WHITE), request, DataSource.MEMORY)
        } else {
            ErrorResult(null, request, IllegalStateException("offline"))
        }
    override fun shutdown() = Unit
    override fun newBuilder(): ImageLoader.Builder = error("unused")
}

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HeroVisualReadyTest {

    @get:Rule
    val compose = createComposeRule()

    private var signals = 0

    @After
    fun resetImageLoader() = Coil.reset()

    private fun show(hero: HeroState) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = hero,
                onHeroVisualReady = { signals++ },
            )
        }
        compose.mainClock.advanceTimeBy(Motion.SPLASH_CAP_MS)
        compose.waitForIdle()
    }

    @Test
    fun `loaded program poster reports the first hero visual`() {
        Coil.setImageLoader(PosterImageLoader(succeed = true))
        show(heroStateOf(items = listOf(heroItem("h1", "Programme", imageUrl = "https://x/p.jpg")), validated = setOf("https://x/p.jpg")))
        assertTrue(signals > 0)
    }

    @Test
    fun `fallback gradient never reports a hero visual`() {
        show(heroStateOf(items = emptyList()))
        assertEquals(0, signals)
    }

    @Test
    fun `program without a validated visual never reports a hero visual`() {
        Coil.setImageLoader(PosterImageLoader(succeed = true))
        show(heroStateOf(items = listOf(heroItem("h1", "Programme", imageUrl = "https://x/p.jpg"))))
        assertEquals(0, signals)
    }

    @Test
    fun `unreadable poster never reports a hero visual`() {
        Coil.setImageLoader(PosterImageLoader(succeed = false))
        show(heroStateOf(items = listOf(heroItem("h1", "Programme", imageUrl = "https://x/p.jpg")), validated = setOf("https://x/p.jpg")))
        assertEquals(0, signals)
    }
}
