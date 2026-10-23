/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.*
import org.junit.Test

class TrackingSessionTest {
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
}
