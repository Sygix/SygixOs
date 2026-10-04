/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import androidx.compose.runtime.snapshots.Snapshot
import coil.Coil
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class HomeGridTest {

    @get:Rule
    val compose = createComposeRule()

    @Before
    fun offlineImages() = Coil.setImageLoader(UnreachableImageLoader())

    @After
    fun resetImages() = Coil.reset()

    @Test
    fun `grid renders each installed app exactly once`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.dock", "Dock")),
                    grid = listOf(app("com.a", "Alpha"), app("com.b", "Beta")),
                ),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        listOf("com.a", "com.b").forEach { pkg ->
            assertEquals(1, compose.onAllNodesWithTag("app-tile-$pkg").fetchSemanticsNodes().size)
        }
    }

    @Test
    fun `empty grid shows no apps detected message`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = emptyList()),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.onNodeWithText("Aucune app TV détectée").assertExists()
    }

    @Test
    fun `focused tile with validated posters opens shelf panel`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "Alpha"))),
                hero = heroStateOf(
                    items = listOf(heroItem("h1", "Héros", imageUrl = "uri1", sourcePackage = "com.a")),
                    validated = setOf("uri1"),
                ),
                initialZone = Zone.GRID,
            )
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertExists()
    }

    @Test
    fun `tile without validated posters keeps shelf closed`() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "Alpha"))),
                hero = heroStateOf(
                    items = listOf(heroItem("h1", "Héros", imageUrl = "uri1", sourcePackage = "com.a")),
                    validated = emptySet(),
                ),
                initialZone = Zone.GRID,
            )
        }
        compose.mainClock.advanceTimeBy(500)
        compose.waitForIdle()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-xhdpi")
    fun `a deep row with its shelf open stays on screen and the hero never comes back while in the grid`() {
        val apps = (0 until 40).map { app("com.a%02d".format(it), "App $it") }
        val withPosters = listOf("com.a05", "com.a25")
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = apps),
                hero = heroStateOf(
                    items = withPosters.map { heroItem("h-$it", "Titre", imageUrl = "uri-$it", sourcePackage = it) },
                    validated = withPosters.map { "uri-$it" }.toSet(),
                ),
            )
        }
        settle()
        val screen = compose.screenHeight()
        press(Key.DirectionDown)
        repeat(5) {
            press(Key.DirectionDown)
            assertTrue(compose.span("zone-hero").bottom <= 0.5f)
        }
        compose.onNodeWithTag("app-tile-com.a25").assertIsFocused()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 200)
        compose.waitForIdle()
        val panel = compose.span("shelf-panel")
        val tile = compose.span("app-tile-com.a25")
        assertTrue(panel.top >= 0f && tile.bottom <= screen)
        assertTrue(panel.bottom <= tile.top)
        assertTrue(compose.span("zone-hero").bottom <= 0.5f)

        repeat(4) {
            press(Key.DirectionUp)
            assertTrue(compose.span("zone-hero").bottom <= 0.5f)
        }
        compose.onNodeWithTag("app-tile-com.a05").assertIsFocused()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 200)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertExists()
        assertTrue(compose.span("zone-hero").bottom <= 0.5f)
        assertTrue(compose.span("shelf-panel").top >= 0f)
    }

    private fun settle() {
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.runOnIdle { Snapshot.sendApplyNotifications() }
        settle()
    }

    @Test
    @Config(qualifiers = "w960dp-h540dp-xhdpi")
    fun `closing the shelf on the first row brings the grid back to its start without showing the hero`() {
        val apps = (0 until 8).map { app("com.a%02d".format(it), "App $it") }
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = apps),
                hero = heroStateOf(
                    items = listOf(heroItem("h-a00", "Titre", imageUrl = "uri-a00", sourcePackage = "com.a00")),
                    validated = setOf("uri-a00"),
                ),
                initialZone = Zone.GRID,
            )
        }
        settle()
        compose.onNodeWithTag("app-tile-com.a00").assertIsFocused()
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 200)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertExists()
        val gridTop = compose.span("zone-grid").top
        assertTrue(compose.span("zone-hero").bottom <= 0.5f)

        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionRight)
            keyUp(Key.DirectionRight)
        }
        repeat(Motion.SHELF_EXPAND_MS / 16 + 8) {
            compose.mainClock.advanceTimeBy(16)
            compose.waitForIdle()
            assertTrue(compose.span("zone-hero").bottom <= 0.5f)
        }
        compose.onNodeWithTag("app-tile-com.a01").assertIsFocused()
        compose.onNodeWithTag("shelf-panel").assertDoesNotExist()
        assertEquals(gridTop, compose.span("zone-grid").top, 1f)
    }
}
