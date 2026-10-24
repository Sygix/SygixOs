/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

class TrackingSessionTest {
    @Test fun cancelledAssociationValidatesWithoutReplacingExistingSessionAndRevokesUnusedToken() = runTest {
        val validation = CompletableDeferred<Unit>()
        val revoked = mutableListOf<String>()
        var saved: String? = "current"
        var visible = true
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String): TrackingAccount { if (token == "candidate") validation.await(); return TrackingAccount(token, "Fixture") }
            override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList())
            override suspend fun revoke(token: String) { revoked += token }
        }, object : TrackingSessionStore {
            override suspend fun read() = saved
            override suspend fun write(token: String?) { saved = token }
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        val caller = backgroundScope.launch { controller.connect("candidate") { visible } }
        runCurrent()
        assertEquals("current", controller.state.value.account?.id)
        visible = false; caller.cancel(); validation.complete(Unit); runCurrent()
        assertEquals("current", saved)
        assertEquals("current", controller.state.value.account?.id)
        assertFalse(controller.state.value.loading)
        assertEquals(listOf("candidate"), revoked)
    }

    @Test fun offlineRevocationIsNotAnAccountFailure() = runTest {
        var saved: String? = "fixture-token"
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String) = TrackingSnapshot(emptyList())
            override suspend fun revoke(token: String) { throw TrackingException(TrackingFailure.NETWORK) }
        }, object : TrackingSessionStore {
            override suspend fun read() = saved
            override suspend fun write(token: String?) { saved = token }
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent(); controller.disconnect(); runCurrent()
        assertNull(saved)
        assertNull(controller.state.value.account)
        assertNull(controller.state.value.failure)
        assertTrue(controller.state.value.revocationUnconfirmed)
    }

    @Test fun disconnectedAccountCannotPublishLateData() = runTest {
        val pending = CompletableDeferred<TrackingSnapshot>()
        val provider = object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount(token, "Test")
            override suspend fun snapshot(token: String) = kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { pending.await() }
            override suspend fun revoke(token: String) {}
        }
        val store = object : TrackingSessionStore {
            var value: String? = "first"
            override suspend fun read() = value
            override suspend fun write(token: String?) { value = token }
        }
        val controller = TrackingController(provider, store, backgroundScope)
        controller.restore()
        runCurrent()
        controller.disconnect()
        runCurrent()
        pending.complete(TrackingSnapshot(emptyList()))
        runCurrent()
        assertNull(controller.state.value.account)
        assertTrue(controller.state.value.contents.isEmpty())
        assertNull(store.value)
    }

    @Test fun identitiesKeepProviderAndContentTypesSeparate() {
        assertNotEquals(TrackingIdentity("one", TrackingType.MOVIE, "42"), TrackingIdentity("two", TrackingType.MOVIE, "42"))
        assertNotEquals(TrackingIdentity("one", TrackingType.MOVIE, "42"), TrackingIdentity("one", TrackingType.SHOW, "42"))
    }

    @Test fun secondAccountCannotBeReplacedByNonCancellableFirstAccountResponse() = runTest {
        val first = CompletableDeferred<TrackingSnapshot>()
        val secondContent = TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "2"), title = "Second fixture")
        val provider = object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount(token, "Fixture")
            override suspend fun snapshot(token: String) = if (token == "first") kotlinx.coroutines.withContext(kotlinx.coroutines.NonCancellable) { first.await() } else TrackingSnapshot(listOf(secondContent))
            override suspend fun revoke(token: String) {}
        }
        var saved: String? = "first"
        val controller = TrackingController(provider, object : TrackingSessionStore {
            override suspend fun read() = saved
            override suspend fun write(token: String?) { saved = token }
        }, backgroundScope, { testScheduler.currentTime })
        controller.restore(); runCurrent()
        controller.connect("second"); runCurrent()
        assertEquals("second", controller.state.value.account?.id)
        first.complete(TrackingSnapshot(emptyList())); runCurrent()
        assertEquals("second", saved)
        assertEquals("second", controller.state.value.account?.id)
        assertEquals(listOf(secondContent.copy(sessionGeneration = controller.state.value.generation, validUntil = 900_000)), controller.state.value.contents)
    }

    @Test fun knownAccessRefusalWithdrawsSnapshotAndStopsFurtherCallsUntilReassociation() = runTest {
        var snapshots = 0
        val content = TrackingContent(TrackingIdentity("fixture", TrackingType.MOVIE, "1"), title = "Fixture")
        val controller = TrackingController(object : WatchTrackingProvider {
            override suspend fun account(token: String) = TrackingAccount("fixture", "Fixture")
            override suspend fun snapshot(token: String): TrackingSnapshot { ++snapshots; return TrackingSnapshot(listOf(content)) }
            override suspend fun revoke(token: String) {}
        }, object : TrackingSessionStore {
            override suspend fun read() = "fixture-token"
            override suspend fun write(token: String?) {}
        }, backgroundScope)
        controller.restore(); runCurrent()
        try { controller.authenticated<Unit> { throw TrackingException(TrackingFailure.ACCESS) }; fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.ACCESS, error.failure) }
        assertEquals(TrackingFailure.ACCESS, controller.state.value.failure)
        assertTrue(controller.state.value.contents.isEmpty())
        val before = snapshots
        controller.refresh(); runCurrent()
        assertEquals(before, snapshots)
        controller.connect("fixture-new-token"); runCurrent()
        assertNull(controller.state.value.failure)
        assertFalse(controller.state.value.contents.isEmpty())
    }
}
