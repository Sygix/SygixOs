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
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.tracking.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "fr-w960dp-h540dp-xhdpi")
class TrackingSettingsTest {
    @get:Rule val compose = createComposeRule()

    @Test fun restoringAndRetryingIdentityKeepsFocusAndNeverStartsAnAssociation() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.savedToken = "fixture-token"
            val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
            fixture.accountGate = gate
            fixture.runtime.controller.restore(); runCurrent()
            show(fixture)
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            compose.onNodeWithTag("tracking-connect").assertDoesNotExist()
            press(Key.Enter)
            assertEquals(1, fixture.accountReads)
            fixture.accountFailure = TrackingFailure.NETWORK
            gate.complete(Unit); runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            compose.onNodeWithTag("tracking-connect").assertDoesNotExist()
            fixture.accountGate = null
            fixture.accountFailure = null
            press(Key.Enter); runCurrent(); compose.waitForIdle()
            assertNotNull(fixture.runtime.controller.state.value.account)
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            compose.onNodeWithTag("tracking-device-code").assertDoesNotExist()
            compose.onNodeWithTag("tracking-device-qr").assertDoesNotExist()
            assertEquals(2, fixture.accountReads)
            assertEquals(1, fixture.sessionReads)
            assertEquals(0, fixture.sessionWrites)
            assertEquals(0, fixture.revocations)
            assertEquals(0, fixture.associations)
        } finally { fixture.close() }
    }

    @Test fun retryAfterTemporaryStorageFailureReadsTheSavedSessionWithoutQr() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.savedToken = "fixture-token"
            fixture.storageFailure = true
            fixture.runtime.controller.restore(); runCurrent()
            show(fixture)
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            compose.onNodeWithTag("tracking-connect").assertDoesNotExist()
            fixture.storageFailure = false
            press(Key.Enter); runCurrent(); compose.waitForIdle()
            assertNotNull(fixture.runtime.controller.state.value.account)
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            assertEquals(2, fixture.sessionReads)
            assertEquals(0, fixture.sessionWrites)
            assertEquals(0, fixture.revocations)
            assertEquals(0, fixture.associations)
        } finally { fixture.close() }
    }

    @Test fun associationShowsOneCompleteAddressAndBackRestoresConnectWithoutPolling() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            show(fixture)
            compose.onNodeWithTag("tracking-connect").assertIsFocused()
            press(Key.Enter)
            compose.onNodeWithTag("tracking-device-code").assertTextEquals("FIXTURE")
            compose.onNodeWithText("https://www.betaseries.com/device").assertExists()
            compose.onNodeWithTag("tracking-device-qr").assertExists()
            press(Key.Back, "tracking-device")
            compose.onNodeWithTag("tracking-connect").assertIsFocused()
            advanceTimeBy(20_000); runCurrent()
            assertEquals(0, fixture.polls)
            assertEquals(DeviceAssociationState.Closed, fixture.runtime.device.state.value)
        } finally { fixture.close() }
    }

    @Test fun countryDropdownCancelsOrCommitsAndReturnsFocusToItsControl() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.preferences.country.value = "FR"
            fixture.runtime.controller.connect("fixture-token")
            runCurrent()
            show(fixture)
            compose.onNodeWithTag("tracking-sync").assertIsFocused()
            press(Key.DirectionDown)
            compose.onNodeWithTag("tracking-country").assertIsFocused()
            press(Key.Enter)
            compose.onNodeWithTag("tracking-country-FR").assertIsFocused()
            press(Key.DirectionRight, "tracking-country-list")
            compose.onNodeWithTag("tracking-country-FR").assertIsFocused()
            press(Key.DirectionDown, "tracking-country-list")
            compose.onNodeWithTag("tracking-country-GA").assertIsFocused()
            press(Key.Back, "tracking-country-list")
            assertEquals("FR", fixture.preferences.country.value)
            compose.onNodeWithTag("tracking-country").assertIsFocused()
            press(Key.Enter)
            press(Key.DirectionDown, "tracking-country-list")
            press(Key.Enter, "tracking-country-list")
            assertEquals("GA", fixture.preferences.country.value)
            compose.onNodeWithTag("tracking-country").assertIsFocused()
            compose.onNodeWithTag("tracking-country-list").assertDoesNotExist()
        } finally { fixture.close() }
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
        compose.waitForIdle()
    }

    @Test fun choosingAnOldSeenEpisodeDoesNotOpenPlayerAndCanMarkOnlyThatEpisodeUnseen() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime }, episodeMode = true)
        try {
            fixture.runtime.controller.connect("fixture-token"); runCurrent()
            val content = TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single()
            fixture.viewModel.menu(TrackingProjection.card(content))
            fixture.viewModel.showEpisodes()
            runCurrent(); show(fixture)
            compose.onNodeWithTag("tracking-episode-2").assertIsFocused()
            press(Key.DirectionUp, "tracking-episodes")
            press(Key.Enter, "tracking-episodes")
            compose.onNodeWithTag("tracking-episode-actions").assertExists()
            assertTrue(fixture.episodeWrites.isEmpty())
            assertNull(fixture.runtime.playback.selection.value)
            press(Key.DirectionDown, "tracking-episode-actions")
            press(Key.Enter, "tracking-episode-actions")
            assertEquals(listOf("1" to false), fixture.episodeWrites)
            assertFalse(fixture.seen.getValue("2"))
        } finally { fixture.close() }
    }

    private fun press(key: Key, tag: String = "tracking-settings") {
        compose.onNodeWithTag(tag).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    @Test fun selectedUnseenEpisodeDoesNotMarkPreviousEpisodesAndReturnRefreshRestoresNeighbor() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime }, episodeMode = true)
        try {
            fixture.runtime.controller.connect("fixture-token"); runCurrent()
            fixture.viewModel.menu(TrackingProjection.card(TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single()))
            fixture.viewModel.showEpisodes(); runCurrent(); show(fixture)
            press(Key.DirectionDown, "tracking-episodes")
            compose.onNodeWithTag("tracking-episode-3").assertIsFocused()
            val reads = fixture.episodeListReads
            fixture.availableEpisodes.remove("3")
            fixture.viewModel.onForeground(); runCurrent(); compose.waitForIdle()
            assertEquals(reads + 1, fixture.episodeListReads)
            compose.onNodeWithTag("tracking-episode-2").assertIsFocused()
            assertTrue(fixture.episodeWrites.isEmpty())
            fixture.availableEpisodes.add("3")
            fixture.viewModel.onForeground(); runCurrent(); compose.waitForIdle()
            press(Key.DirectionDown, "tracking-episodes")
            press(Key.Enter, "tracking-episodes")
            press(Key.DirectionDown, "tracking-episode-actions")
            press(Key.Enter, "tracking-episode-actions")
            assertEquals(listOf("3" to true), fixture.episodeWrites)
            assertTrue(fixture.seen.getValue("1"))
            assertFalse(fixture.seen.getValue("2"))
        } finally { fixture.close() }
    }

    @Test fun delayedEpisodeRefreshKeepsCurrentBackAndSelectionDecisions() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime }, episodeMode = true)
        try {
            fixture.runtime.controller.connect("fixture-token"); runCurrent()
            fixture.viewModel.menu(TrackingProjection.card(TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single()))
            fixture.viewModel.showEpisodes(); runCurrent(); show(fixture)
            press(Key.DirectionUp, "tracking-episodes"); press(Key.Enter, "tracking-episodes")
            val first = kotlinx.coroutines.CompletableDeferred<Unit>()
            fixture.episodeGate = first
            fixture.viewModel.onForeground(); runCurrent(); compose.waitForIdle()
            press(Key.Back, "tracking-episode-actions")
            first.complete(Unit); runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-episode-actions").assertDoesNotExist()
            compose.onNodeWithTag("tracking-episode-1").assertIsFocused()
            val second = kotlinx.coroutines.CompletableDeferred<Unit>()
            fixture.episodeGate = second
            fixture.viewModel.onForeground(); runCurrent(); compose.waitForIdle()
            press(Key.DirectionDown, "tracking-episodes"); press(Key.Enter, "tracking-episodes")
            second.complete(Unit); runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-episode-actions").assertExists()
            assertEquals("2", fixture.viewModel.state.value.episodes?.selected?.id)
            assertTrue(fixture.episodeWrites.isEmpty())
        } finally { fixture.close() }
    }

    @Test fun markingAnEpisodeCancelsPendingRefreshAndNeverReopensTheList() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime }, episodeMode = true)
        try {
            fixture.runtime.controller.connect("fixture-token"); runCurrent()
            fixture.viewModel.menu(TrackingProjection.card(TrackingProjection.candidates(fixture.runtime.controller.state.value.contents).single()))
            fixture.viewModel.showEpisodes(); runCurrent(); show(fixture)
            press(Key.DirectionUp, "tracking-episodes"); press(Key.Enter, "tracking-episodes")
            val pending = kotlinx.coroutines.CompletableDeferred<Unit>()
            fixture.episodeGate = pending
            fixture.viewModel.onForeground(); runCurrent(); compose.waitForIdle()
            press(Key.DirectionDown, "tracking-episode-actions"); press(Key.Enter, "tracking-episode-actions")
            pending.complete(Unit); runCurrent(); compose.waitForIdle()
            compose.onNodeWithTag("tracking-episodes").assertDoesNotExist()
            compose.onNodeWithTag("tracking-episode-actions").assertDoesNotExist()
            assertNull(fixture.viewModel.state.value.episodes)
            assertEquals(listOf("1" to false), fixture.episodeWrites)
        } finally { fixture.close() }
    }

    private class Fixture(scope: CoroutineScope, clock: () -> Long, private val episodeMode: Boolean = false) : AutoCloseable {
        private val harness = SettingsHarness()
        private val store = ViewModelStore()
        val preferences = TrackingPreferencesFixture()
        var polls = 0
        var savedToken: String? = null
        var storageFailure = false
        var accountFailure: TrackingFailure? = null
        var accountGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        var sessionReads = 0
        var sessionWrites = 0
        var accountReads = 0
        var revocations = 0
        var associations = 0
        val seen = mutableMapOf("1" to true, "2" to false, "3" to false)
        val episodeWrites = mutableListOf<Pair<String, Boolean>>()
        val availableEpisodes = linkedSetOf("1", "2", "3")
        var episodeListReads = 0
        var episodeGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null
        private val show = "{\"id\":1,\"title\":\"Fixture\",\"user\":{\"archived\":false}}"
        private fun episode(id: String) = "{\"id\":$id,\"season\":1,\"episode\":$id,\"title\":\"Fixture episode $id\",\"date\":\"2020-01-01\",\"user\":{\"seen\":${seen.getValue(id)}}}"
        val runtime = TrackingRuntime(ApplicationProvider.getApplicationContext(), harness.repo, scope,
            transport = TrackingTransport { request ->
                when {
                    request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                    request.url.endsWith("oauth/device") -> { ++associations; TrackingResponse(200, "{\"device_code\":\"fixture-device\",\"user_code\":\"FIXTURE\",\"verification_url\":\"https://www.betaseries.com/device\",\"expires_in\":100,\"interval\":1}") }
                    request.url.endsWith("oauth/access_token") -> { ++polls; TrackingResponse(400, "{\"errors\":[{\"code\":2001}]}") }
                    request.url.endsWith("members/infos") -> { ++accountReads; accountGate?.await(); accountFailure?.let { throw TrackingException(it) }; TrackingResponse(200, "{\"member\":{\"id\":1,\"login\":\"Fixture\"}}") }
                    request.url.endsWith("members/destroy") -> { ++revocations; TrackingResponse(200, "{}") }
                    request.url.contains("shows/member") -> TrackingResponse(200, if (episodeMode) "{\"shows\":[$show],\"total\":1,\"totalMissingShows\":0}" else "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}")
                    request.url.contains("episodes/list") -> TrackingResponse(200, if (episodeMode) "{\"shows\":[{\"id\":1,\"unseen\":[${seen.filterValues { !it }.keys.joinToString(",") { episode(it) }}]}],\"total\":1,\"totalMissingShows\":0}" else "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}")
                    request.url.contains("shows/display") -> TrackingResponse(200, "{\"show\":$show}")
                    request.url.contains("shows/episodes") -> { ++episodeListReads; episodeGate?.await(); TrackingResponse(200, "{\"episodes\":[${availableEpisodes.joinToString(",") { episode(it) }}],\"total\":${availableEpisodes.size},\"totalMissingEpisodes\":0}") }
                    request.url.contains("episodes/watched") -> {
                        val id = request.fields["id"] ?: java.net.URI(request.url).rawQuery.split('&').first { it.startsWith("id=") }.substringAfter('=')
                        if (request.method == "POST") check(request.fields["bulk"] == "false")
                        seen[id] = request.method == "POST"
                        episodeWrites += id to seen.getValue(id)
                        TrackingResponse(200, "{}")
                    }
                    request.url.contains("episodes/display") -> {
                        val id = java.net.URI(request.url).rawQuery.split('&').first { it.startsWith("id=") }.substringAfter('=')
                        TrackingResponse(200, "{\"episode\":${episode(id)}}")
                    }
                    request.url.contains("movies/member") -> TrackingResponse(200, "{\"movies\":[],\"total\":0,\"totalMissingMovies\":0}")
                    else -> error("Unexpected fixture endpoint")
                }
            }, sessionStore = object : TrackingSessionStore {
                override suspend fun read(): String? { ++sessionReads; if (storageFailure) throw TrackingException(TrackingFailure.STORAGE); return savedToken }
                override suspend fun write(token: String?) { ++sessionWrites; savedToken = token }
            }, preferences = preferences, clock = clock, nanoTime = { clock() * 1_000_000 }, wallClock = { 1_800_000_000_000L },
            parserDispatcher = scope.coroutineContext[kotlin.coroutines.ContinuationInterceptor] as kotlinx.coroutines.CoroutineDispatcher, configurationFile = null)
        val viewModel = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackingViewModel(runtime) {} as T
        })[TrackingViewModel::class.java]
        override fun close() { store.clear(); harness.clear() }
    }
}
