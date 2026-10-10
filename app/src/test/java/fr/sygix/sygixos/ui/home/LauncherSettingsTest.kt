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
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.model.BootStartState
import fr.sygix.sygixos.model.LauncherSystemPreferences
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
    private val launcherRows = listOf("setting-home-role", "setting-boot-start", "setting-accessibility-home")
    private lateinit var launcher: LauncherSystemViewModel

    private fun open(store: FakeLauncherStore = FakeLauncherStore(), gateway: FakeLauncherGateway = FakeLauncherGateway()) {
        launcher = viewModels.create(store, gateway)
        compose.setContent {
            val state by launcher.state.collectAsState()
            TestHome(
                catalog = catalogOf(dock = listOf(app("com.dock", "Dock")), grid = listOf(app("com.grid", "Grid"))),
                hero = heroStateOf(emptyList()),
                launcherState = state,
                launcherActions = launcher.actions,
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

    private fun detail(tag: String) = compose.onNodeWithTag("$tag-detail", useUnmergedTree = true)

    private fun state(tag: String) = compose.onNodeWithTag("$tag-state", useUnmergedTree = true)

    @Test
    fun `launcher rows follow up next rows in order and fit the TV screen`() {
        open()
        val order = listOf("setting-upnext-visible", "setting-upnext-position", "setting-launcher-group") + launcherRows
        order.zipWithNext().forEach { (above, below) -> assertTrue("$above above $below", top(above) < top(below)) }
        val screen = compose.onRoot().fetchSemanticsNode().boundsInRoot.bottom
        val density = compose.density.density
        val position = compose.onNodeWithTag("setting-upnext-position").fetchSemanticsNode().boundsInRoot
        val title = compose.onNodeWithTag("setting-launcher-group", useUnmergedTree = true).fetchSemanticsNode().boundsInRoot
        val firstRow = compose.onNodeWithTag(launcherRows.first()).fetchSemanticsNode().boundsInRoot
        assertEquals(position.bottom, title.top, 1f)
        assertEquals(title.bottom, firstRow.top, 1f)
        val textHeight = title.height / density - (Dimens.SettingsGroupTitleTop + Dimens.SettingsGroupTitleBottom).value
        assertTrue("group title text $textHeight dp", textHeight in 10f..24f)
        assertTrue(compose.onNodeWithTag(launcherRows.last()).fetchSemanticsNode().boundsInRoot.bottom <= screen)
        compose.onNodeWithTag("setting-launcher-group", useUnmergedTree = true)
            .assert(SemanticsMatcher.keyNotDefined(SemanticsProperties.Focused))
        compose.onNodeWithTag("setting-boot-start").assertIsOff()
        detail("setting-home-role").assertTextEquals("SygixOs devient l’écran d’accueil de la TV")
        detail("setting-boot-start").assertTextEquals("Ouvrir SygixOs quand la TV s’allume")
        detail("setting-accessibility-home").assertTextEquals("Revenir à SygixOs quand la touche Home ouvre l’accueil du constructeur")
        state("setting-home-role").assertTextEquals("Inactif")
        state("setting-accessibility-home").assertTextEquals("Inactif")
        assertEquals(listOf(SettingsCategory.SOURCES, SettingsCategory.HIDDEN, SettingsCategory.HOME_SCREEN, SettingsCategory.ABOUT), SettingsCategory.entries)
    }

    @Test
    fun `dpad walks every row without the group title wrapping or escaping`() {
        open()
        press(Key.DirectionDown)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        repeat(4) { index ->
            press(Key.DirectionDown)
            compose.onNodeWithTag(launcherRows[index.coerceAtMost(2)]).assertIsFocused()
        }
        val up = listOf("setting-boot-start", "setting-home-role", "setting-upnext-position", "setting-upnext-visible")
        up.forEach { tag ->
            press(Key.DirectionUp)
            compose.onNodeWithTag(tag).assertIsFocused()
        }
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-home-role").assertIsFocused()
        press(Key.DirectionRight)
        compose.onNodeWithTag("setting-home-role").assertIsFocused()
        press(Key.DirectionLeft)
        compose.onNodeWithTag("settings-category-HOME_SCREEN").assertIsFocused()
        press(Key.DirectionRight)
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-home-role").assertIsFocused()
        press(Key.Back)
        compose.onNodeWithTag("settings-screen").assertDoesNotExist()
    }

    @Test
    fun `state is reread on return and focus comes back to the opener`() {
        val gateway = FakeLauncherGateway()
        open(gateway = gateway)
        repeat(4) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-accessibility-home").assertIsFocused()
        press(Key.DirectionCenter)
        state("setting-accessibility-home").assertTextEquals("En attente…")
        compose.onNodeWithTag("setting-accessibility-home-dot", useUnmergedTree = true).assertExists()
        compose.runOnIdle {
            gateway.accessibility = SystemControlState.ACTIVE
            gateway.home = SystemControlState.ACTIVE
            launcher.onResume()
        }
        compose.waitForIdle()
        state("setting-accessibility-home").assertTextEquals("Actif")
        state("setting-home-role").assertTextEquals("Actif")
        compose.onNodeWithTag("setting-accessibility-home").assertIsFocused()
        assertEquals(listOf("accessibility"), gateway.opened)
    }

    @Test
    fun `closing the position dropdown never pulls focus to a launcher row`() {
        open()
        repeat(2) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-home-role").assertIsFocused()
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setting-upnext-position-list").performKeyInput { keyDown(Key.Back); keyUp(Key.Back) }
        compose.waitForIdle()
        compose.onNodeWithTag("setting-upnext-position-list").assertDoesNotExist()
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
        compose.onNodeWithTag("setting-home-role").assertIsNotFocused()
    }

    @Test
    fun `system screen that cannot open turns the control unavailable and inert`() {
        val gateway = FakeLauncherGateway(succeeds = false)
        open(gateway = gateway)
        repeat(4) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        state("setting-accessibility-home").assertTextEquals("Indisponible")
        detail("setting-accessibility-home").assertTextEquals("Les réglages d’accessibilité ne peuvent pas s’ouvrir sur cette TV")
        compose.onNodeWithTag("setting-accessibility-home").assertIsFocused()
        compose.runOnIdle { launcher.onResume() }
        compose.waitForIdle()
        state("setting-accessibility-home").assertTextEquals("Indisponible")
        press(Key.DirectionCenter)
        assertEquals(listOf("accessibility"), gateway.opened)
        press(Key.DirectionUp)
        press(Key.DirectionUp)
        press(Key.DirectionCenter)
        state("setting-home-role").assertTextEquals("Indisponible")
        detail("setting-home-role").assertTextEquals("Cette TV ne permet pas de changer l’écran d’accueil")
        press(Key.DirectionUp)
        compose.onNodeWithTag("setting-upnext-position").assertIsFocused()
    }

    @Test
    fun `unobserved automatic start only changes the boot detail`() {
        val store = FakeLauncherStore(LauncherSystemPreferences(bootStart = BootStartState(enabled = true, enabledAtBoot = 7)))
        val gateway = FakeLauncherGateway(boot = 8)
        open(store, gateway)
        detail("setting-boot-start").assertTextEquals("SygixOs ne s’est pas ouvert automatiquement à ce démarrage")
        compose.onNodeWithTag("setting-boot-start").assertIsOn()
        compose.runOnIdle { launcher.onBootObserved() }
        compose.waitForIdle()
        detail("setting-boot-start").assertTextEquals("Ouvrir SygixOs quand la TV s’allume")
        compose.runOnIdle {
            gateway.boot = 9
            launcher.onResume()
        }
        compose.waitForIdle()
        detail("setting-boot-start").assertTextEquals("SygixOs ne s’est pas ouvert automatiquement à ce démarrage")
        repeat(3) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-boot-start").assertIsFocused()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setting-boot-start").assertIsOff().assertIsFocused()
        detail("setting-boot-start").assertTextEquals("Ouvrir SygixOs quand la TV s’allume")
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-decline").assertExists()
        press(Key.Back)
        compose.onNodeWithTag("setting-boot-start").assertIsOn().assertIsFocused()
        detail("setting-boot-start").assertTextEquals("Ouvrir SygixOs quand la TV s’allume")
        assertEquals(9, store.boot.enabledAtBoot)
    }

    @Test
    fun `boot start is unavailable and refused without an overlay settings screen`() {
        val store = FakeLauncherStore()
        val gateway = FakeLauncherGateway(overlay = SystemControlState.UNAVAILABLE)
        open(store, gateway)
        repeat(3) { press(Key.DirectionDown) }
        compose.onNodeWithTag("setting-boot-start").assertIsFocused()
        state("setting-boot-start").assertTextEquals("Indisponible")
        detail("setting-boot-start").assertTextEquals("Cette TV ne permet pas d’autoriser l’ouverture au démarrage")
        compose.onNodeWithTag("setting-boot-start-switch", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithTag("setting-boot-start-dot", useUnmergedTree = true).assertDoesNotExist()
        press(Key.DirectionCenter)
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        compose.onNodeWithTag("setting-boot-start").assertIsFocused()
        state("setting-boot-start").assertTextEquals("Indisponible")
        assertFalse(store.boot.enabled)
        assertTrue(gateway.opened.isEmpty())
    }

    @Test
    fun `inactive accessibility service never prompts again by itself`() {
        val gateway = FakeLauncherGateway(accessibility = SystemControlState.INACTIVE)
        open(gateway = gateway)
        state("setting-accessibility-home").assertTextEquals("Inactif")
        compose.runOnIdle { launcher.onResume() }
        compose.waitForIdle()
        compose.onNodeWithTag("onboarding-launcher").assertDoesNotExist()
        compose.onNodeWithTag("launcher-overlay-confirmation").assertDoesNotExist()
        state("setting-accessibility-home").assertTextEquals("Inactif")
        assertTrue(gateway.opened.isEmpty())
        repeat(3) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        compose.onNodeWithTag("setting-boot-start").assertIsOn()
    }

    @Test
    fun `active accessibility service is shown active and still opens system settings`() {
        val gateway = FakeLauncherGateway(accessibility = SystemControlState.ACTIVE)
        open(gateway = gateway)
        state("setting-accessibility-home").assertTextEquals("Actif")
        compose.onNodeWithTag("setting-accessibility-home-dot", useUnmergedTree = true).assertExists()
        repeat(4) { press(Key.DirectionDown) }
        press(Key.DirectionCenter)
        assertEquals(listOf("accessibility"), gateway.opened)
        compose.runOnIdle { launcher.onResume() }
        compose.waitForIdle()
        state("setting-accessibility-home").assertTextEquals("Actif")
    }
}
