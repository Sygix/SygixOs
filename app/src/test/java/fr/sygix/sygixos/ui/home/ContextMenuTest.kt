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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ContextMenuTest {

    @get:Rule
    val compose = createComposeRule()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun openMenuOnGridTile() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(dock = emptyList(), grid = listOf(app("com.a", "Alpha"), app("com.b", "Beta"))),
                hero = heroStateOf(items = emptyList()),
                initialZone = Zone.GRID,
            )
        }
        compose.waitForIdle()
        press(Key.DirectionRight)
        compose.onNodeWithTag("app-tile-com.b").assertIsFocused()
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertExists()
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.top

    private fun left(tag: String) = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot.left

    @Test
    fun `actions are listed vertically and down walks them in order without wrapping`() {
        openMenuOnGridTile()
        compose.onNodeWithTag("menu-action-pin").assertIsFocused()
        val order = listOf("menu-action-pin", "menu-action-move", "menu-action-hide", "menu-action-close")
        order.zipWithNext().forEach { (above, below) ->
            assertTrue(top(below) > top(above))
            assertEquals(left(above), left(below), 0.5f)
        }
        order.drop(1).forEach { tag ->
            press(Key.DirectionDown)
            compose.onNodeWithTag(tag).assertIsFocused()
        }
        press(Key.DirectionDown)
        compose.onNodeWithTag("menu-action-close").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("menu-action-close").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("menu-action-close").assertIsFocused()
        repeat(4) { press(Key.DirectionUp) }
        compose.onNodeWithTag("menu-action-pin").assertIsFocused()
    }

    @Test
    fun `back closes the menu and gives the focus back to the tile`() {
        openMenuOnGridTile()
        press(Key.DirectionDown)
        press(Key.Back)
        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        compose.onNodeWithTag("app-tile-com.b").assertIsFocused()
    }
}
