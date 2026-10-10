/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performKeyInput
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.model.UpNextPosition
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class HomeScreenPaneTest {
    @get:Rule val compose = createComposeRule()
    private val harness = SettingsHarness()

    @Before
    fun resetPreferences() = runBlocking {
        harness.prefs.setUpNextVisible(true)
        harness.prefs.setUpNextPosition(UpNextPosition.BEFORE_APPS)
    }

    @After
    fun clear() = harness.clear()

    private fun press(key: Key, tag: String = "settings-screen") {
        compose.onNodeWithTag(tag).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun openPositionList() {
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("settings-category-HOME_SCREEN").assertIsFocused()
        press(Key.DirectionRight)
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("setting-upnext-position-list").assertExists()
    }

    @Test
    fun `position list left open is closed when the settings are reopened and the value is unchanged`() {
        harness.install("Alpha")
        compose.showSettings(harness)
        openPositionList()
        press(Key.DirectionDown, "setting-upnext-position-list")
        compose.runOnIdle { harness.closeSettings() }
        compose.waitForIdle()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.runOnIdle { harness.openSettings() }
        compose.waitForIdle()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.onNodeWithTag("setting-upnext-position-value").assertTextEquals("Avant les applications")
        assertEquals(UpNextPosition.BEFORE_APPS, runBlocking { harness.prefs.upNextPosition.first() })
    }

    @Test
    fun `position list open when the launcher goes to the background is closed on return with focus on its row`() {
        val owner = object : LifecycleOwner {
            val registry = LifecycleRegistry(this).apply { currentState = Lifecycle.State.RESUMED }
            override val lifecycle: Lifecycle get() = registry
        }
        harness.install("Alpha")
        harness.openSettings()
        compose.setContent {
            CompositionLocalProvider(LocalLifecycleOwner provides owner) { harness.Content() }
        }
        compose.waitForIdle()
        openPositionList()
        press(Key.DirectionDown, "setting-upnext-position-list")
        compose.onNodeWithTag("setting-upnext-position-AFTER_APPS").assertIsFocused()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.CREATED }
        compose.waitForIdle()
        compose.runOnIdle { owner.registry.currentState = Lifecycle.State.RESUMED }
        compose.waitForIdle()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.onNodeWithTag("settings-screen").assertExists()
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        compose.onNodeWithTag("setting-upnext-position-value").assertTextEquals("Avant les applications")
        assertEquals(UpNextPosition.BEFORE_APPS, runBlocking { harness.prefs.upNextPosition.first() })
    }
}
