/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.ui.settings.SettingsHarness
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.advanceTimeBy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingRuntimeProviderTest {
    @Test fun aNewTraktAssociationValidatesTheCandidateBeforePersistingItsFirstSession() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.preferences.selectProvider("trakt")
            fixture.runtime.device.open(); runCurrent()
            assertTrue(fixture.runtime.device.state.value is DeviceAssociationState.Waiting)
            assertNull(fixture.saved)
            advanceTimeBy(5_000); runCurrent()
            assertEquals(DeviceAssociationState.Connected, fixture.runtime.device.state.value)
            assertEquals("trakt:fixture-account", fixture.runtime.controller.state.value.account?.id)
            assertEquals("trakt", TraktSessionEnvelope.provider(fixture.saved))
            assertEquals(1, fixture.writes)
            assertEquals(1, fixture.requests.count { it.url == "https://auth.trakt.tv/oauth/device/token" })
        } finally { fixture.close() }
    }
    @Test fun selectingAProviderWithARetainedCountryDoesNotDiscoverOrAssociate() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.preferences.confirmCountry("FR")
            fixture.preferences.selectProvider("trakt")
            fixture.runtime.controller.restore(); runCurrent()
            assertEquals("trakt", fixture.runtime.selectedProvider.value)
            assertFalse(fixture.runtime.controller.state.value.hasSession)
            assertTrue(fixture.requests.isEmpty())
        } finally { fixture.close() }
    }

    @Test fun aSavedTraktSessionOverridesTheChoiceWithoutRediscoveringItsClient() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.saved = TraktSessionEnvelope.create("a".repeat(64), "fixture-access", "fixture-refresh", fixture.now() + 604_800_000).encode()
            fixture.runtime.controller.restore(); runCurrent()
            assertEquals("trakt", fixture.runtime.selectedProvider.value)
            assertEquals("trakt:fixture-account", fixture.runtime.controller.state.value.account?.id)
            assertFalse(fixture.runtime.controller.state.value.loading)
            assertNull(fixture.runtime.controller.state.value.failure)
            assertTrue(fixture.requests.isNotEmpty())
            assertTrue(fixture.requests.all { it.url.startsWith("https://api.trakt.tv/") })
            assertTrue(fixture.requests.all { it.headers["Authorization"] == "Bearer fixture-access" })
            assertEquals(0, fixture.writes)
        } finally { fixture.close() }
    }

    @Test fun aLegacyBetaSeriesSessionOverridesAStoredTraktChoiceWithoutReassociation() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.preferences.selectProvider("trakt")
            fixture.saved = "fixture-legacy-beta"
            fixture.runtime.controller.restore(); runCurrent()
            assertEquals("betaseries", fixture.runtime.selectedProvider.value)
            assertEquals("fixture-beta", fixture.runtime.controller.state.value.account?.id)
            assertFalse(fixture.requests.any { "oauth/" in it.url || "trakt.tv" in it.url })
            assertEquals(0, fixture.writes)
        } finally { fixture.close() }
    }

    @Test fun unsupportedTraktEnvelopeDoesNotGetSentAsABetaSeriesCredential() = runTest {
        val fixture = Fixture(CoroutineScope(backgroundScope.coroutineContext + UnconfinedTestDispatcher(testScheduler)), { testScheduler.currentTime })
        try {
            fixture.saved = "trakt:v9:fixture-unsupported"
            fixture.runtime.controller.restore(); runCurrent()
            assertEquals(TrackingFailure.STORAGE, fixture.runtime.controller.state.value.failure)
            assertTrue(fixture.runtime.controller.state.value.hasSession)
            assertTrue(fixture.requests.isEmpty())
            assertEquals("trakt:v9:fixture-unsupported", fixture.saved)
        } finally { fixture.close() }
    }

    private class Fixture(scope: CoroutineScope, private val clock: () -> Long) : AutoCloseable {
        private val harness = SettingsHarness()
        val preferences = TrackingPreferencesFixture()
        val requests = mutableListOf<TrackingRequest>()
        var saved: String? = null
        var writes = 0
        fun now() = 1_800_000_000_000L + clock()
        val runtime = TrackingRuntime(ApplicationProvider.getApplicationContext(), harness.repo, scope,
            transport = TrackingTransport { request ->
                requests += request
                TrackingResponse(200, when {
                    request.url == "https://app.trakt.tv/" -> "import('/entry/app.fixture.js')"
                    request.url == "https://app.trakt.tv/entry/app.fixture.js" -> "['../nodes/0.fixture.js']"
                    request.url == "https://app.trakt.tv/nodes/0.fixture.js" -> "?trakt-api-key=${"a".repeat(64)}"
                    request.url == "https://auth.trakt.tv/oauth/device/code" -> """{"device_code":"fixture-device","user_code":"FIXTURE","verification_url":"https://auth.trakt.tv/activate","expires_in":600,"interval":1}"""
                    request.url == "https://auth.trakt.tv/oauth/device/token" -> """{"access_token":"fixture-access","refresh_token":"fixture-refresh","expires_in":604800}"""
                    request.url == "https://auth.trakt.tv/oauth/revoke" -> "{}"
                    request.url == "https://www.betaseries.com/" -> "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}"
                    "members/infos" in request.url -> """{"member":{"id":"fixture-beta","login":"Fixture"}}"""
                    "shows/member" in request.url || "episodes/list" in request.url -> """{"shows":[],"total":0,"totalMissingShows":0}"""
                    "movies/member" in request.url -> """{"movies":[],"total":0,"totalMissingMovies":0}"""
                    request.url == "https://api.trakt.tv/users/settings" -> """{"user":{"ids":{"uuid":"fixture-account"},"username":"Fixture"}}"""
                    request.url.startsWith("https://api.trakt.tv/sync/") || request.url.startsWith("https://api.trakt.tv/users/hidden/") -> "[]"
                    else -> error("Unexpected provider fixture endpoint")
                })
            }, sessionStore = object : TrackingSessionStore {
                override suspend fun read() = saved
                override suspend fun write(token: String?) { writes++; saved = token }
            }, preferences = preferences, clock = clock, wallClock = ::now, nanoTime = { clock() * 1_000_000 },
            parserDispatcher = scope.coroutineContext[kotlin.coroutines.ContinuationInterceptor] as kotlinx.coroutines.CoroutineDispatcher,
            configurationFile = null, identityFile = null)
        override fun close() { harness.clear() }
    }
}
