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
import fr.sygix.sygixos.model.BootStartState
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
class LauncherGuideTest {
    @get:Rule val compose = createComposeRule()
    @get:Rule val viewModels = LauncherViewModelRule()
    private val accessibility = "onboarding-step-accessibility"
    private val boot = "onboarding-step-boot"
    private val overlay = "onboarding-step-overlay"
    private val role = "onboarding-step-home-role"
    private val done = "onboarding-done"
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

    private fun press(key: Key) {
        compose.onRoot().performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun resume() {
        compose.runOnIdle { launcher!!.onResume() }
        compose.waitForIdle()
    }

    private fun bounds(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true).fetchSemanticsNode().boundsInRoot

    private fun part(tag: String) = compose.onNodeWithTag(tag, useUnmergedTree = true)

    private fun dot(tag: String) = part("$tag-dot").fetchSemanticsNode().config.getOrNull(StateDotColor)

    private fun assertHomeNeverFocused() {
        listOf("zone-hero", "zone-dock", "hero-capsule", "settings-gear").forEach {
            compose.onNodeWithTag(it).assert(SemanticsMatcher("$it not focused") { node -> node.config.getOrNull(SemanticsProperties.Focused) != true })
        }
    }

    @Test
    fun `guide is drawn over the hero in mockup order with the first step focused`() {
        show()
        compose.onNodeWithTag("onboarding-launcher").assertIsDisplayed()
        val mascot = bounds("onboarding-mascot")
        val title = bounds("onboarding-title")
        assertTrue(mascot.right <= title.left && title.center.y in mascot.top..mascot.bottom)
        part("onboarding-progress").assertTextEquals("0 étape sur 2 terminée")
        val order = listOf("onboarding-progress", accessibility, boot, role, "onboarding-hint")
        order.zipWithNext().forEach { (above, below) -> assertTrue("$above above $below", bounds(above).top < bounds(below).top) }
        compose.onNodeWithTag(overlay).assertDoesNotExist()
        val hint = bounds("onboarding-hint")
        val button = bounds(done)
        assertTrue(hint.right <= button.left && hint.center.y in button.top..button.bottom)
        compose.onNodeWithTag(accessibility).assertIsFocused()
        compose.onNodeWithTag(boot).assertIsOff()
        part("$boot-switch").assertExists()
        part("$role-state").assertTextEquals("Facultatif")
        part("$accessibility-state").assertTextEquals("À faire")
        part("$accessibility-marker").assertExists()
        part("$boot-state").assertDoesNotExist()
        val focusedBounds = bounds(accessibility)
        press(Key.DirectionDown)
        assertEquals(focusedBounds, bounds(accessibility))
        assertHomeNeverFocused()
    }

    @Test
    fun `navigation walks the shown steps then done without wrapping or leaving the panel`() {
        show()
        val focusables = listOf(accessibility, boot, role, done)
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
    }

    @Test
    fun `the overlay sub step appears under the boot step and counts in the progress`() {
        val gateway = FakeLauncherGateway(accessibility = SystemControlState.ACTIVE)
        val store = FakeLauncherStore()
        show(store, gateway)
        compose.onNodeWithTag(boot).assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag(boot).assertIsFocused()
        compose.onNodeWithTag(boot).assertIsOn()
        assertTrue(bounds(boot).bottom <= bounds(overlay).top && bounds(overlay).bottom <= bounds(role).top)
        part("$overlay-state").assertTextEquals("À faire")
        part("$boot-marker").assert(SemanticsMatcher("number 2") { node -> node.children.any { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() == "2" } })
        part("$overlay-marker").assert(SemanticsMatcher("number 2b") { node -> node.children.any { it.config.getOrNull(SemanticsProperties.Text)?.joinToString() == "2b" } })
        assertTrue(bounds("$overlay-marker").left > bounds("$boot-marker").left)
        part("onboarding-progress").assertTextEquals("1 étape sur 3 terminée")
        assertTrue(store.boot.enabled)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        press(Key.DirectionDown)
        compose.onNodeWithTag(overlay).assertIsFocused()
        press(Key.DirectionCenter)
        assertEquals(listOf("overlay"), gateway.opened)
        part("$overlay-state").assertTextEquals("En attente…")
        gateway.overlay = SystemControlState.ACTIVE
        resume()
        part("$overlay-state").assertTextEquals("Fait")
        compose.onNodeWithTag(overlay).assertIsFocused()
        part("onboarding-progress").assertTextEquals("3 étapes sur 3 terminées")
        part("$boot-check").assertExists()
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        compose.onNodeWithTag(overlay).assertDoesNotExist()
        compose.onNodeWithTag(boot).assertIsFocused()
    }

    @Test
    fun `states show their label dot marker and dimmed unavailable title`() {
        show(gateway = FakeLauncherGateway(accessibility = SystemControlState.ACTIVE, home = SystemControlState.PENDING))
        part("$accessibility-state").assertTextEquals("Fait")
        assertEquals(SygixColors.SwitchOn, dot(accessibility))
        part("$accessibility-check").assertExists()
        part("$role-state").assertTextEquals("En attente…")
        assertEquals(SygixColors.Badge, dot(role))
        part("$boot-dot").assertDoesNotExist()
    }

    @Test
    fun `a blocked service explains itself and still opens the accessibility settings`() {
        val gateway = FakeLauncherGateway(accessibility = SystemControlState.BLOCKED)
        show(gateway = gateway)
        part("$accessibility-state").assertTextEquals("Bloqué")
        part("$accessibility-dot").assertDoesNotExist()
        part("$accessibility-detail").assertTextEquals(
            "La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité ; si la TV le propose, autorisez aussi le démarrage automatique de SygixOs.",
        )
        press(Key.DirectionCenter)
        assertEquals(listOf("accessibility"), gateway.opened)
    }

    @Test
    fun `an unavailable step keeps the focus, shows its reason and opens nothing`() {
        val gateway = FakeLauncherGateway(accessibility = SystemControlState.UNAVAILABLE)
        show(gateway = gateway)
        compose.onNodeWithTag(accessibility).assertIsFocused()
        part("$accessibility-state").assertTextEquals("Indisponible")
        part("$accessibility-detail").assertTextEquals("Les réglages d’accessibilité ne peuvent pas s’ouvrir sur cette TV")
        press(Key.DirectionCenter)
        assertTrue(gateway.opened.isEmpty())
        part("onboarding-progress").assertTextEquals("0 étape sur 1 terminée")
        press(Key.DirectionDown)
        part("$accessibility-title").assert(SemanticsMatcher("dimmed") { it.config.getOrNull(RowTitleColor) == preferenceLabelColor(focused = false, dimmed = true) })
    }

    @Test
    fun `the role step is hidden without role nor replacement screen`() {
        show(gateway = FakeLauncherGateway(home = SystemControlState.UNAVAILABLE))
        compose.onNodeWithTag(role).assertDoesNotExist()
        press(Key.DirectionDown)
        press(Key.DirectionDown)
        compose.onNodeWithTag(done).assertIsFocused()
    }

    @Test
    fun `system return keeps the guide and the opener focused with the state reread`() {
        val gateway = FakeLauncherGateway()
        show(gateway = gateway)
        press(Key.DirectionCenter)
        part("$accessibility-state").assertTextEquals("En attente…")
        assertEquals(SygixColors.Badge, dot(accessibility))
        gateway.accessibility = SystemControlState.ACTIVE
        compose.runOnIdle { gateway.accessibilityUpdates.tryEmit(Unit) }
        compose.waitForIdle()
        part("$accessibility-state").assertTextEquals("En attente…")
        resume()
        part("$accessibility-state").assertTextEquals("Fait")
        part("$accessibility-check").assertExists()
        compose.onNodeWithTag("onboarding-launcher").assertExists()
        compose.onNodeWithTag(accessibility).assertIsFocused()
    }

    @Test
    fun `refused role goes through pending then optional and never done`() {
        val gateway = FakeLauncherGateway()
        show(gateway = gateway)
        repeat(2) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        part("$role-state").assertTextEquals("En attente…")
        resume()
        part("$role-state").assertTextEquals("Facultatif")
        compose.onNodeWithTag(role).assertIsFocused()
    }

    @Test
    fun `done back and home each close the automatic guide for good`() {
        val closers = listOf<() -> Unit>(
            { press(Key.DirectionDown); press(Key.DirectionDown); press(Key.DirectionDown); press(Key.DirectionCenter) },
            { press(Key.Back) },
            { compose.runOnIdle { launcher!!.onHome() }; compose.waitForIdle() },
        )
        closers.forEachIndexed { index, close ->
            val store = FakeLauncherStore()
            if (index == 0) {
                show(store)
            } else {
                compose.runOnIdle { launcher = viewModels.create(store, FakeLauncherGateway(), onboarding = true) }
                compose.waitForIdle()
            }
            compose.onNodeWithTag("onboarding-launcher").assertExists()
            close()
            compose.onNodeWithTag("onboarding-launcher").assertDoesNotExist()
            compose.onNodeWithTag("zone-hero").assertIsFocused()
            compose.waitUntil(5_000) { store.dismissed }
            compose.runOnIdle { launcher = viewModels.create(store, FakeLauncherGateway(), onboarding = true) }
            compose.waitForIdle()
            compose.onNodeWithTag("onboarding-launcher").assertDoesNotExist()
        }
    }

    @Test
    fun `boot chosen before closing stays enabled`() {
        val store = FakeLauncherStore()
        show(store, FakeLauncherGateway(overlay = SystemControlState.ACTIVE))
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        press(Key.Back)
        compose.waitUntil(5_000) { store.dismissed }
        assertTrue(store.boot.enabled)
    }

    @Test
    fun `a missing permission screen makes boot unavailable and refuses the option`() {
        val store = FakeLauncherStore()
        show(store, FakeLauncherGateway(overlay = SystemControlState.UNAVAILABLE))
        press(Key.DirectionDown)
        part("$boot-state").assertTextEquals("Indisponible")
        part("$boot-detail").assertTextEquals("Cette TV ne permet pas d’autoriser l’ouverture au démarrage")
        part("$boot-switch").assertDoesNotExist()
        compose.onNodeWithTag(boot).assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.ToggleableState))
        part("$boot-dot").assertDoesNotExist()
        press(Key.DirectionCenter)
        compose.onNodeWithTag(boot).assertIsFocused()
        compose.onNodeWithTag(overlay).assertDoesNotExist()
        assertFalse(store.boot.enabled)
    }

    @Test
    fun `a permission screen that fails to open turns boot off and unavailable`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway(succeeds = false)
        show(store, gateway)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        press(Key.DirectionDown)
        compose.onNodeWithTag(overlay).assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag(overlay).assertDoesNotExist()
        part("$boot-state").assertTextEquals("Indisponible")
        compose.onNodeWithTag(boot).assertIsFocused()
        compose.waitUntil(5_000) { !store.boot.enabled }
        press(Key.DirectionCenter)
        assertFalse(store.boot.enabled)
    }

    @Test
    fun `an unobserved automatic start only changes the boot detail`() {
        val store = FakeLauncherStore(LauncherSystemPreferences(bootStart = BootStartState(enabled = true, enabledAtBoot = 7)))
        show(store, FakeLauncherGateway(boot = 8, overlay = SystemControlState.ACTIVE))
        part("$boot-detail").assertTextEquals("SygixOs ne s’est pas ouvert automatiquement à ce démarrage")
        compose.onNodeWithTag(boot).assertIsOn()
        compose.runOnIdle { launcher!!.onBootObserved() }
        compose.waitForIdle()
        part("$boot-detail").assertTextEquals("Quand la TV démarre, SygixOs s’ouvre au lieu de l’accueil Google TV.")
    }
}
