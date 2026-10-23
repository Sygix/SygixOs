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
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class TrackingSettingsTest {
    @get:Rule val compose = createComposeRule()

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

    private fun press(key: Key, tag: String = "tracking-settings") {
        compose.onNodeWithTag(tag).performKeyInput { keyDown(key); keyUp(key) }
        compose.waitForIdle()
    }

    private class Preferences : TrackingSettingsStore {
        override val country = MutableStateFlow<String?>(null)
        override val observation = MutableStateFlow(false)
        override val players = MutableStateFlow<Map<String, String>>(emptyMap())
        override suspend fun confirmCountry(country: String) { this.country.value = country }
        override suspend fun observation(enabled: Boolean) { observation.value = enabled }
        override suspend fun preferred(content: TrackingIdentity) = players.value[TrackingPreferences.playerKey(content)]
        override suspend fun remember(content: TrackingIdentity, packageName: String) { players.value += TrackingPreferences.playerKey(content) to packageName }
    }

    private class Fixture(scope: CoroutineScope, clock: () -> Long) : AutoCloseable {
        private val harness = SettingsHarness()
        private val store = ViewModelStore()
        val preferences = Preferences()
        var polls = 0
        val runtime = TrackingRuntime(ApplicationProvider.getApplicationContext(), harness.repo, scope,
            transport = TrackingTransport { request ->
                when {
                    request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                    request.url.endsWith("oauth/device") -> TrackingResponse(200, "{\"device_code\":\"fixture-device\",\"user_code\":\"FIXTURE\",\"verification_url\":\"https://www.betaseries.com/device\",\"expires_in\":100,\"interval\":1}")
                    request.url.endsWith("oauth/access_token") -> { ++polls; TrackingResponse(400, "{\"errors\":[{\"code\":2001}]}") }
                    request.url.endsWith("members/infos") -> TrackingResponse(200, "{\"member\":{\"id\":1,\"login\":\"Fixture\"}}")
                    request.url.contains("shows/member") || request.url.contains("episodes/list") -> TrackingResponse(200, "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}")
                    request.url.contains("movies/member") -> TrackingResponse(200, "{\"movies\":[],\"total\":0,\"totalMissingMovies\":0}")
                    else -> error("Unexpected fixture endpoint")
                }
            }, sessionStore = object : TrackingSessionStore {
                override suspend fun read(): String? = null
                override suspend fun write(token: String?) {}
            }, preferences = preferences, clock = clock, nanoTime = { clock() * 1_000_000 })
        val viewModel = ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = TrackingViewModel(runtime) {} as T
        })[TrackingViewModel::class.java]
        override fun close() { store.clear(); harness.clear() }
    }
}
