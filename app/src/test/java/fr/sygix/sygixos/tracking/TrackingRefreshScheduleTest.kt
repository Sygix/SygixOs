/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.testDataStore
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.test.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingRefreshScheduleTest {
    private val interval = 15 * 60_000L

    @Test fun accountRefreshIsPeriodicOnlyWhileForegroundAndResumeHasOneTimer() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.runtime.controller.restore(); runCurrent()
        fixture.runtime.onForeground(); runCurrent()
        val initial = fixture.accounts
        advanceTimeBy(interval - 1); runCurrent()
        assertEquals(initial, fixture.accounts)
        advanceTimeBy(1); runCurrent()
        assertEquals(initial + 1, fixture.accounts)
        fixture.runtime.onBackground()
        advanceTimeBy(interval * 2); runCurrent()
        assertEquals(initial + 1, fixture.accounts)
        fixture.runtime.onForeground(); runCurrent()
        assertEquals(initial + 2, fixture.accounts)
        fixture.runtime.onForeground(); runCurrent()
        assertEquals(initial + 2, fixture.accounts)
        advanceTimeBy(interval); runCurrent()
        assertEquals(initial + 3, fixture.accounts)
    }

    @Test fun periodicTickDoesNotDuplicateAnAlreadyRunningSharedRead() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.runtime.controller.restore(); runCurrent()
        fixture.runtime.onForeground(); runCurrent()
        advanceTimeBy(interval - 1000); runCurrent()
        val gate = CompletableDeferred<Unit>()
        fixture.gate = gate
        fixture.runtime.controller.refresh(); runCurrent()
        val current = fixture.accounts
        fixture.runtime.onForeground(); runCurrent()
        advanceTimeBy(1000); runCurrent()
        assertEquals(current, fixture.accounts)
        assertEquals(1, fixture.maximumActive)
        gate.complete(Unit); runCurrent()
        advanceTimeBy(interval); runCurrent()
        assertEquals(current + 1, fixture.accounts)
        assertEquals(1, fixture.maximumActive)
    }

    @Test fun initialRestoreFailureStillRetriesWhenForegroundPrecedesTokenLoading() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.failSnapshot = true
        fixture.runtime.onForeground()
        fixture.runtime.controller.restore(); runCurrent()
        assertEquals(TrackingFailure.NETWORK, fixture.runtime.controller.state.value.failure)
        val initial = fixture.accounts
        fixture.failSnapshot = false
        advanceTimeBy(interval); runCurrent()
        assertEquals(initial + 1, fixture.accounts)
        assertNull(fixture.runtime.controller.state.value.failure)
    }

    @Test fun initialConnectionSnapshotFailureStillRetriesWithoutAnotherLifecycleEvent() = runTest {
        val fixture = Fixture(backgroundScope)
        fixture.failSnapshot = true
        fixture.runtime.onForeground()
        fixture.runtime.controller.connect("fixture-token"); runCurrent()
        assertEquals(TrackingFailure.NETWORK, fixture.runtime.controller.state.value.failure)
        val initial = fixture.accounts
        fixture.failSnapshot = false
        advanceTimeBy(interval); runCurrent()
        assertEquals(initial + 1, fixture.accounts)
        assertNull(fixture.runtime.controller.state.value.failure)
    }

    private class Fixture(scope: CoroutineScope) {
        private val context: android.content.Context = ApplicationProvider.getApplicationContext()
        var accounts = 0
        var gate: CompletableDeferred<Unit>? = null
        private var active = 0
        var maximumActive = 0
        var failSnapshot = false
        val runtime = TrackingRuntime(context, AppCatalogRepository(InstalledAppsSource(context), LauncherPrefs(testDataStore(scope))), scope,
            transport = TrackingTransport { request ->
                ++active; maximumActive = maxOf(maximumActive, active)
                try {
                    when {
                        request.url.endsWith("/") -> TrackingResponse(200, "{apiHost:\"https://api.betaseries.com\",apiKey:\"fixture-client\"}")
                        request.url.contains("members/infos") -> { ++accounts; TrackingResponse(200, "{\"member\":{\"id\":1,\"login\":\"Fixture\"}}") }
                        request.url.contains("shows/member") -> { if (failSnapshot) throw TrackingException(TrackingFailure.NETWORK); gate?.await(); TrackingResponse(200, "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}") }
                        request.url.contains("episodes/list") -> TrackingResponse(200, "{\"shows\":[],\"total\":0,\"totalMissingShows\":0}")
                        request.url.contains("movies/member") -> TrackingResponse(200, "{\"movies\":[],\"total\":0,\"totalMissingMovies\":0}")
                        else -> error("Unexpected refresh fixture endpoint")
                    }
                } finally { --active }
            }, sessionStore = object : TrackingSessionStore {
                override suspend fun read() = "fixture-token"
                override suspend fun write(token: String?) {}
            }, preferences = TrackingPreferencesFixture())
    }
}
