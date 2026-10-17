/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.model.*
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import fr.sygix.sygixos.ui.settings.LauncherSystemControls
import fr.sygix.sygixos.ui.settings.launcherSummaryLabel
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class DefaultLauncherPanelTest {
    @get:Rule val compose = createComposeRule()
    private val state = mutableStateOf(LauncherSystemState(accessibility = SystemControlState.INACTIVE, overlay = SystemControlState.INACTIVE, defaultLauncher = DefaultLauncherState(snapshot = HomeScreenSnapshot())))
    private val actions = mutableListOf<SetupAction>()
    private fun show(screen: SetupScreen = SetupScreen.CHOICE) {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(screen = screen))
        compose.setContent { SygixOsTheme { Box(Modifier.fillMaxSize()) { DefaultLauncherPanel(state.value, LauncherSystemActions(setup = { actions += it })) } } }
        compose.waitForIdle()
    }
    private fun key(key: Key) = compose.onNodeWithTag("setup-panel").performKeyInput { pressKey(key) }

    @Test fun choiceInitialFocusAndEdgesAreContained() {
        show()
        compose.onNodeWithTag("setup-choice-auto").assertIsFocused()
        key(Key.DirectionDown); compose.onNodeWithTag("setup-choice-manual").assertIsFocused()
        key(Key.DirectionDown); key(Key.DirectionDown); compose.onNodeWithTag("setup-later").assertIsFocused()
        key(Key.DirectionUp); key(Key.DirectionUp); key(Key.DirectionUp)
        key(Key.DirectionLeft); key(Key.DirectionRight)
        compose.onNodeWithTag("setup-choice-auto").assertIsFocused()
        compose.onNodeWithTag("setup-mascot").assertExists()
        key(Key.Back); assertEquals(listOf(SetupAction.BACK), actions)
    }

    @Test fun unavailableChoiceFocusesManualAndAutoDoesNothing() {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(access = AdbAccessState.UNREACHABLE))
        show()
        compose.onNodeWithTag("setup-choice-manual").assertIsFocused()
        key(Key.DirectionUp); key(Key.DirectionCenter)
        assertEquals(listOf(SetupAction.NONE), actions)
        compose.onNodeWithTag("setup-choice-auto-state", useUnmergedTree = true).assertTextEquals("Indisponible sur cette TV")
    }

    @Test fun debugAndAuthorizeFocusTheExplicitAction() {
        show(SetupScreen.DEBUG)
        compose.onNodeWithTag("setup-debug-open").assertIsFocused()
        compose.onNodeWithTag("setup-debug-step-1").assertExists()
        key(Key.DirectionUp); key(Key.DirectionDown)
        compose.onNodeWithTag("setup-debug-open").assertIsFocused()
        compose.runOnIdle { state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(screen = SetupScreen.AUTHORIZE, debugEnabled = true)) }
        compose.onNodeWithTag("setup-authorize-continue").assertIsFocused()
        key(Key.DirectionCenter); assertEquals(listOf(SetupAction.AUTHORIZE), actions)
    }

    @Test fun rejectedAuthorizationHasErrorAndRetryFocus() {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(error = true))
        show(SetupScreen.AUTHORIZE)
        compose.onNodeWithTag("setup-error-title").assertTextEquals("Autorisation refusée")
        compose.onNodeWithTag("setup-authorize-retry").assertIsFocused()
        key(Key.DirectionLeft); compose.onNodeWithTag("setup-authorize-back").assertIsFocused()
        key(Key.DirectionLeft); key(Key.DirectionUp); key(Key.DirectionDown)
        compose.onNodeWithTag("setup-authorize-back").assertIsFocused()
    }

    @Test fun confirmationDefaultsAreDistinctAndHaveNoMascot() {
        show(SetupScreen.DISABLE)
        compose.onNodeWithTag("confirm-disable-cancel").assertIsFocused()
        compose.onNodeWithTag("setup-mascot").assertDoesNotExist()
        key(Key.DirectionRight); key(Key.DirectionCenter)
        assertEquals(listOf(SetupAction.DISABLE), actions)
        compose.runOnIdle { state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(screen = SetupScreen.RESTORE)) }
        compose.onNodeWithTag("confirm-restore-accept").assertIsFocused()
        key(Key.DirectionRight); compose.onNodeWithTag("confirm-restore-accept").assertIsFocused()
    }

    @Test fun manualRowsAndOverlayAreOrderedWithVendorUnavailable() {
        state.value = state.value.copy(bootEnabled = true, defaultLauncher = state.value.defaultLauncher.copy(snapshot = HomeScreenSnapshot(vendorManager = true)))
        show(SetupScreen.MANUAL)
        compose.onNodeWithTag("setup-manual-home").assertIsFocused()
        key(Key.DirectionDown); compose.onNodeWithTag("setup-manual-boot").assertIsFocused()
        key(Key.DirectionDown); compose.onNodeWithTag("setup-manual-overlay").assertIsFocused()
        key(Key.DirectionDown); compose.onNodeWithTag("setup-manual-vendor-boot").assertIsFocused()
        key(Key.DirectionCenter); assertEquals(listOf(SetupAction.NONE), actions)
        key(Key.DirectionDown); compose.onNodeWithTag("setup-manual-finish").assertIsFocused()
    }

    @Test fun removedOverlayReturnsFocusToBootRow() {
        state.value = state.value.copy(bootEnabled = true, focusReturn = GuideStep.OVERLAY, overlay = SystemControlState.ACTIVE)
        show(SetupScreen.MANUAL)
        compose.onNodeWithTag("setup-manual-overlay").assertDoesNotExist()
        compose.onNodeWithTag("setup-manual-boot").assertIsFocused()
    }

    @Test fun settingsPanelStartsOnRelaunchAndRestoreIsConditional() {
        show(SetupScreen.PANEL)
        compose.onNodeWithTag("launcher-panel-relaunch").assertIsFocused()
        compose.onNodeWithTag("launcher-panel-restore").assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(snapshot = HomeScreenSnapshot(stock = mapOf("com.google.android.apps.tv.launcherx" to StockHomeState.DISABLED_USER)))) }
        key(Key.DirectionDown)
        compose.onNodeWithTag("launcher-panel-restore").assertIsFocused()
    }

    @Test fun accessibilityConnectionChangesDoNotStealPanelFocus() {
        show(SetupScreen.PANEL)
        key(Key.DirectionUp)
        compose.onNodeWithTag("launcher-panel-manual").assertIsFocused()
        compose.runOnIdle { state.value = state.value.copy(accessibility = SystemControlState.PENDING) }
        compose.onNodeWithTag("launcher-panel-manual").assertIsFocused()
        compose.runOnIdle { state.value = state.value.copy(accessibility = SystemControlState.ACTIVE) }
        compose.onNodeWithTag("launcher-panel-manual").assertIsFocused()
    }

    @Test fun detailsShowOnlyRecordedChangesAndStock() {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(modifications = setOf(SetupStep.HOME)))
        show(SetupScreen.DETAILS)
        compose.onNodeWithTag("setup-details-close").assertIsFocused()
        compose.onNodeWithTag("setup-details-item-home").assertExists()
        compose.onNodeWithTag("setup-details-item-overlay").assertDoesNotExist()
        compose.onNodeWithTag("setup-details-item-stock").assertExists()
        compose.onNodeWithTag("setup-mascot").assertDoesNotExist()
    }

    @Test fun failedStepShowsCancellationAndRetry() {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(steps = mapOf(SetupStep.HOME to SetupStepState.FAILED, SetupStep.VERIFY to SetupStepState.CANCELLED)))
        show(SetupScreen.FAILED)
        compose.onNodeWithTag("setup-failed-retry").assertIsFocused()
        compose.onNodeWithTag("setup-step-home-state").assertTextEquals("Échec")
        compose.onNodeWithTag("setup-step-verify-state").assertTextEquals("Annulée")
    }

    @Test fun vendorFailureAfterFallbackIsExplainedAndControlsFitTvViewport() {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(steps = mapOf(
            SetupStep.VENDOR_BOOT to SetupStepState.FAILED, SetupStep.HOME to SetupStepState.DONE, SetupStep.VERIFY to SetupStepState.FAILED,
            SetupStep.ACCESSIBILITY to SetupStepState.DONE, SetupStep.OVERLAY to SetupStepState.DONE, SetupStep.BOOT to SetupStepState.DONE,
        )))
        show(SetupScreen.FAILED)
        compose.onNodeWithTag("setup-error-title").assertTextEquals("Le démarrage automatique n'a pas pu être autorisé")
        compose.onNodeWithTag("setup-step-boot-state").assertTextEquals("Échec")
        compose.onNodeWithTag("setup-failed-retry").assertIsFocused().assertIsDisplayed()
        val button = compose.onNodeWithTag("setup-failed-retry").fetchSemanticsNode().boundsInRoot
        val viewport = compose.onNodeWithTag("setup-panel").fetchSemanticsNode().boundsInRoot
        assertTrue(button.bottom <= viewport.bottom)
    }

    @Test fun successTipDisappearsWhenDebugIsDisabled() {
        state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(debugEnabled = true, snapshot = HomeScreenSnapshot(effective = true)))
        show(SetupScreen.DONE)
        compose.onNodeWithTag("setup-done-finish").assertIsFocused()
        compose.onNodeWithTag("setup-done-tip").assertExists()
        compose.runOnIdle { state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(debugEnabled = false)) }
        compose.onNodeWithTag("setup-done-tip").assertDoesNotExist()
    }

    @Test fun settingsSummaryPrioritizesActiveThenBlockedThenPartial() {
        val snapshot = DefaultLauncherState(snapshot = HomeScreenSnapshot())
        assertEquals(R.string.setup_state_configure, launcherSummaryLabel(snapshot, LauncherSystemState(accessibility = SystemControlState.INACTIVE)))
        assertEquals(R.string.setup_state_partial, launcherSummaryLabel(snapshot, LauncherSystemState(bootEnabled = true)))
        assertEquals(R.string.setup_state_blocked, launcherSummaryLabel(snapshot, LauncherSystemState(accessibility = SystemControlState.BLOCKED)))
        assertEquals(R.string.setup_state_active_summary, launcherSummaryLabel(snapshot.copy(snapshot = HomeScreenSnapshot(effective = true)), LauncherSystemState(accessibility = SystemControlState.BLOCKED)))
    }

    @Test fun settingsRowRendersBothRouteStatesAndOnlyActiveHasDot() {
        compose.setContent { SygixOsTheme { Column { LauncherSystemControls(state.value, LauncherSystemActions(), true) } } }
        val detail = compose.onNode(hasAnyAncestor(hasTestTag("setting-launcher-guide")) and hasText("Automatique", substring = true), useUnmergedTree = true)
        val label = compose.onNodeWithTag("setting-launcher-guide-state", useUnmergedTree = true)
        detail.assertTextEquals("Automatique : à configurer · Par autorisations : à faire")
        label.assertTextEquals("À configurer")
        compose.runOnIdle { state.value = state.value.copy(bootEnabled = true) }
        detail.assertTextEquals("Automatique : à configurer · Par autorisations : en partie")
        label.assertTextEquals("En partie")
        compose.runOnIdle { state.value = state.value.copy(accessibility = SystemControlState.BLOCKED) }
        detail.assertTextEquals("Automatique : à configurer · Par autorisations : bloquée")
        label.assertTextEquals("Bloqué")
        compose.onNodeWithTag("setting-launcher-guide-dot", useUnmergedTree = true).assertDoesNotExist()
        compose.runOnIdle { state.value = state.value.copy(accessibility = SystemControlState.INACTIVE, bootEnabled = false, defaultLauncher = state.value.defaultLauncher.copy(snapshot = HomeScreenSnapshot(effective = true))) }
        detail.assertTextEquals("Automatique : actif · Par autorisations : non utilisée")
        label.assertTextEquals("Actif")
        compose.onNodeWithTag("setting-launcher-guide-dot", useUnmergedTree = true).assertExists()
    }
}
