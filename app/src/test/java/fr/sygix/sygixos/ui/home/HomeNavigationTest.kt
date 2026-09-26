/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import kotlin.math.abs
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HomeNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    @Test
    fun `down moves hero to dock then grid and up comes back`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.dock", "Dock")),
                    grid = listOf(app("com.grid", "Grid")),
                ),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()

        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()

        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()

        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()

        press(Key.DirectionUp)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `with empty dock down goes hero to grid and up comes back`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()

        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()

        press(Key.DirectionUp)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `back from grid returns focus to hero`() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.dock", "Dock")),
                    grid = listOf(app("com.grid", "Grid")),
                ),
                hero = heroStateOf(items = emptyList()),
            )
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()

        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    private val dockAndGrid = catalogOf(
        dock = listOf(app("com.dock", "Dock")),
        grid = listOf(app("com.grid", "Grid")),
    )

    private fun send(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
    }

    private fun step(ms: Long = 16) {
        compose.mainClock.advanceTimeBy(ms)
        compose.waitForIdle()
    }

    private fun assertDockNeverOnGrid(frames: Int) {
        val screen = compose.screenHeight()
        repeat(frames) {
            step()
            val dock = compose.span("zone-dock")
            val grid = compose.span("zone-grid")
            val gridVisible = grid.top < screen && grid.bottom > 0f
            assertTrue(!gridVisible || dock.bottom <= grid.top || dock.bottom <= 0f)
        }
    }

    @Test
    fun `down from the dock scrolls the grid onto the screen and the hero and dock off it`() {
        compose.setContent { TestHome(catalog = dockAndGrid, hero = heroStateOf(items = emptyList())) }
        compose.waitForIdle()
        val screen = compose.screenHeight()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        val grid = compose.span("zone-grid")
        assertEquals(0f, grid.top, 1f)
        assertTrue(grid.bottom <= screen + 1f)
        assertTrue(compose.span("zone-hero").bottom <= 0f)
        assertTrue(compose.span("zone-dock").bottom <= 0f)
    }

    @Test
    fun `the dock never covers the grid while the page scrolls down and up`() {
        compose.mainClock.autoAdvance = false
        compose.setContent { TestHome(catalog = dockAndGrid, hero = heroStateOf(items = emptyList())) }
        step(1_000)
        send(Key.DirectionDown)
        step(1_000)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
        send(Key.DirectionDown)
        assertDockNeverOnGrid(frames = Motion.PAGE_SCROLL_MS / 16 + 4)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        send(Key.DirectionUp)
        assertDockNeverOnGrid(frames = Motion.PAGE_SCROLL_MS / 16 + 4)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
    }

    @Test
    fun `up from the first row scrolls the page back to the dock`() {
        compose.setContent { TestHome(catalog = dockAndGrid, hero = heroStateOf(items = emptyList())) }
        compose.waitForIdle()
        val screen = compose.screenHeight()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
        assertEquals(0f, compose.span("zone-hero").top, 1f)
        assertTrue(compose.span("zone-grid").top >= screen - 1f)
    }

    @Test
    fun `back from a deep row scrolls up to the hero in a single move`() {
        val apps = (0 until 40).map { app("com.a%02d".format(it), "App $it") }
        compose.mainClock.autoAdvance = false
        compose.setContent { TestHome(catalog = catalogOf(dock = emptyList(), grid = apps), hero = heroStateOf(items = emptyList())) }
        step(1_000)
        send(Key.DirectionDown)
        step(1_000)
        repeat(6) {
            send(Key.DirectionDown)
            step(1_000)
        }
        compose.onNodeWithTag("app-tile-com.a30").assertIsFocused()
        val start = compose.span("zone-hero").top
        assertTrue(start < -compose.screenHeight())
        send(Key.Back)
        val tops = mutableListOf(start)
        repeat(Motion.PAGE_SCROLL_MS / 16 + 4) {
            step()
            tops += compose.span("zone-hero").top
        }
        assertTrue(tops.zipWithNext().all { (a, b) -> b >= a })
        assertEquals(0f, tops.last(), 1f)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `up during the descent turns the page back to the dock from where it is`() {
        compose.mainClock.autoAdvance = false
        compose.setContent { TestHome(catalog = dockAndGrid, hero = heroStateOf(items = emptyList())) }
        step(1_000)
        send(Key.DirectionDown)
        step(1_000)
        send(Key.DirectionDown)
        repeat(8) { step() }
        val midway = compose.span("zone-hero").top
        assertTrue(midway < 0f && midway > -compose.screenHeight())
        send(Key.DirectionUp)
        step()
        val next = compose.span("zone-hero").top
        assertTrue(abs(next - midway) < compose.screenHeight() / 4)
        step(1_000)
        assertEquals(0f, compose.span("zone-hero").top, 1f)
        compose.onNodeWithTag("app-tile-com.dock").assertIsFocused()
    }
}
