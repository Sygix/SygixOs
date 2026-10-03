/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

private class ReadyPosterLoader : ImageLoader {
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
class HeroMotionDeferTest {

    @get:Rule
    val compose = createComposeRule()

    private val interactive = mutableStateOf(false)

    @After
    fun resetImageLoader() = Coil.reset()

    private fun show(hero: HeroState) {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(catalog = catalogOf(dock = emptyList(), grid = emptyList()), hero = hero, interactive = interactive.value)
        }
        advance(2_000)
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun stateWritesDuring(millis: Long): Int {
        var writes = 0
        val handle = Snapshot.registerApplyObserver { changed, _ -> writes += changed.size }
        try {
            advance(millis)
        } finally {
            handle.dispose()
        }
        return writes
    }

    private fun startFade() {
        interactive.value = true
        Snapshot.sendApplyNotifications()
        advance(100)
    }

    @Test
    fun `fallback gradient waits for the fade then makes its whole pass`() {
        show(heroStateOf(items = emptyList()))
        assertEquals(0, stateWritesDuring(5_000))
        startFade()
        compose.onNodeWithTag("zone-hero").assertExists()
        advance(Motion.AMBIENT_PASS_MS - 4_000L)
        assertTrue(stateWritesDuring(2_000) > 0)
        advance(3_000)
        assertEquals(0, stateWritesDuring(3_000))
    }

    @Test
    fun `hero Ken Burns waits for the fade then runs from the start`() {
        Coil.setImageLoader(ReadyPosterLoader())
        val url = "https://x/p.jpg"
        show(heroStateOf(items = listOf(heroItem("h1", "Programme", imageUrl = url)), validated = setOf(url)))
        compose.onNodeWithTag("hero-poster").assertExists()
        assertEquals(0, stateWritesDuring(5_000))
        startFade()
        advance(Motion.HERO_KEN_BURNS_MS - 3_000L)
        assertTrue(stateWritesDuring(2_000) > 0)
        advance(2_000)
        assertEquals(0, stateWritesDuring(2_000))
    }
}
