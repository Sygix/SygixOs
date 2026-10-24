/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.domain.AppLanguage
import fr.sygix.sygixos.domain.LanguageState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class LanguageSettingsTest {
    @get:Rule val compose = createComposeRule()
    private val harness = SettingsHarness()

    @After
    fun clear() = harness.clear()

    private fun press(key: Key, tag: String = "settings-screen") {
        compose.onNodeWithTag(tag).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun openLanguage() {
        harness.install("Alpha")
        compose.showSettings(harness)
        repeat(4) { press(Key.DirectionDown) }
        compose.onNodeWithTag("settings-category-LANGUAGE").assertIsFocused()
        press(Key.DirectionRight)
    }

    @Test
    fun `language pane offers the TV language then each language in its own name`() {
        openLanguage()
        compose.onNodeWithTag("setting-language-system").assertIsFocused().assertIsSelected()
        compose.onNodeWithText("Langue de la TV", useUnmergedTree = true).assertExists()
        compose.onNode(hasText("Français") and hasAnyAncestor(hasTestTag("setting-language-system")), useUnmergedTree = true).assertExists()
        listOf("de" to "Deutsch", "en" to "English", "es" to "Español", "fr" to "Français", "it" to "Italiano").forEach { (tag, name) ->
            compose.onNodeWithTag("setting-language-$tag").assertIsNotSelected()
            compose.onNodeWithTag("setting-language-$tag-check", useUnmergedTree = true).assertDoesNotExist()
            compose.onNode(hasText(name) and hasAnyAncestor(hasTestTag("setting-language-$tag")), useUnmergedTree = true).assertExists()
        }
    }

    @Test
    fun `choosing a language applies it once and moves the check`() {
        openLanguage()
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-language-de").assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf<AppLanguage?>(AppLanguage.GERMAN), harness.languages.selections)
        compose.onNodeWithTag("setting-language-de").assertIsSelected()
        compose.onNodeWithTag("setting-language-de-check", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("setting-language-system").assertIsNotSelected()
        press(Key.Enter)
        assertEquals(listOf<AppLanguage?>(AppLanguage.GERMAN), harness.languages.selections)
    }

    @Test
    fun `focus starts on the chosen language and the TV language clears the choice`() {
        harness.languages.state = LanguageState(selected = AppLanguage.ITALIAN, system = AppLanguage.FRENCH)
        openLanguage()
        compose.onNodeWithTag("setting-language-it").assertIsFocused().assertIsSelected()
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-language-it").assertIsFocused()
        repeat(5) { press(Key.DirectionUp) }
        compose.onNodeWithTag("setting-language-system").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-language-system").assertIsFocused()
        press(Key.Enter)
        assertEquals(listOf<AppLanguage?>(null), harness.languages.selections)
        compose.onNodeWithTag("setting-language-system").assertIsSelected()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-LANGUAGE").assertIsFocused()
    }

    @Test
    fun `an untranslated TV language says English is used`() {
        harness.languages.state = LanguageState(system = null)
        openLanguage()
        compose.onNodeWithText("Langue de la TV non traduite · English", useUnmergedTree = true).assertExists()
    }

    @Test
    fun `entering the category reads the TV language again`() {
        openLanguage()
        harness.languages.state = LanguageState(system = AppLanguage.ITALIAN)
        press(Key.DirectionLeft)
        press(Key.DirectionUp)
        press(Key.DirectionDown)
        compose.onNodeWithTag("settings-category-LANGUAGE").assertIsFocused()
        compose.onNode(hasText("Italiano") and hasAnyAncestor(hasTestTag("setting-language-system")), useUnmergedTree = true).assertExists()
    }
}
