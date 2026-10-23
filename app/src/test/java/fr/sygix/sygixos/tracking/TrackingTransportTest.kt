/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

class TrackingTransportTest {
    @Test fun serverFailureIsRetriedForReadsOnly() = runTest {
        var calls = 0
        val transport = BoundedTrackingTransport { ++calls; TrackingResponse(if (calls < 3) 502 else 200, "{}") }
        assertEquals(200, transport.execute(TrackingRequest("https://api.betaseries.com/shows/member")).status)
        assertEquals(3, calls)
        calls = 0
        assertEquals(502, transport.execute(TrackingRequest("https://api.betaseries.com/shows/show", "POST")).status)
        assertEquals(1, calls)
    }

    @Test fun lostMutationResponseIsNotReplayed() = runTest {
        var calls = 0
        val transport = BoundedTrackingTransport { ++calls; throw TrackingException(TrackingFailure.NETWORK) }
        try { transport.execute(TrackingRequest("https://api.betaseries.com/shows/show", "DELETE")); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        assertEquals(1, calls)
    }

    @Test fun retryAfterAndRetryBudgetAreRespected() = runTest {
        val start = testScheduler.currentTime
        var calls = 0
        val transport = BoundedTrackingTransport { ++calls; TrackingResponse(429, "{}", 2000) }
        assertEquals(429, transport.execute(TrackingRequest("https://api.betaseries.com/shows/member")).status)
        assertEquals(3, calls)
        assertEquals(4000, testScheduler.currentTime - start)
    }

    @Test fun accessErrorsAreNotRetried() = runTest {
        var calls = 0
        val transport = BoundedTrackingTransport { ++calls; TrackingResponse(403, "{}") }
        assertEquals(403, transport.execute(TrackingRequest("https://api.betaseries.com/shows/member")).status)
        assertEquals(1, calls)
    }

    @Test fun serverDelayBeyondCallBudgetFailsWithoutRetryingEarly() = runTest {
        var calls = 0
        val transport = BoundedTrackingTransport { ++calls; TrackingResponse(429, "{}", 20_000) }
        try { transport.execute(TrackingRequest("https://api.betaseries.com/shows/member")); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.NETWORK, error.failure) }
        assertEquals(1, calls)
        assertEquals(15_000L, testScheduler.currentTime)
    }

    @Test fun hostValidationRejectsCredentialsAndLookalikes() {
        assertFalse(officialTrackingUrl("https://www.betaseries.com.example.org/"))
        assertFalse(officialTrackingUrl("http://www.betaseries.com/"))
        assertFalse(officialTrackingUrl("https://user@api.betaseries.com/"))
        assertFalse(officialTrackingUrl("https://api.betaseries.com:444/"))
        assertTrue(officialTrackingUrl("https://www.betaseries.com/device"))
    }

    @Test fun serviceUnavailableReadsAlsoHaveALimitedRetryCount() = runTest {
        var calls = 0
        val transport = BoundedTrackingTransport { ++calls; TrackingResponse(503, "{}") }
        assertEquals(503, transport.execute(TrackingRequest("https://api.betaseries.com/shows/member")).status)
        assertEquals(3, calls)
    }
}
