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
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.settings.SettingsState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
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
    fun `gear disappears in the grid zone and comes back on the hero`() {
        setupHome()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        compose.onNodeWithTag("settings-gear").assertDoesNotExist()
        press(Key.Back)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        compose.onNodeWithTag("settings-gear").assertExists()
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
    fun `ok on gear opens settings and back resumes hero`() {
        setupHome()
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `back from hidden apps sub screen returns to settings pane, not home`() {
        setupHome(SettingsState(hiddenApps = listOf(app("com.hidden", "Hidden"))))
        press(Key.DirectionUp)
        press(Key.Enter)
        compose.onNodeWithTag("settings-screen").assertExists()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        press(Key.Enter)
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        compose.onNodeWithTag("settings-screen").assertExists()
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
        // Après le retour des réglages : le héro reprend le focus et les réglages restent ouvrables.
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
