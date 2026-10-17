/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassActive
import fr.sygix.sygixos.model.BootStartState
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.ui.settings.SettingsCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class LauncherSettingsTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val viewModels = LauncherViewModelRule()
    private val row = "setting-launcher-guide"
    private lateinit var launcher: LauncherSystemViewModel

    private fun open(store: FakeLauncherStore = FakeLauncherStore(), gateway: FakeLauncherGateway = FakeLauncherGateway(), glassBlur: Boolean = false) {
        launcher = viewModels.create(store, gateway)
        compose.setContent {
            val state by launcher.state.collectAsState()
            TestHome(
                catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(emptyList()),
                launcherState = state,
                launcherActions = launcher.actions,
                glassBlur = glassBlur,
            )
        }
        compose.waitForIdle()
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionRight)
        compose.onNodeWithTag("setting-upnext-visible").assertIsFocused()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top

    private fun resume() {
        compose.runOnIdle { launcher.onResume() }
        compose.waitForIdle()
    }

    private fun detail(tag: String) = compose.onNode(hasAnyAncestor(hasTestTag(tag)) and hasText("Automatique", substring = true), useUnmergedTree = true)

    private fun state(tag: String) = compose.onNodeWithTag("$tag-state", useUnmergedTree = true)

    @Test
    fun `the launcher line follows the up next rows under its group title and fits the TV screen`() {
        open()
        val order = listOf("setting-upnext-visible", "setting-upnext-position", "setting-launcher-group", row)
        order.zipWithNext().forEach { (above, below) -> assertTrue("$above above $below", top(above) < top(below)) }
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val density = compose.density.density
        val position = compose.onNodeWithTag("setting-upnext-position").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("setting-launcher-group", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val line = compose.onNodeWithTag(row).fetchSemanticsNode().boundsInRoot
        assertEquals(position.bottom, title.top, 1f)
        assertEquals(title.bottom, line.top, 1f)
        val textHeight = title.height / density - (Dimens.SettingsGroupTitleTop + Dimens.SettingsGroupTitleBottom).value
        assertTrue("group title text $textHeight dp", textHeight in 10f..24f)
        assertTrue(line.bottom <= screen)
        compose.onNodeWithTag("setting-launcher-group", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        listOf("setting-home-role", "setting-boot-start", "setting-accessibility-home", "onboarding-row-boot").forEach {
            compose.onNodeWithTag(it).assertDoesNotExist()
        }
        assertEquals(listOf(SettingsCategory.SOURCES, SettingsCategory.HIDDEN, SettingsCategory.HOME_SCREEN, SettingsCategory.ABOUT), SettingsCategory.entries)
    }

    @Test
    fun `dpad walks every line without the group title wrapping or escaping`() {
        open()
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        repeat(2) {
            press(Key.DirectionDown)
            compose.onNodeWithTag(row).assertIsFocused()
        }
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-upnext-visible").assertIsFocused()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag(row).assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag(row).assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-HOME_SCREEN").assertIsFocused()
        press(Key.DirectionRight)
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag(row).assertIsFocused()
        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
    }

    @Test
    fun `the guide opens over the settings and gives the focus back to the line without touching the seen flag`() {
        val store = FakeLauncherStore()
        open(store)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-panel").assertExists()
        compose.onNodeWithTag("settings-home-screen").assertExists()
        compose.onNodeWithTag("launcher-panel-relaunch").assertIsFocused()
        press(Key.Back)
        compose.onNodeWithTag("setup-panel").assertDoesNotExist()
        compose.onNodeWithTag("settings-screen").assertExists()
        compose.onNodeWithTag(row).assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-panel").assertExists()
        press(Key.DirectionUp)
        compose.onNodeWithTag("launcher-panel-manual").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setup-manual-home").assertIsFocused()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setup-manual-finish").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setup-panel").assertDoesNotExist()
        compose.onNodeWithTag(row).assertIsFocused()
        compose.waitForIdle()
        assertEquals(0, store.seenWrites)
    }

    @Test
    fun `the guide over the settings is glass blurring the settings page`() {
        open(glassBlur = true)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        val panel = "launcher-panel"
        assertTrue(compose.onNodeWithTag(panel).fetchSemanticsNode().config[GlassActive])
        val blurred = compose.blurredHaze(panel)
        assertTrue(compose.isHazeSource("settings-screen", blurred))
        assertFalse(compose.isHazeSource("zone-hero", blurred))
    }

    @Test
    fun `home while the guide is open over the settings closes everything`() {
        open()
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        compose.runOnIdle { launcher.onHome() }
        compose.waitForIdle()
        compose.onNodeWithTag("setup-panel").assertDoesNotExist()
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
    }

    @Test
    fun `the line sums up the guide`() {
        val gateway = FakeLauncherGateway(accessibility = SystemControlState.ACTIVE)
        val store = FakeLauncherStore()
        open(store, gateway)
        state(row).assertTextEquals("En partie")
        detail(row).assertTextEquals("Automatique : à configurer · Par autorisations : en partie")
        compose.onNodeWithTag("$row-dot", useUnmergedTree = true).assertDoesNotExist()
        gateway.overlay = SystemControlState.ACTIVE
        compose.runOnIdle { store.launcherSystem.value = store.launcherSystem.value.copy(bootStart = BootStartState(enabled = true)) }
        resume()
        state(row).assertTextEquals("Actif")
        detail(row).assertTextEquals("Automatique : à configurer · Par autorisations : active")
        compose.onNodeWithTag("$row-dot", useUnmergedTree = true).assertExists()
        gateway.accessibility = SystemControlState.BLOCKED
        resume()
        state(row).assertTextEquals("Bloqué")
        detail(row).assertTextEquals("Automatique : à configurer · Par autorisations : bloquée")
        gateway.accessibility = SystemControlState.INACTIVE
        compose.runOnIdle { store.launcherSystem.value = store.launcherSystem.value.copy(bootStart = BootStartState()) }
        resume()
        state(row).assertTextEquals("À configurer")
        detail(row).assertTextEquals("Automatique : à configurer · Par autorisations : à faire")
    }

    @Test
    fun `closing the position dropdown never pulls focus to the launcher line`() {
        open()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag(row).assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setting-upnext-position-list").performKeyInput { keyDown(Key.Back); keyUp(Key.Back) }
        compose.waitForIdle()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        compose.onNodeWithTag(row).assertIsNotFocused()
    }
}
