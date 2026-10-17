/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.domain.TsuyuInstallError
import fr.sygix.sygixos.domain.TsuyuInstallStep
import fr.sygix.sygixos.domain.TsuyuProbe
import fr.sygix.sygixos.domain.TsuyuRelease
import fr.sygix.sygixos.model.*
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w960dp-h540dp-xhdpi")
class DefaultLauncherTsuyuPanelTest {
    @get:Rule val compose = createComposeRule()
    private val release = TsuyuRelease("v1.0.0", false, "https://example.invalid/tsuyu.apk", 10, "a".repeat(64))
    private val active = TsuyuProbe(installed = true, binder = true, apiVersion = 13, authorized = true)
    private val actions = mutableListOf<SetupAction>()

    private fun show(launcher: DefaultLauncherState, system: LauncherSystemState = LauncherSystemState(accessibility = SystemControlState.INACTIVE, overlay = SystemControlState.INACTIVE)) {
        val state = mutableStateOf(system.copy(defaultLauncher = launcher))
        compose.setContent { SygixOsTheme { Box(Modifier.fillMaxSize()) { DefaultLauncherPanel(state.value, LauncherSystemActions(setup = { actions += it })) } } }
        compose.waitForIdle()
    }

    private fun key(key: Key) = compose.onNodeWithTag("setup-panel").performKeyInput { pressKey(key) }
    private fun state(tag: String) = compose.onNodeWithTag("$tag-state", useUnmergedTree = true)

    private fun base(screen: SetupScreen, plan: List<SetupPhase> = SetupPhase.entries) =
        DefaultLauncherState(screen = screen, snapshot = HomeScreenSnapshot(), tsuyuRelease = release, route = SetupRoute.TSUYU, plan = plan)

    @Test fun choiceWithTsuyuListsThreeWaysAndFocusesTsuyu() {
        show(base(SetupScreen.CHOICE))
        compose.onNodeWithTag("setup-choice-tsuyu").assertIsFocused()
        compose.onNodeWithTag("setup-step-counter").assertDoesNotExist()
        val tops = listOf("setup-choice-tsuyu", "setup-choice-auto", "setup-choice-manual").map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot().top }
        assertEquals(tops.sorted(), tops)
        key(Key.DirectionDown); compose.onNodeWithTag("setup-choice-auto").assertIsFocused()
        key(Key.DirectionDown); key(Key.DirectionDown); key(Key.DirectionDown); compose.onNodeWithTag("setup-later").assertIsFocused()
        key(Key.DirectionUp); key(Key.DirectionUp); key(Key.DirectionUp); key(Key.DirectionUp)
        key(Key.DirectionLeft); key(Key.DirectionRight)
        compose.onNodeWithTag("setup-choice-tsuyu").assertIsFocused()
        key(Key.DirectionCenter)
        assertEquals(listOf(SetupAction.AUTO_TSUYU), actions)
    }

    @Test fun choiceWithoutReleaseKeepsTheP7Lines() {
        show(DefaultLauncherState(snapshot = HomeScreenSnapshot()))
        compose.onNodeWithTag("setup-choice-tsuyu").assertDoesNotExist()
        compose.onNodeWithTag("setup-choice-auto").assertIsFocused()
    }

    @Test fun unreachablePortHidesTsuyu() {
        show(base(SetupScreen.CHOICE).copy(portUnreachable = true))
        compose.onNodeWithTag("setup-choice-tsuyu").assertDoesNotExist()
        compose.onNodeWithTag("setup-choice-manual").assertIsFocused()
        state("setup-choice-auto").assertTextEquals("Indisponible sur cette TV")
    }

    @Test fun debugGuideHasStepStatesAndTheMatchingButton() {
        show(base(SetupScreen.DEBUG))
        compose.onNodeWithTag("setup-step-counter").assertTextEquals("Étape 1 sur 5")
        state("setup-debug-step-1").assertTextEquals("À faire")
        state("setup-debug-step-2").assertTextEquals("À faire")
        compose.onNodeWithTag("setup-debug-open").assertIsFocused()
        compose.onNodeWithText("Ouvrir À propos", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("setup-debug-status").assertDoesNotExist()
    }

    @Test fun debugGuideAfterOptionsAreShown() {
        show(base(SetupScreen.DEBUG).copy(devOptions = true))
        state("setup-debug-step-1").assertTextEquals("Fait")
        compose.onNodeWithTag("setup-debug-step-1-dot", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("Ouvrir les options pour les développeurs", useUnmergedTree = true).assertExists()
    }

    @Test fun installScreenFocusesInstallAndOffersSkip() {
        show(base(SetupScreen.TSUYU_INSTALL))
        compose.onNodeWithTag("setup-step-counter").assertTextEquals("Étape 2 sur 5")
        compose.onNodeWithTag("tsuyu-install-accept").assertIsFocused()
        state("tsuyu-install-status").assertTextEquals("Non installé")
        key(Key.DirectionUp); key(Key.DirectionDown); compose.onNodeWithTag("tsuyu-install-accept").assertIsFocused()
        key(Key.DirectionLeft); compose.onNodeWithTag("tsuyu-install-skip").assertIsFocused()
        key(Key.DirectionLeft); compose.onNodeWithTag("tsuyu-install-skip").assertIsFocused()
        key(Key.DirectionCenter)
        assertEquals(listOf(SetupAction.TSUYU_SKIP), actions)
    }

    @Test fun downloadShowsProgressWithoutButtons() {
        show(base(SetupScreen.TSUYU_INSTALL).copy(tsuyuInstall = TsuyuInstallStep.Downloading(42)))
        state("tsuyu-install-status").assertTextEquals("Téléchargement… 42 %")
        compose.onNodeWithTag("tsuyu-install-status-dot", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("tsuyu-install-accept").assertDoesNotExist()
        compose.onNodeWithTag("setup-hint").assertTextEquals("Retour : annuler")
    }

    @Test fun installErrorFocusesRetry() {
        show(base(SetupScreen.TSUYU_INSTALL).copy(tsuyuError = TsuyuInstallError.NO_NETWORK))
        compose.onNodeWithTag("setup-error-title").assertTextEquals("Tsuyu n'a pas pu être téléchargé")
        compose.onNodeWithTag("tsuyu-install-retry").assertIsFocused()
        compose.onNodeWithTag("tsuyu-install-skip").assertExists()
    }

    @Test fun startScreenOpensTsuyu() {
        show(base(SetupScreen.TSUYU_START).copy(tsuyu = TsuyuProbe(installed = true)))
        compose.onNodeWithTag("setup-step-counter").assertTextEquals("Étape 3 sur 5")
        state("tsuyu-start-status").assertTextEquals("Arrêté")
        compose.onNodeWithTag("tsuyu-start-open").assertIsFocused()
        key(Key.DirectionCenter)
        assertEquals(listOf(SetupAction.TSUYU_OPEN), actions)
    }

    @Test fun authorizeThenRefusalThenNoMoreRequests() {
        val waiting = base(SetupScreen.TSUYU_AUTHORIZE).copy(tsuyu = TsuyuProbe(installed = true, binder = true, apiVersion = 13))
        show(waiting)
        compose.onNodeWithTag("tsuyu-authorize-continue").assertIsFocused()
        state("tsuyu-authorize-status").assertTextEquals("Actif")
    }

    @Test fun refusedAuthorizationFocusesRetry() {
        show(base(SetupScreen.TSUYU_AUTHORIZE).copy(tsuyu = TsuyuProbe(installed = true, binder = true, apiVersion = 13), tsuyuRefusal = TsuyuRefusal.REFUSED))
        compose.onNodeWithTag("setup-error-title").assertTextEquals("SygixOs n'est pas autorisé")
        compose.onNodeWithTag("tsuyu-authorize-retry").assertIsFocused()
        key(Key.DirectionLeft); compose.onNodeWithTag("tsuyu-authorize-back").assertIsFocused()
    }

    @Test fun noMoreRequestsOffersContinuingWithoutTsuyu() {
        show(base(SetupScreen.TSUYU_AUTHORIZE).copy(tsuyu = TsuyuProbe(installed = true, binder = true, apiVersion = 13), tsuyuRefusal = TsuyuRefusal.NO_MORE_REQUESTS))
        compose.onNodeWithTag("tsuyu-authorize-skip").assertIsFocused()
        compose.onNodeWithTag("tsuyu-authorize-retry").assertDoesNotExist()
    }

    @Test fun lastChoiceFocusesKeepAndNavigatesVertically() {
        show(base(SetupScreen.MORE))
        compose.onNodeWithTag("setup-more-keep").assertIsFocused()
        compose.onNodeWithTag("setup-step-counter").assertTextEquals("Étape 5 sur 5")
        key(Key.DirectionLeft); key(Key.DirectionRight); compose.onNodeWithTag("setup-more-keep").assertIsFocused()
        key(Key.DirectionDown); compose.onNodeWithTag("setup-more-disable").assertIsFocused()
        key(Key.DirectionDown); compose.onNodeWithTag("setup-more-disable").assertIsFocused()
        key(Key.DirectionUp); key(Key.DirectionUp); compose.onNodeWithTag("setup-more-keep").assertIsFocused()
        compose.onNodeWithTag("setup-hint").assertTextEquals("Retour : garder")
    }

    @Test fun resultsWithTsuyuShowRepairInsteadOfTheDebugTip() {
        val launcher = base(SetupScreen.FALLBACK).copy(tsuyu = active, debugEnabled = true, modifications = setOf(SetupStep.REPAIR))
        show(launcher)
        state("setup-fallback-repair").assertTextEquals("Active")
        compose.onNodeWithTag("setup-done-tip").assertDoesNotExist()
    }

    @Test fun doneWithoutTsuyuKeepsTheDebugTip() {
        show(DefaultLauncherState(screen = SetupScreen.DONE, snapshot = HomeScreenSnapshot(effective = true), debugEnabled = true))
        compose.onNodeWithTag("setup-done-tip").assertExists()
        compose.onNodeWithTag("setup-done-repair").assertDoesNotExist()
    }

    @Test fun panelShowsTsuyuAndReplaceWithoutCounter() {
        val snapshot = HomeScreenSnapshot(provisioned = true, ownHomeEnabled = true, roleHolder = "fr.sygix.sygixos", accessibilityConnected = true, overlay = true,
            stock = mapOf("com.google.android.apps.tv.launcherx" to StockHomeState.ACTIVE))
        show(DefaultLauncherState(screen = SetupScreen.PANEL, snapshot = snapshot, bootEnabled = true, tsuyu = active, tsuyuRelease = release, fromPanel = true, plan = SetupPhase.entries))
        val order = listOf("launcher-panel-auto", "launcher-panel-tsuyu", "launcher-panel-manual", "launcher-panel-relaunch", "launcher-panel-replace").map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot().top }
        assertEquals(order.sorted(), order)
        state("launcher-panel-tsuyu").assertTextEquals("Actif")
        compose.onNodeWithText("Refait les réglages automatiques", useUnmergedTree = true).assertExists()
        compose.onNodeWithTag("launcher-panel-restore").assertDoesNotExist()
        compose.onNodeWithTag("setup-step-counter").assertDoesNotExist()
        compose.onNodeWithTag("launcher-panel-relaunch").assertIsFocused()
        key(Key.DirectionDown); key(Key.DirectionCenter)
        assertEquals(listOf(SetupAction.REPLACE_CONFIRM), actions)
    }

    @Test fun panelTsuyuStates() {
        val labels = mapOf(
            TsuyuProbe() to "À installer",
            TsuyuProbe(installed = true) to "Arrêté",
            TsuyuProbe(installed = true, binder = true, apiVersion = 13) to "À autoriser",
            TsuyuProbe(installed = true, binder = true, apiVersion = 12, authorized = true) to "À mettre à jour",
        )
        val state = mutableStateOf(LauncherSystemState(defaultLauncher = DefaultLauncherState(screen = SetupScreen.PANEL, snapshot = HomeScreenSnapshot(), tsuyuRelease = release, fromPanel = true)))
        compose.setContent { SygixOsTheme { Box(Modifier.fillMaxSize()) { DefaultLauncherPanel(state.value, LauncherSystemActions()) } } }
        labels.forEach { (probe, label) ->
            compose.runOnIdle { state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(tsuyu = probe)) }
            state("launcher-panel-tsuyu").assertTextEquals(label)
        }
        compose.runOnIdle { state.value = state.value.copy(defaultLauncher = state.value.defaultLauncher.copy(tsuyu = TsuyuProbe(), tsuyuRelease = null)) }
        compose.onNodeWithTag("launcher-panel-tsuyu").assertDoesNotExist()
    }

    @Test fun detailsListTsuyuAndRepairs() {
        show(DefaultLauncherState(screen = SetupScreen.DETAILS, snapshot = HomeScreenSnapshot(), tsuyu = active, tsuyuRelease = release,
            modifications = setOf(SetupStep.TSUYU, SetupStep.REPAIR, SetupStep.ACCESSIBILITY), lastRepair = RepairRecord(SetupStep.ACCESSIBILITY, 1_794_700_800_000L)))
        val order = listOf("setup-details-item-stock", "setup-details-item-tsuyu", "setup-details-item-repairs").map { compose.onNodeWithTag(it).getUnclippedBoundsInRoot().top }
        assertEquals(order.sorted(), order)
        state("setup-details-item-tsuyu").assertTextEquals("Fait")
        state("setup-details-item-repairs").assertTextEquals("Active")
        compose.onNodeWithTag("setup-details-item-repairs-detail", useUnmergedTree = true).assertTextContains("Touche Home", substring = true)
        compose.onNodeWithTag("setup-mascot").assertDoesNotExist()
    }
}
