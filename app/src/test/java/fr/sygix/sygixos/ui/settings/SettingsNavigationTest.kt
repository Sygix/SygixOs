/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsNavigationTest {

    @get:Rule
    val compose = createComposeRule()

    private val toggled = mutableListOf<String>()
    private val unhid = mutableListOf<String>()
    private var unhideAll = 0

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun setState(
        sources: List<SourceRow> = emptyList(),
        hidden: List<TvApp> = emptyList(),
    ) {
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsState(sources = sources, hiddenApps = hidden, version = "0.1.0"),
                    onToggleSource = { toggled.add(it) },
                    onUnhide = { unhid.add(it) },
                    onUnhideAll = { unhideAll++ },
                )
            }
        }
        compose.waitForIdle()
    }

    private fun sourceRows() = listOf(
        SourceRow(app = TvApp("com.a", "A"), enabled = true),
        SourceRow(app = TvApp("com.b", "B"), enabled = false),
    )

    @Test
    fun `first category has focus at opening and right pane shows its content`() {
        setState()
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
        compose.onNodeWithTag("settings-sources").assertExists()
    }

    @Test
    fun `up and down in left pane switch the active category`() {
        setState()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertExists()
        compose.onNodeWithTag("open-hidden").assertExists()
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-about").assertExists()
        press(Key.DirectionUp)
        compose.onNodeWithTag("open-hidden").assertExists()
    }

    @Test
    fun `right moves focus to content and left returns it to categories`() {
        setState(sources = sourceRows())
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.a").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
    }

    @Test
    fun `ok on a source row toggles it`() {
        setState(sources = sourceRows())
        press(Key.DirectionRight)
        press(Key.Enter)
        assertEquals(listOf("com.a"), toggled)
    }

    @Test
    fun `hidden category opens sub screen and back returns to settings pane`() {
        setState(hidden = listOf(TvApp("com.h", "H")))
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        compose.onNodeWithTag("settings-screen").assertExists()
    }

    @Test
    fun `emptying the hidden list while sub-screen is open closes it and back reaches settings`() {
        val hidden = mutableStateOf(listOf(TvApp("com.h", "H")))
        var backs = 0
        compose.setContent {
            MaterialTheme {
                SettingsScreen(
                    state = SettingsState(sources = emptyList(), hiddenApps = hidden.value, version = "0.1.0"),
                    onToggleSource = { toggled.add(it) },
                    onUnhide = { pkg -> hidden.value = hidden.value.filterNot { it.packageName == pkg } },
                    onUnhideAll = { hidden.value = emptyList() },
                    onBack = { backs++ },
                )
            }
        }
        compose.waitForIdle()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-apps-screen").assertExists()
        press(Key.Enter)
        compose.onNodeWithTag("hidden-apps-screen").assertDoesNotExist()
        compose.onNodeWithTag("hidden-empty").assertDoesNotExist()
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        press(Key.Back)
        assertEquals(1, backs)
        compose.onNodeWithTag("settings-screen").assertExists()
    }

    @Test
    fun `about category shows version and licenses`() {
        setState()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag("about-version").assertExists()
        compose.onNodeWithTag("about-licenses").assertExists()
    }
}
