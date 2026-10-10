/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.ui.settings.RowTitleColor
import fr.sygix.sygixos.ui.settings.StateDotColor
import fr.sygix.sygixos.ui.settings.preferenceLabelColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class LauncherOnboardingTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val viewModels = LauncherViewModelRule()
    private val rows = listOf("onboarding-row-home-role", "onboarding-row-boot", "onboarding-row-accessibility")
    private val focusables = rows + "onboarding-continue"
    private var launcher by mutableStateOf<LauncherSystemViewModel?>(null)

    private fun show(store: FakeLauncherStore = FakeLauncherStore(), gateway: FakeLauncherGateway = FakeLauncherGateway()) {
        launcher = viewModels.create(store, gateway, onboarding = true)
        compose.setContent {
            launcher?.let { vm ->
                val state by vm.state.collectAsState()
                TestHome(
                    catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                    hero = heroStateOf(emptyList()),
                    launcherState = state,
                    launcherActions = vm.actions,
                )
            }
        }
        compose.waitForIdle()
    }

    private fun relaunch(store: FakeLauncherStore, gateway: FakeLauncherGateway) {
        compose.runOnIdle { launcher = viewModels.create(store, gateway, onboarding = true) }
        compose.waitForIdle()
    }

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun top(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot.top

    private fun assertHomeNeverFocused() {
        listOf("zone-hero", "zone-dock", "hero-capsule", "settings-gear").forEach {
            compose.onNodeWithTag(it).assert(SemanticsMatcher("$it not focused") { node -> node.config.getOrNull(SemanticsProperties.Focused) != true })
        }
    }

    @Test
    fun `onboarding is drawn over the hero in mockup order with the first row focused`() {
        show()
        compose.onNodeWithTag("zone-hero").assertExists()
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        val panel = compose.onNodeWithTag("onboarding-launcher").fetchSemanticsNode().boundsInRoot
        val hero = compose.onNodeWithTag("zone-hero").fetchSemanticsNode().boundsInRoot
        assertTrue(panel.contains(hero.center))
        val order = listOf("onboarding-mascot") + rows + listOf("onboarding-hint")
        order.zipWithNext().forEach { (above, below) -> assertTrue("$above above $below", top(above) < top(below)) }
        assertTrue(top("onboarding-continue") > top(rows.last()))
        val hint = compose.onNodeWithTag("onboarding-hint", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val continueButton = compose.onNodeWithTag("onboarding-continue").fetchSemanticsNode().boundsInRoot
        assertTrue(hint.right <= continueButton.left && hint.center.y in continueButton.top..continueButton.bottom)
        (order + "onboarding-continue").forEach { tag ->
            val bounds = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
            assertTrue(tag, panel.contains(bounds.topLeft) && panel.contains(bounds.bottomRight))
        }
        compose.onNodeWithTag(rows[0]).assertIsFocused()
        compose.onNodeWithTag(rows[1]).assertIsOff()
        rows.forEach { compose.onNodeWithTag("$it-detail", useUnmergedTree = true).assertExists() }
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("Inactif")
        compose.onNodeWithTag("onboarding-row-accessibility-state", useUnmergedTree = true).assertTextEquals("Inactif")
        assertHomeNeverFocused()
    }

    @Test
    fun `navigation stays inside the panel without wrapping or resizing rows`() {
        show()
        val initialBounds = focusables.map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot }
        repeat(4) { index ->
            press(Key.DirectionDown)
            compose.onNodeWithTag(focusables[(index + 1).coerceAtMost(3)]).assertIsFocused()
        }
        repeat(4) { index ->
            press(Key.DirectionUp)
            compose.onNodeWithTag(focusables[(2 - index).coerceAtLeast(0)]).assertIsFocused()
        }
        focusables.forEach { tag ->
            compose.onNodeWithTag(tag).assertIsFocused()
            press(Key.DirectionLeft)
            press(Key.DirectionRight)
            compose.onNodeWithTag(tag).assertIsFocused()
            assertHomeNeverFocused()
            press(Key.DirectionDown)
        }
        assertEquals(initialBounds, focusables.map { compose.onNodeWithTag(it).fetchSemanticsNode().boundsInRoot })
        initialBounds.zipWithNext().forEach { (a, b) -> assertTrue(a.bottom <= b.top) }
    }

    @Test
    fun `states show the right label dot colour and dimmed title`() {
        val gateway = FakeLauncherGateway(home = SystemControlState.ACTIVE, accessibility = SystemControlState.UNAVAILABLE)
        show(gateway = gateway)
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("Actif")
        compose.onNodeWithTag("onboarding-row-home-role-dot", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(StateDotColor, SygixColors.SwitchOn))
        compose.onNodeWithTag("onboarding-row-accessibility-state", useUnmergedTree = true).assertTextEquals("Indisponible")
        compose.onNodeWithTag("onboarding-row-accessibility-dot", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("onboarding-row-accessibility-detail", useUnmergedTree = true)
            .assertTextEquals("Les réglages d’accessibilité ne peuvent pas s’ouvrir sur cette TV")
        compose.onNodeWithTag("onboarding-row-accessibility-title", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(RowTitleColor, SygixColors.OnDarkSecondary))
        compose.onNodeWithTag("onboarding-row-boot-title", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(RowTitleColor, SygixColors.OnDark))
        press(Key.DirectionCenter)
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("onboarding-row-accessibility").assertIsFocused()
        compose.onNodeWithTag("onboarding-row-accessibility-detail", useUnmergedTree = true)
            .assertTextEquals("Les réglages d’accessibilité ne peuvent pas s’ouvrir sur cette TV")
        press(Key.DirectionCenter)
        assertTrue(gateway.opened.isEmpty())
        compose.runOnIdle { gateway.home = SystemControlState.INACTIVE }
        press(Key.DirectionUp)
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("Actif")
        compose.runOnIdle { launcher!!.onResume() }
        press(Key.DirectionCenter)
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("En attente…")
        compose.onNodeWithTag("onboarding-row-home-role-dot", useUnmergedTree = true).assert(SemanticsMatcher.expectValue(StateDotColor, SygixColors.Badge))
        assertEquals(listOf("home"), gateway.opened)
    }

    @Test
    fun `refused role returns inactive on the same row without automatic retry`() {
        val gateway = FakeLauncherGateway()
        show(gateway = gateway)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("En attente…")
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        compose.runOnIdle { launcher!!.onResume() }
        compose.waitForIdle()
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("Inactif")
        compose.onNodeWithTag("onboarding-row-home-role").assertIsFocused()
        assertEquals(listOf("home"), gateway.opened)
    }

    @Test
    fun `system return keeps the onboarding and the opener focused`() {
        val gateway = FakeLauncherGateway()
        show(gateway = gateway)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        compose.onNodeWithTag("onboarding-row-accessibility-state", useUnmergedTree = true).assertTextEquals("En attente…")
        compose.onNodeWithTag("onboarding-row-accessibility-dot", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        compose.onNodeWithTag("onboarding-row-accessibility").assertIsFocused()
        compose.runOnIdle {
            gateway.accessibility = SystemControlState.ACTIVE
            launcher!!.onResume()
        }
        compose.waitForIdle()
        compose.onNodeWithTag("onboarding-row-accessibility-state", useUnmergedTree = true).assertTextEquals("Actif")
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        compose.onNodeWithTag("onboarding-row-accessibility").assertIsFocused()
        assertFalse(launcher!!.state.value.systemPending)
    }

    @Test
    fun `continue closes for good and keeps the boot choice for settings`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway(overlay = SystemControlState.ACTIVE)
        show(store, gateway)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("onboarding-row-boot").assertIsOn().assertIsFocused()
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        assertClosedForGood(store, gateway)
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionRight)
        compose.onNodeWithTag("setting-boot-start-switch", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("setting-boot-start").assertIsOn()
    }

    @Test
    fun `back closes for good without requesting anything`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway()
        show(store, gateway)
        press(Key.Back)
        assertClosedForGood(store, gateway)
        assertTrue(gateway.opened.isEmpty())
        assertFalse(store.boot.enabled)
    }

    @Test
    fun `foreground home closes for good`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway()
        show(store, gateway)
        compose.runOnIdle { launcher!!.onHome() }
        assertClosedForGood(store, gateway)
    }

    @Test
    fun `overlay confirmation follows boot opt in and returns focus to the boot row`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway()
        show(store, gateway)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertIsDisplayed()
        compose.onNodeWithTag("launcher-overlay-open").assertIsFocused()
        press(Key.DirectionDown)
        compose.onNodeWithTag("launcher-overlay-decline").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("onboarding-row-boot").assertIsOn().assertIsFocused()
        assertTrue(store.boot.enabled)
        press(Key.DirectionCenter)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-open").assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("overlay"), gateway.opened)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        compose.runOnIdle { launcher!!.onResume() }
        compose.waitForIdle()
        compose.onNodeWithTag("onboarding-row-boot").assertIsOn().assertIsFocused()
        press(Key.DirectionCenter)
        press(Key.DirectionCenter)
        press(Key.Back)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        compose.onNodeWithTag("onboarding-row-boot").assertIsOn().assertIsFocused()
    }

    @Test
    fun `overlay settings that fail to open turn the boot row unavailable and off`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway(succeeds = false)
        show(store, gateway)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-open").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("onboarding-row-boot").assertIsFocused()
        compose.onNodeWithTag("onboarding-row-boot-state", useUnmergedTree = true).assertTextEquals("Indisponible")
        compose.onNodeWithTag("onboarding-row-boot-detail", useUnmergedTree = true)
            .assertTextEquals("Cette TV ne permet pas d’autoriser l’ouverture au démarrage")
        compose.onNodeWithTag("onboarding-row-boot-title", useUnmergedTree = true)
            .assert(SemanticsMatcher.expectValue(RowTitleColor, preferenceLabelColor(focused = true)))
        compose.waitUntil(5_000) { !store.boot.enabled }
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        assertFalse(store.boot.enabled)
        assertEquals(listOf("overlay"), gateway.opened)
    }

    private fun assertClosedForGood(store: FakeLauncherStore, gateway: FakeLauncherGateway) {
        compose.waitForIdle()
        compose.onNodeWithTag("onboarding-launcher").assertDoesNotExist()
        compose.onNodeWithTag("zone-hero").assertIsFocused()
        compose.waitUntil(5_000) { store.dismissed }
        relaunch(store, gateway)
        compose.onNodeWithTag("onboarding-launcher").assertDoesNotExist()
        assertFalse(launcher!!.state.value.showOnboarding)
    }

    @Test
    fun `onboarding returns after process death while never dismissed`() {
        val store = FakeLauncherStore(LauncherSystemPreferences())
        val gateway = FakeLauncherGateway()
        show(store, gateway)
        press(Key.DirectionCenter)
        relaunch(store, gateway)
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        compose.onNodeWithTag("onboarding-row-home-role-state", useUnmergedTree = true).assertTextEquals("Inactif")
    }
}
