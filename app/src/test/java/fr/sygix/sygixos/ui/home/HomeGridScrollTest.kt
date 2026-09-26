/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HomeGridScrollTest {

    @get:Rule
    val compose = createComposeRule()

    private val apps = (0 until 40).map { app("com.a%02d".format(it), "App $it") }
    private val withPosters = listOf("com.a05", "com.a25")

    private fun setContent() {
        compose.mainClock.autoAdvance = false
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = apps),
                hero = heroStateOf(
                    items = withPosters.map { heroItem("h-$it", "Titre", imageUrl = "uri-$it", sourcePackage = it) },
                    validated = withPosters.map { "uri-$it" }.toSet(),
                ),
                initialZone = Zone.GRID,
            )
        }
        settle()
        compose.onNodeWithTag("app-tile-com.a00").assertIsFocused()
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
        settle()
    }

    private fun openShelf() {
        compose.mainClock.advanceTimeBy(Motion.SHELF_OPEN_DELAY_MS + Motion.SHELF_EXPAND_MS + 200)
        compose.waitForIdle()
        compose.onNodeWithTag("shelf-panel").assertExists()
    }

    private fun top(packageName: String) = compose.onNodeWithTag("app-tile-$packageName").getBoundsInRoot().top.value

    private fun bottom(packageName: String) = compose.onNodeWithTag("app-tile-$packageName").getBoundsInRoot().bottom.value

    @Test
    fun `focusing a row below the fold aligns the tile to the bottom margin`() {
        setContent()
        repeat(4) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.a20").assertIsFocused()
        assertEquals(520f, bottom("com.a20"), 1f)
    }

    @Test
    fun `moving within a row leaves the grid where it is`() {
        setContent()
        repeat(5) { press(Key.DirectionDown) }
        val before = top("com.a25")
        press(Key.DirectionRight)
        assertEquals(before, top("com.a25"), 0.5f)
        press(Key.DirectionLeft)
        assertEquals(before, top("com.a25"), 0.5f)
    }

    @Test
    fun `shelf opening on a top row keeps the panel below the top margin`() {
        setContent()
        press(Key.DirectionDown)
        openShelf()
        assertEquals(40f, compose.onNodeWithTag("shelf-panel").getBoundsInRoot().top.value, 1f)
        assertEquals(384f, top("com.a05"), 1f)
    }

    @Test
    fun `shelf opening and closing on a deep row keeps the focused tile in place`() {
        setContent()
        repeat(5) { press(Key.DirectionDown) }
        val before = top("com.a25")
        assertEquals(430f, before, 1f)
        openShelf()
        assertEquals(before, top("com.a25"), 1f)

        press(Key.DirectionRight)
        compose.onNodeWithTag("shelf-panel").assertDoesNotExist()
        assertEquals(before, top("com.a26"), 1f)
    }

    @Test
    fun `re-entering the grid brings the restored tile back within the margins`() {
        setContent()
        repeat(5) { press(Key.DirectionDown) }
        press(Key.Back)
        settle()
        compose.onNodeWithTag("hero-open").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.a25").assertIsFocused()
        assertEquals(520f, bottom("com.a25"), 1f)
    }
}
