/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.tracking.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class TrackingOfflineSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun pendingMovieWithoutAnyCardKeepsSyncFocusAndStatusBackRestoresIt() = runTest {
        Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))).use { fixture ->
            show(fixture)
            fixture.runtime.controller.state.first { it.account != null && !it.loading }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            fixture.runtime.actions.perform(fixture.runtime.controller.state.value.contents.single(), TrackingAction.SEEN)
            fixture.runtime.actions.notice.first { it?.locallySaved == true }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            assertTrue(TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).isEmpty())
            compose.runOnIdle { fixture.viewModel.showStatus() }
            compose.onNodeWithTag("tracking-status-retry").assertIsFocused()
            press(Key.DirectionUp, "tracking-status-detail")
            compose.onNodeWithTag("tracking-status-retry").assertIsFocused()
            press(Key.Back, "tracking-status-detail")
            compose.onNodeWithTag("tracking-status-detail").assertDoesNotExist()
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            assertEquals(1, fixture.runtime.controller.state.value.pendingCount)
            fixture.runtime.actions.undo()
            fixture.runtime.controller.state.first { it.pendingCount == 0 }
            assertEquals(fixture.movie.identity, TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single().identity)
        }
    }

    @Test fun accessRefusalKeepsPendingDataAndReconnectionDoesNotDisconnectAccount() = runTest {
        Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler))).use { fixture ->
            show(fixture)
            fixture.runtime.controller.state.first { it.account != null && !it.loading }
            runCurrent(); compose.waitForIdle()
            fixture.runtime.actions.perform(fixture.runtime.controller.state.value.contents.single(), TrackingAction.SEEN)
            fixture.runtime.controller.state.first { it.pendingCount == 1 }
            fixture.repository.failure(fixture.account.incarnation, TrackingFailure.ACCESS)
            fixture.runtime.controller.state.first { it.failure == TrackingFailure.ACCESS }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            press(Key.Enter, "tracking-settings")
            fixture.runtime.device.state.first { it is DeviceAssociationState.Error }
            runCurrent(); compose.waitForIdle()
            assertEquals(1, fixture.repository.pending(fixture.account.incarnation).size)
            assertEquals("A", fixture.repository.account()?.accountId)
            press(Key.Back, "tracking-device")
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
        }
    }

    @Test fun offlineEpisodeAdvancesSharedHeroAndRowWithoutTakingSettingsFocus() = runTest {
        val parent = TrackingIdentity("betaseries", TrackingType.SHOW, "show")
        val episodes = (1..6).map { number -> TrackingContent(TrackingIdentity("betaseries", TrackingType.EPISODE, "episode-$number"),
            parent, "Example", season = 1, episode = number, airDate = "2020-01-01") }
        Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), episodes).use { fixture ->
            show(fixture)
            fixture.runtime.controller.state.first { it.account != null && !it.loading }
            runCurrent(); compose.waitForIdle()
            fixture.runtime.actions.perform(fixture.runtime.controller.state.value.contents.first { it.episode == 1 }, TrackingAction.SEEN)
            fixture.runtime.actions.notice.first { it?.locallySaved == true }
            runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            val candidate = TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single()
            assertEquals(2, candidate.episode)
            assertEquals(candidate.identity, TrackingProjection.hero(candidate).tracking?.identity)
            assertEquals(candidate.identity, TrackingProjection.card(candidate).tracking?.identity)
            assertTrue(fixture.runtime.controller.state.value.contents.filter { it.episode != 1 }.none { it.seen })
            fixture.runtime.actions.undo()
            fixture.runtime.controller.state.first { it.pendingCount == 0 }
            assertEquals(1, TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single().episode)
        }
    }

    private fun press(key: Key, tag: String) {
        compose.onNodeWithTag(tag).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private fun show(fixture: Fixture) {
        compose.setContent {
            val state by fixture.viewModel.state.collectAsState()
            val focus = remember { FocusRequester() }
            MaterialTheme { Box {
                TrackingSettingsContent(state, fixture.viewModel, !state.modal, focus)
                TrackingPanels(state, fixture.viewModel)
            } }
        }
    }

    private class Fixture(private val scope: CoroutineScope, private val seed: List<TrackingContent>? = null) : AutoCloseable {
        val movie = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "1"), title = "Example")
        private val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        private val database = Room.databaseBuilder(context, TrackingDatabase::class.java,
            context.noBackupFilesDir.resolve("${UUID.randomUUID()}.db").absolutePath).build()
        val repository = TrackingRepository(database)
        val account = runBlocking {
            repository.bind("fixture-session", TrackingAccount("A", "Example")).also {
                repository.snapshot(it.incarnation, 0, TrackingSnapshot(seed ?: listOf(movie)))
            }
        }
        private val harness = SettingsHarness()
        private val store = ViewModelStore()
        val runtime = TrackingRuntime(context, harness.repo, scope,
            transport = TrackingTransport { throw TrackingException(TrackingFailure.NETWORK) },
            sessionStore = object : TrackingSessionStore {
                override suspend fun read() = "fixture-session"
                override suspend fun write(token: String?) = Unit
            }, preferences = TrackingPreferencesFixture(), configurationFile = null, identityFile = null,
            repository = repository, scheduler = object : TrackingWorkScheduler {
                override fun enqueue(incarnation: String) = Unit
                override fun cancel(incarnation: String) = Unit
            })
        val viewModel = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackingViewModel(runtime) {} as T
        })[TrackingViewModel::class.java]
        init { runtime.actions.setSettingsOpen(true); runtime.controller.restore() }
        override fun close() { store.clear(); scope.cancel(); database.close(); harness.clear() }
    }
}
