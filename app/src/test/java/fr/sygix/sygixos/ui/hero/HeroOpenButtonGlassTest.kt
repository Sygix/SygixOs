/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil.Coil
import fr.sygix.sygixos.core.designsystem.GlassActive
import fr.sygix.sygixos.core.designsystem.GlassBackdrop
import fr.sygix.sygixos.model.HeroItem
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class HeroOpenButtonGlassTest {

    @get:Rule
    val compose = createComposeRule()

    private val backdrop = GlassBackdrop()
    private val video = FakeVideo()
    private val poster = HeroItem(id = "h1", title = "Affiche fictive", imageUrl = "https://example.invalid/h1.jpg", sourcePackage = "com.source")
    private val clip = HeroItem(id = "v1", title = "Vidéo fictive", videoUrl = "https://example.invalid/v1.mp4", sourcePackage = "com.source")

    @After
    fun resetImageLoader() = Coil.reset()

    private fun show(first: HeroItem, glass: Boolean = true): (HeroItem) -> Unit {
        Coil.setImageLoader(TransformingImageLoader())
        var item by mutableStateOf(first)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            CompositionLocalProvider(LocalHeroVideoFactory provides HeroVideoFactory { _, _, _ -> video }) {
                HeroStage(
                    items = listOf(item),
                    validatedVisuals = setOfNotNull(item.imageUrl),
                    active = true,
                    visible = true,
                    focusRequester = FocusRequester(),
                    onOpen = {},
                    backdrop = backdrop,
                    glass = glass,
                )
            }
        }
        return { item = it }
    }

    private fun advance(millis: Long) {
        compose.mainClock.advanceTimeBy(millis)
        compose.waitForIdle()
    }

    private fun frosted(): Boolean = compose.onNodeWithTag("hero-open").fetchSemanticsNode().config[GlassActive]

    @Test
    fun `the open button at rest on a programme image is frosted with its blurred copy`() {
        show(poster)
        advance(2_000)
        assertTrue(backdrop.ready)
        assertTrue(frosted())
    }

    @Test
    fun `on a video the open button keeps its tint without any blur`() {
        val select = show(poster)
        advance(2_000)
        assertTrue(frosted())
        select(clip)
        advance(100)
        compose.runOnIdle { video.render(clip.id) }
        advance(2_000)
        compose.onNodeWithTag("hero-open").assertExists()
        assertFalse(backdrop.ready)
        assertFalse(frosted())
    }

    @Test
    fun `without glass the open button stays flat on a programme image`() {
        show(poster, glass = false)
        advance(2_000)
        assertTrue(backdrop.ready)
        assertFalse(frosted())
    }
}
