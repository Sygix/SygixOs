/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class SettingsPillTest {

    @get:Rule
    val compose = createComposeRule()

    private val harness = SettingsHarness()

    @After
    fun clearViewModel() = harness.clear()

    private fun press(key: Key) {
        compose.onRoot().performKeyInput {
            keyDown(key)
            keyUp(key)
        }
        compose.waitForIdle()
    }

    private fun label(text: String, container: String): Rect =
        compose.onNode(hasText(text) and hasAnyAncestor(hasTestTag(container)), useUnmergedTree = true)
            .fetchSemanticsNode().boundsInRoot

    private fun open() {
        harness.install("Alpha", "Beta", "Gamma")
        harness.hideInOrder("Gamma", "Beta")
        compose.showSettings(harness)
        compose.onNodeWithTag("settings-category-SOURCES").assertIsFocused()
    }

    @Test
    fun `categories keep their place and size when focused`() {
        open()
        val focused = label("Apps sources", "settings-category-SOURCES")
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-HIDDEN").assertIsFocused()
        assertEquals(focused, label("Apps sources", "settings-category-SOURCES"))
    }

    @Test
    fun `source rows keep their place and size when focused`() {
        open()
        press(Key.DirectionRight)
        compose.onNodeWithTag("source-row-com.alpha").assertIsFocused()
        val focused = label("Alpha", "source-row-com.alpha")
        press(Key.DirectionDown)
        compose.onNodeWithTag("source-row-com.beta").assertIsFocused()
        assertEquals(focused, label("Alpha", "source-row-com.alpha"))
    }

    @Test
    fun `hidden rows and the unhide all button keep their place and size when focused`() {
        open()
        press(Key.DirectionDown)
        press(Key.DirectionRight)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        val row = label("Beta", "hidden-row-com.beta")
        press(Key.DirectionUp)
        compose.onNodeWithTag("unhide-all").assertIsFocused()
        assertEquals(row, label("Beta", "hidden-row-com.beta"))
        val button = label("Tout réactiver", "unhide-all")
        press(Key.DirectionDown)
        compose.onNodeWithTag("hidden-row-com.beta").assertIsFocused()
        assertEquals(button, label("Tout réactiver", "unhide-all"))
    }
}
