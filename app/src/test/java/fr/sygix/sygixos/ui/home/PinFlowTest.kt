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
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PinFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private fun focusGridTile() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.other", "Other")),
                    grid = listOf(app("com.pin", "Pinned")),
                ),
                hero = heroStateOf(items = emptyList()),
                onTogglePin = { pinned.add(it) },
            )
        }
        compose.waitForIdle()
        compose.onRoot().performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onRoot().performKeyInput { keyDown(Key.DirectionDown); keyUp(Key.DirectionDown) }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.pin").assertIsFocused()
    }

    private val pinned = mutableListOf<TvApp>()

    private fun openMenu() {
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertExists()
    }

    @Test
    fun `long press opens context menu and confirm toggles pin`() {
        focusGridTile()
        openMenu()

        compose.onRoot().performKeyInput { keyDown(Key.Enter); keyUp(Key.Enter) }
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        assertEquals(listOf("com.pin"), pinned.map { it.packageName })
    }

    @Test
    fun `back closes context menu without toggling pin`() {
        focusGridTile()
        openMenu()

        compose.onRoot().performKeyInput { keyDown(Key.Back); keyUp(Key.Back) }
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        assertEquals(emptyList<String>(), pinned.map { it.packageName })
    }
}
