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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class TrackingHomeNavigationTest {
    @get:Rule val compose = createComposeRule()

    @Test fun publisherLeadsPickerAndFirstTrackedCardRegainsFocusAfterRealControllerReturn() = runTest {
        val scope = CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))
        val fixture = TrackingMovieFixture(scope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        val original = TrackingProjection.card(fixture.content).copy(id = 7, providerArtwork = false,
            source = fr.sygix.sygixos.model.UpNextSourceEntry("org.jellyfin.androidtv", "Jellyfin", targetIdentity = fixture.content.identity),
            sources = listOf(fr.sygix.sygixos.model.UpNextSourceEntry("org.jellyfin.androidtv", "Jellyfin", targetIdentity = fixture.content.identity)))
        val second = fr.sygix.sygixos.model.UpNextItem(999, fr.sygix.sygixos.model.UpNextSourceEntry("fixture.other"), fr.sygix.sygixos.model.UpNextContentType.MOVIE, "Other fixture", watchNextType = fr.sygix.sygixos.model.ProgramKind.WATCHLIST)
        val source = object : fr.sygix.sygixos.domain.UpNextSource {
            override suspend fun load() = Result.success(listOf(original, second))
            override fun changes() = kotlinx.coroutines.flow.emptyFlow<Unit>()
        }
        val row = fr.sygix.sygixos.domain.UpNextController(source, kotlinx.coroutines.flow.MutableStateFlow(true), kotlinx.coroutines.flow.MutableStateFlow(UpNextPosition.BEFORE_APPS),
            kotlinx.coroutines.flow.MutableStateFlow(emptySet()), scope, tracking = fixture.runtime.state,
            enrichment = kotlinx.coroutines.flow.MutableStateFlow(mapOf(original.key to fixture.content)), clock = { 10_000 + testScheduler.currentTime })
        scope.launch { fixture.runtime.opened.collect { row.opened(TrackingProjection.card(it.content)) } }
        fixture.playbackApps.value = listOf(PlaybackApp("fixture.alpha", "Alpha", true), PlaybackApp("org.jellyfin.androidtv", "Jellyfin", true))
        row.refresh(); runCurrent()
        val store = ViewModelStore()
        try {
            val vm = viewModel(store, fixture)
            compose.setContent {
                val state by vm.state.collectAsState()
                val upNext by row.state.collectAsState()
                TestHome(catalogOf(emptyList(), listOf(app("fixture.grid", "Grid"))), heroStateOf(listOf(heroItem("fixture-hero", "Fixture hero"))),
                    upNext = upNext, trackingViewModel = vm, trackingUi = state, onUpNextReturnHandled = row::returnHandled,
                    onOpenUpNext = { item -> fixture.runtime.playback.open(PlaybackRequest(item.tracking!!, PlaybackOrigin.UP_NEXT,
                        item.sources.map { PlaybackTarget(it.packageName, item.tracking.identity, it.intentUri) })) })
            }
            runCurrent(); compose.waitForIdle(); press(Key.DirectionDown)
            val firstKey = (row.state.value!!.content as UpNextContent.Items).items.first().key
            val firstTag = "upnext-card-$firstKey"
            compose.onNodeWithTag(firstTag).assertIsFocused()
            press(Key.Enter); runCurrent(); compose.waitForIdle()
            assertEquals(listOf("org.jellyfin.androidtv", "fixture.alpha"), vm.state.value.playerApps.map { it.packageName })
            compose.onNodeWithTag("tracking-player-org.jellyfin.androidtv").assertIsFocused()
            press(Key.Enter, "tracking-player-picker"); runCurrent(); compose.waitForIdle()
            assertEquals(1, fixture.launches.size)
            fixture.runtime.onBackground()
            compose.onNodeWithTag("upnext-card-${second.key}").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.RequestFocus) { it() }
            compose.onNodeWithTag("upnext-card-${second.key}").assertIsFocused()
            fixture.runtime.onForeground(); row.refresh(); runCurrent(); compose.waitForIdle()
            assertEquals(firstKey, (row.state.value!!.content as UpNextContent.Items).items.first().key)
            compose.onNodeWithTag(firstTag).assertIsFocused()
        } finally { store.clear() }
    }

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
            fixture.runtime.onForeground(); runCurrent()
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

    @Test fun restorationFailureBeforeIdentityKeepsStatusAndRetryReachableFromTheGear() = runTest {
        val fixture = TrackingMovieFixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))) { testScheduler.currentTime }
        fixture.storageFailure = TrackingFailure.STORAGE
        fixture.runtime.controller.restore(); runCurrent()
        assertNull(fixture.runtime.controller.state.value.account)
        val local = fr.sygix.sygixos.model.UpNextItem(999, fr.sygix.sygixos.model.UpNextSourceEntry("fixture.other"),
            fr.sygix.sygixos.model.UpNextContentType.MOVIE, "Local fixture", watchNextType = fr.sygix.sygixos.model.ProgramKind.WATCHLIST)
        val store = ViewModelStore()
        try {
            val vm = viewModel(store, fixture)
            compose.setContent {
                val state by vm.state.collectAsState()
                TestHome(catalogOf(emptyList(), listOf(app("fixture.grid", "Grid"))), heroStateOf(listOf(heroItem("fixture-hero", "Fixture hero"))),
                    initialZone = Zone.GRID, upNext = UpNextState(UpNextContent.Items(listOf(local)), UpNextPosition.BEFORE_APPS),
                    trackingViewModel = vm, trackingUi = state)
            }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-status").assertExists()
            press(Key.Back); press(Key.DirectionUp); press(Key.DirectionRight)
            compose.onNodeWithTag("tracking-status-retry").assertIsFocused()
            fixture.storageFailure = null
            press(Key.Enter, "tracking-status-detail"); runCurrent(); compose.waitForIdle()
            assertNotNull(fixture.runtime.controller.state.value.account)
            compose.onNodeWithTag("tracking-status-detail").assertDoesNotExist()
            compose.onNodeWithTag("settings-gear").assertIsFocused()
            compose.onNodeWithTag("tracking-device-code").assertDoesNotExist()
            assertEquals(0, fixture.sessionWrites)
            assertEquals(0, fixture.revocations)
            assertTrue(fixture.requests.none { "/oauth/" in it.url })
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
