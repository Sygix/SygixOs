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
class HideFlowTest {

    @get:Rule
    val compose = createComposeRule()

    private val hidden = mutableListOf<TvApp>()
    private val pinned = mutableListOf<TvApp>()

    private fun openMenu() {
        compose.setContent {
            TestHome(
                catalog = catalogOf(
                    dock = listOf(app("com.other", "Other")),
                    grid = listOf(app("com.hide", "Hideable")),
                ),
                hero = heroStateOf(items = emptyList()),
                onTogglePin = { pinned.add(it) },
                onHide = { hidden.add(it) },
            )
        }
        compose.waitForIdle()
        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        compose.waitForIdle()
        compose.onRoot().performKeyInput {
            keyDown(Key.DirectionDown)
            keyUp(Key.DirectionDown)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("app-tile-com.hide").assertExists()

        // Menu contextuel = appui long sur OK.
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
        compose.onNodeWithTag("app-menu").assertExists()
    }

    @Test
    fun `context menu offers cacher which hides the app without confirmation`() {
        openMenu()

        repeat(2) {
            compose.onRoot().performKeyInput {
                keyDown(Key.DirectionDown)
                keyUp(Key.DirectionDown)
            }
            compose.waitForIdle()
        }
        compose.onNodeWithTag("menu-action-hide").assertIsFocused()
        compose.onRoot().performKeyInput {
            keyDown(Key.Enter)
            keyUp(Key.Enter)
        }
        compose.waitForIdle()

        compose.onNodeWithTag("app-menu").assertDoesNotExist()
        assertEquals(listOf("com.hide"), hidden.map { it.packageName })
        assertEquals(emptyList<String>(), pinned.map { it.packageName })
    }
}
