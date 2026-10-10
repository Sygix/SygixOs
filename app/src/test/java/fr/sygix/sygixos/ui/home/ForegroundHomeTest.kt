/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import fr.sygix.sygixos.ui.settings.HomeScreenActions
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class ForegroundHomeTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val viewModels = LauncherViewModelRule()
    private val store = FakeLauncherStore()
    private lateinit var launcher: LauncherSystemViewModel
    private var restored: List<String>? = null
    private val positions = mutableListOf<UpNextPosition>()
    private val source = UpNextSourceEntry("com.source", "Source")

    private fun content(onboarding: Boolean = false, started: Boolean = true, upNext: Boolean = false) {
        launcher = viewModels.create(store, started = started, onboarding = onboarding)
        val row = if (upNext) {
            UpNextState(UpNextContent.Items(listOf(UpNextItem(1L, source, UpNextContentType.MOVIE, "Movie"))), UpNextPosition.BEFORE_APPS)
        } else {
            null
        }
        compose.setContent {
            val state by launcher.state.collectAsState()
            TestHome(
                catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(emptyList()),
                launcherState = state,
                launcherActions = launcher.actions,
                onRestoreOrder = { restored = it },
                upNext = row,
                homeScreenActions = HomeScreenActions(onUpNextPosition = { positions += it }),
            )
        }
        compose.waitForIdle()
    }

    private fun home() {
        compose.runOnIdle { launcher.onHome() }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        listOf("app-menu", "upnext-menu", "move-banner", "settings-screen", "setting-upnext-position-list", "onboarding-launcher")
            .forEach { compose.onNodeWithTag(it).assertDoesNotExist() }
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun longPress() {
        compose.mainClock.autoAdvance = false
        compose.onRoot().performKeyInput { keyDown(Key.DirectionCenter) }
        compose.mainClock.advanceTimeBy(Motion.LONG_PRESS_MS + 100)
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    private fun openHomeScreenSettings() {
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionRight)
        compose.onNodeWithTag("setting-upnext-visible").assertIsFocused()
    }

    @Test
    fun `home returns from grid to hero and remains stable on repeated home`() {
        content()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("app-tile-com.grid").assertIsFocused()
        home()
        home()
    }

    @Test
    fun `home closes settings in one step`() {
        content()
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("settings-screen").assertExists()
        home()
    }

    @Test
    fun `home closes the position dropdown and settings together without changing the value`() {
        content()
        openHomeScreenSettings()
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setting-upnext-position-list").assertExists()
        home()
        assertTrue(positions.isEmpty())
        openHomeScreenSettings()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
    }

    @Test
    fun `home dismisses the app context menu`() {
        content()
        press(Key.DirectionDown)
        longPress()
        compose.onNodeWithTag("app-menu").assertExists()
        home()
    }

    @Test
    fun `home dismisses the up next context menu`() {
        content(upNext = true)
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("upnext-card-com.source:1").assertIsFocused()
        press(Key.Menu)
        compose.onNodeWithTag("upnext-menu").assertExists()
        home()
    }

    @Test
    fun `home cancels move mode and restores the original order`() {
        content()
        repeat(2) { press(Key.DirectionDown) }
        longPress()
        press(Key.DirectionDown)
        compose.onNodeWithTag("menu-action-move").assertIsFocused()
        press(Key.Enter)
        compose.onNodeWithTag("move-banner").assertExists()
        home()
        assertEquals(listOf("com.grid"), restored)
    }

    @Test
    fun `home closes the onboarding for good`() {
        content(onboarding = true)
        compose.onNodeWithTag("onboarding-launcher").assertExists()
        home()
        compose.waitUntil(5_000) { store.dismissed }
    }

    @Test
    fun `home on the hero keeps its focus`() {
        content()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        home()
        assertEquals(1L, launcher.state.value.homeRequest)
    }

    @Test
    fun `home during the splash changes nothing`() {
        content(started = false)
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        compose.runOnIdle { launcher.onHome() }
        compose.waitForIdle()
        compose.onNodeWithTag("settings-screen").assertExists()
        assertEquals(0L, launcher.state.value.homeRequest)
    }
}
