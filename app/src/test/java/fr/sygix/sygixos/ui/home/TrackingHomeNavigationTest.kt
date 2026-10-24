/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.runtime.*
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.tracking.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class TrackingHomeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun returnPromptDoesNotStealFocusAndRemainsReachableAfterItsCardDisappears() = runTest {
        val fixture = TrackingMovieFixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val content = fixture.content
        val store = ViewModelStore()
        try {
            show(viewModel(store, fixture))
            val card = "upnext-card-${TrackingProjection.card(content).key}"
            compose.onNodeWithTag(card).assertIsFocused()
            fixture.runtime.actions.returned(content, fixture.runtime.controller.state.value.generation)
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag(card).assertIsFocused()
            fixture.removed = true
            fixture.runtime.controller.refresh(); runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("app-tile-fixture.grid").assertIsFocused()
            press(Key.Back); press(Key.DirectionUp)
            compose.onNodeWithTag("settings-gear").assertIsFocused()
            press(Key.DirectionRight)
            compose.onNodeWithTag("tracking-return-prompt").assertIsFocused()
            press(Key.Back)
            compose.onNodeWithTag("settings-gear").assertIsFocused()
            assertNull(fixture.runtime.actions.notice.value)
            assertEquals(0, fixture.removals)
            assertEquals(0, fixture.writes)
        } finally { store.clear() }
    }

    @Test fun integratedUndoFocusPausesDeletionAndLeavingResumesOnlyRemainingTime() = runTest {
        val fixture = TrackingMovieFixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val content = fixture.content
        val store = ViewModelStore()
        try {
            show(viewModel(store, fixture))
            fixture.runtime.actions.perform(content, TrackingAction.REMOVE)
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("app-tile-fixture.grid").assertIsFocused()
            press(Key.Back); press(Key.DirectionUp); press(Key.DirectionRight)
            compose.onNodeWithTag("tracking-undo").assertIsFocused()
            advanceTimeBy(20_000); runCurrent()
            assertEquals(0, fixture.removals)
            press(Key.DirectionLeft)
            compose.onNodeWithTag("settings-gear").assertIsFocused()
            advanceTimeBy(9999); runCurrent(); assertEquals(0, fixture.removals)
            advanceTimeBy(1); runCurrent(); compose.waitForIdle()
            assertEquals(1, fixture.removals)
            assertNull(fixture.runtime.actions.notice.value)
            compose.onNodeWithTag("settings-gear").assertIsFocused()
        } finally { store.clear() }
    }

    private fun viewModel(store: ViewModelStore, fixture: TrackingMovieFixture) = ViewModelProvider(store, object : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackingViewModel(fixture.runtime) {} as T
    })[TrackingViewModel::class.java]

    @Test fun failedTrackingRemainsReachableWithoutCardsAndRetryRestoresTheGear() = runTest {
        val fixture = TrackingMovieFixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val store = ViewModelStore()
        try {
            show(viewModel(store, fixture))
            fixture.runtime.controller.markFailure(TrackingFailure.NETWORK)
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("zone-upnext").assertDoesNotExist()
            press(Key.Back); press(Key.DirectionUp); press(Key.DirectionRight)
            compose.onNodeWithTag("tracking-status-retry").assertIsFocused()
            press(Key.Enter, "tracking-status-detail")
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-status-detail").assertDoesNotExist()
            compose.onNodeWithTag("settings-gear").assertIsFocused()
            assertNull(fixture.runtime.controller.state.value.failure)
            assertEquals(0, fixture.removals)
        } finally { store.clear() }
    }

    private fun show(viewModel: TrackingViewModel) {
        compose.setContent {
            val state by viewModel.state.collectAsState()
            val row = state.tracking.contents.takeIf { it.isNotEmpty() }?.let { UpNextState(UpNextContent.Items(it.map(TrackingProjection::card)), UpNextPosition.BEFORE_APPS) }
            TestHome(catalogOf(emptyList(), listOf(app("fixture.grid", "Grid"))), heroStateOf(listOf(heroItem("fixture-hero", "Fixture hero", sourcePackage = "fixture.player"))),
                initialZone = Zone.HERO, upNext = row, trackingViewModel = viewModel, trackingUi = state)
        }
        compose.waitForIdle()
        compose.onNodeWithTag("zone-upnext").assertExists()
        press(Key.DirectionDown)
    }

    private fun press(key: Key, tag: String? = null) {
        val node = if (tag == null) compose.onRoot() else compose.onNodeWithTag(tag)
        node.performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }
}
