/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.input.key.Key
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onNodeWithText
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.ui.hero.FakeVideo
import fr.sygix.sygixos.ui.hero.HeroVideoFactory
import fr.sygix.sygixos.ui.hero.LocalHeroVideoFactory
import org.junit.Assert.assertFalse
import fr.sygix.sygixos.core.designsystem.GlassActive
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.settings.HiddenRow
import fr.sygix.sygixos.ui.settings.SettingsState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsEntryTest {

    @get:Rule
    val compose = createComposeRule()

    private val hidden = mutableListOf<TvApp>()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun setupHome(settings: SettingsState? = null) {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.dock", "Dock")),
                    grid = listOf(app("com.grid", "Grid")),
                ),
                hero = heroStateOf(items = emptyList()),
                onHide = { hidden.add(it) },
                settings = settings,
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `gear is visible on hero without stealing focus`() {
        setupHome()
        compose.onNodeWithTag("settings-gear").assertExists()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `gear leaves with the hero in the grid zone and comes back with it`() {
        setupHome()
        val gear = compose.span("settings-gear")
        val capsule = compose.span("hero-capsule")
        val hero = compose.span("zone-hero")
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        val gearDown = compose.span("settings-gear")
        val capsuleDown = compose.span("hero-capsule")
        val heroDown = compose.span("zone-hero")
        assertTrue(gearDown.bottom <= 0f)
        assertTrue(capsuleDown.bottom <= 0f)
        assertTrue(heroDown.top < hero.top)
        assertEquals(heroDown.top - hero.top, gearDown.top - gear.top, 1f)
        assertEquals(heroDown.top - hero.top, capsuleDown.top - capsule.top, 1f)

        press(Key.DirectionUp)
        press(Key.DirectionUp)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        assertEquals(gear, compose.span("settings-gear"))
        assertEquals(capsule, compose.span("hero-capsule"))
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
    }

    @Test
    fun `back from the grid brings the gear back to its place without focus`() {
        setupHome()
        val gear = compose.span("settings-gear")
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        assertEquals(gear, compose.span("settings-gear"))
    }

    @Test
    fun `up focuses the gear, down returns to hero`() {
        setupHome()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `settings replace the home, which is neither displayed nor focusable, and back shows the focused hero`() {
        setupHome()
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertIsDisplayed()
        listOf("zone-hero", "zone-dock", "home-grid").forEach { compose.onNodeWithTag(it).assertExists().assertIsNotDisplayed() }
        listOf(Key.DirectionLeft, Key.DirectionRight, Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft).forEach { key ->
            press(key)
            compose.onAllNodes(isFocused() and !hasAnyAncestor(hasTestTag("settings-screen"))).assertCountEquals(0)
        }
        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
        compose.onNodeWithTag("zone-hero").assertIsDisplayed().assertIsFocused()
    }

    @Test
    fun `the up next position list blurs the settings page instead of the hidden home`() {
        compose.setContent {
            TestHome(catalogOf(listOf(app("com.dock", "Dock")), listOf(app("com.grid", "Grid"))), heroStateOf(emptyList()), glassBlur = true)
        }
        compose.waitForIdle()
        press(Key.DirectionUp)
        press(Key.Enter)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        press(Key.Enter)
        val list = "setting-upnext-position-list"
        assertTrue(compose.onNodeWithTag(list).fetchSemanticsNode().config[GlassActive])
        val blurred = compose.blurredHaze(list)
        assertTrue(compose.isHazeSource("settings-screen", blurred))
        assertFalse(compose.isHazeSource("zone-hero", blurred))
    }

    @Test
    fun `the hero video pauses under the settings and resumes on back without loading again`() {
        val video = FakeVideo()
        val clip = HeroItem(id = "v1", title = "Vidéo fictive", videoUrl = "https://example.invalid/v1.mp4", sourcePackage = "com.example.player")
        compose.setContent {
            CompositionLocalProvider(LocalHeroVideoFactory provides HeroVideoFactory { _, _, _ -> video }) {
                TestHome(catalogOf(listOf(app("com.dock", "Dock")), emptyList()), heroStateOf(listOf(clip)))
            }
        }
        compose.runOnIdle { video.render("v1") }
        compose.onNodeWithText(clip.title, useUnmergedTree = true).assertIsDisplayed()
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        assertEquals("v1", video.loadedItem)
        assertFalse(video.playing)
        press(Key.Back)
        compose.onNodeWithText(clip.title, useUnmergedTree = true).assertIsDisplayed()
        assertTrue(video.playing)
        assertTrue(video.firstFrameRendered)
        assertEquals(listOf("v1"), video.prepared)
    }

    @Test
    fun `back from the hidden apps pane closes the settings and resumes the hero`() {
        setupHome(SettingsState(hiddenRows = listOf(HiddenRow(app("com.hidden", "Hidden"), hidden = true, hiddenAt = null))))
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.hidden").assertIsFocused()
        compose.onNode(hasTestTag("hidden-row-com.hidden") and hasAnyAncestor(hasTestTag("hidden-pane"))).assertIsDisplayed()
        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `settings reopen and nav work after returning from settings`() {
        setupHome()
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("settings-gear").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
    }

    @Test
    fun `three paliers nav still works after settings changes`() {
        setupHome()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }
}
