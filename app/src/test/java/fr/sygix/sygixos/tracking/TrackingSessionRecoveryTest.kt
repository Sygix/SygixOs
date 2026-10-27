/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.test.*
import kotlinx.coroutines.withContext
import org.junit.Assert.*
import org.junit.Test

class TrackingSessionRecoveryTest {
    @Test fun retryRecoversIdentityWithTheSavedTokenWithoutReplacingOrRevokingIt() = runTest {
        for (failure in listOf(TrackingFailure.NETWORK, TrackingFailure.CONFIGURATION, TrackingFailure.PROTOCOL)) {
            val fixture = Fixture(this)
            fixture.accountFailure = failure
            fixture.controller.restore(); runCurrent()
            val failed = fixture.controller.state.value
            assertNull(failed.account)
            assertEquals(TrackingSessionStatus.SAVED, failed.sessionStatus)
            assertTrue(failed.hasSession)
            assertTrue(failed.statusVisible)
            assertEquals(failure, failed.failure)
            fixture.accountFailure = null
            fixture.controller.retry(); runCurrent()
            assertEquals(listOf("fixture-token", "fixture-token"), fixture.accountTokens)
            assertNotNull(fixture.controller.state.value.account)
            assertEquals(1, fixture.controller.state.value.contents.size)
            assertFalse(fixture.controller.state.value.statusVisible)
            assertEquals(0, fixture.writes)
            assertEquals(0, fixture.revocations)
            assertEquals(1, fixture.reads)
        }
    }

    @Test fun foregroundRecoversFailedIdentityOnlyAfterTheRetryDelay() = runTest {
        val fixture = Fixture(this)
        fixture.accountFailure = TrackingFailure.NETWORK
        fixture.controller.restore(); runCurrent()
        fixture.accountFailure = null
        fixture.controller.setForeground(true); runCurrent()
        assertEquals(1, fixture.accountTokens.size)
        fixture.controller.setForeground(false)
        advanceTimeBy(60_000)
        fixture.controller.setForeground(true); runCurrent()
        assertNotNull(fixture.controller.state.value.account)
        assertEquals(2, fixture.accountTokens.size)
        assertEquals(0, fixture.writes)
        assertEquals(0, fixture.revocations)
    }

    @Test fun storageRetryKeepsRestorationDistinctFromAnAbsentSession() = runTest {
        val fixture = Fixture(this)
        fixture.storageFailure = true
        fixture.controller.restore(); runCurrent()
        assertEquals(TrackingSessionStatus.RESTORING, fixture.controller.state.value.sessionStatus)
        assertEquals(TrackingFailure.STORAGE, fixture.controller.state.value.failure)
        assertTrue(fixture.controller.state.value.hasSession)
        fixture.storageFailure = false
        fixture.controller.retry(); runCurrent()
        assertEquals(2, fixture.reads)
        assertNotNull(fixture.controller.state.value.account)
        assertEquals(0, fixture.writes)
        assertEquals(0, fixture.revocations)
    }

    @Test fun absentSessionFinishesRestorationAndClearsAnyPreviousStorageError() = runTest {
        val fixture = Fixture(this)
        fixture.storageFailure = true
        fixture.controller.restore(); runCurrent()
        fixture.storageFailure = false
        fixture.saved = null
        fixture.controller.retry(); runCurrent()
        assertEquals(TrackingSessionStatus.NONE, fixture.controller.state.value.sessionStatus)
        assertFalse(fixture.controller.state.value.hasSession)
        assertFalse(fixture.controller.state.value.loading)
        assertNull(fixture.controller.state.value.failure)
        assertTrue(fixture.accountTokens.isEmpty())
    }

    @Test fun explicitTokenRefusalKeepsReconnectionDistinctAndStopsAutomaticCalls() = runTest {
        val fixture = Fixture(this)
        fixture.accountFailure = TrackingFailure.ACCESS
        fixture.controller.restore(); runCurrent()
        assertTrue(fixture.controller.state.value.hasSession)
        fixture.accountFailure = null
        advanceTimeBy(20 * 60_000)
        fixture.controller.setForeground(true)
        fixture.controller.retry(); runCurrent()
        assertEquals(1, fixture.accountTokens.size)
        assertEquals(TrackingFailure.ACCESS, fixture.controller.state.value.failure)
        assertEquals(0, fixture.writes)
        assertEquals(0, fixture.revocations)
    }

    @Test fun disconnectDuringStorageReadCannotRestoreAnOldSession() = runTest {
        val fixture = Fixture(this)
        fixture.readGate = CompletableDeferred()
        fixture.controller.restore(); runCurrent()
        assertTrue(fixture.controller.state.value.loading)
        fixture.controller.disconnect(); runCurrent()
        fixture.readGate!!.complete(Unit); runCurrent()
        assertFalse(fixture.controller.state.value.hasSession)
        assertNull(fixture.controller.state.value.account)
        assertTrue(fixture.accountTokens.isEmpty())
        assertNull(fixture.saved)
    }

    private class Fixture(scope: TestScope) {
        var saved: String? = "fixture-token"
        var storageFailure = false
        var accountFailure: TrackingFailure? = null
        var readGate: CompletableDeferred<Unit>? = null
        var reads = 0
        var writes = 0
        var revocations = 0
        val accountTokens = mutableListOf<String>()
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String): TrackingAccount {
                accountTokens += token
                accountFailure?.let { throw TrackingException(it) }
                return TrackingAccount("fixture", "Fixture")
            }
            override suspend fun snapshot(token: String) = TrackingSnapshot(listOf(TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "1"), title = "Fixture")))
            override suspend fun revoke(token: String) { ++revocations }
        }, object : TrackingSessionStore {
            override suspend fun read(): String? {
                ++reads
                val value = saved
                if (storageFailure) throw TrackingException(TrackingFailure.STORAGE)
                readGate?.let { withContext(NonCancellable) { it.await() } }
                return value
            }
            override suspend fun write(token: String?) { ++writes; saved = token }
        }, scope.backgroundScope, { scope.testScheduler.currentTime })
    }
}
