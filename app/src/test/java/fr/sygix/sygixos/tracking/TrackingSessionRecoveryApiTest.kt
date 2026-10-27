/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingSessionRecoveryApiTest {
    @Test fun genericRefusalsKeepLoadedContentsAndAllowRecoveryWithoutReassociation() = runTest {
        for (status in listOf(401, 403)) for (body in listOf("{}", "<html>Fixture unavailable</html>")) {
            val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
            fixture.runtime.controller.restore(); runCurrent()
            val original = fixture.runtime.controller.state.value.contents
            assertEquals(1, original.size)
            fixture.movieListResponse = TrackingResponse(status, body)
            fixture.runtime.controller.refresh(); runCurrent()
            assertEquals(original, fixture.runtime.controller.state.value.contents)
            assertEquals(TrackingFailure.INCOMPLETE, fixture.runtime.controller.state.value.failure)
            fixture.movieListResponse = null
            fixture.runtime.controller.retry(); runCurrent()
            assertNull(fixture.runtime.controller.state.value.failure)
            assertEquals(original, fixture.runtime.controller.state.value.contents)
            assertEquals(0, fixture.sessionWrites)
            assertEquals(0, fixture.revocations)
            assertTrue(fixture.requests.filter { it.url.contains("/movies/member") }.all { it.headers["X-BetaSeries-Token"] == "fixture-token" })
            assertTrue(fixture.requests.none { "/oauth/" in it.url })
        }
    }

    @Test fun explicitTokenRefusalWithdrawsContentsAndStopsFurtherAccountReads() = runTest {
        val fixture = TrackingMovieFixture(backgroundScope) { testScheduler.currentTime }
        fixture.runtime.controller.restore(); runCurrent()
        assertEquals(1, fixture.runtime.controller.state.value.contents.size)
        fixture.movieListResponse = TrackingResponse(403, "{\"errors\":[{\"code\":1002}]}")
        fixture.runtime.controller.refresh(); runCurrent()
        assertEquals(TrackingFailure.ACCESS, fixture.runtime.controller.state.value.failure)
        assertTrue(fixture.runtime.controller.state.value.contents.isEmpty())
        val before = fixture.requests.size
        fixture.runtime.controller.retry(); runCurrent()
        assertEquals(before, fixture.requests.size)
        assertEquals(0, fixture.sessionWrites)
        assertEquals(0, fixture.revocations)
    }
}
