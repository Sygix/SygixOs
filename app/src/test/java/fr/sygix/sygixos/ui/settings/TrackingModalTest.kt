/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class TrackingModalTest {
    @get:Rule val compose = createComposeRule()
    private val focus = fr.sygix.sygixos.tracking.TrackingFocusViewModel()

    @Test fun modalKeepsDirectionalBoundariesAndBackClosesWithoutAction() {
        var selected = false
        var closed = false
        compose.setContent {
            MaterialTheme {
                TrackingModal("Fixture", "fixture-modal", listOf(
                    TrackingModalAction("fixture-first", "First") { selected = true },
                    TrackingModalAction("fixture-last", "Last") { selected = true },
                ), { closed = true }, focusModel = focus)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("fixture-first").assertIsFocused()
        press(Key.DirectionUp)
        press(Key.DirectionLeft)
        press(Key.DirectionRight)
        compose.onNodeWithTag("fixture-first").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("fixture-last").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("fixture-last").assertIsFocused()
        press(Key.Back)
        assertTrue(closed)
        assertFalse(selected)
    }

    @Test fun currentEpisodeCanBeInitiallyFocusedBeyondFirstScreen() {
        var chosen = -1
        compose.setContent {
            MaterialTheme {
                TrackingModal("Fixture", "fixture-modal", (0 until 40).map { index ->
                    TrackingModalAction("fixture-$index", "Episode $index") { chosen = index }
                }, {}, initial = 25, focusModel = focus)
            }
        }
        compose.waitForIdle()
        compose.onNodeWithTag("fixture-25").assertIsFocused()
        press(Key.Enter)
        assertEquals(25, chosen)
    }

    private fun press(key: Key) {
        compose.onNodeWithTag("fixture-modal").performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    @Test fun removedFocusedPlayerUsesNextThenPreviousWithoutEscapingModal() {
        val players = androidx.compose.runtime.mutableStateOf(listOf("a", "b", "c"))
        compose.setContent {
            MaterialTheme {
                TrackingModal("Fixture", "fixture-modal", players.value.map { key -> TrackingModalAction("fixture-$key", key) {} }, {}, focusModel = focus)
            }
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        compose.onNodeWithTag("fixture-b").assertIsFocused()
        compose.runOnIdle { players.value = listOf("a", "c") }
        compose.waitForIdle()
        compose.onNodeWithTag("fixture-c").assertIsFocused()
        compose.runOnIdle { players.value = listOf("a") }
        compose.waitForIdle()
        compose.onNodeWithTag("fixture-a").assertIsFocused()
    }
}
