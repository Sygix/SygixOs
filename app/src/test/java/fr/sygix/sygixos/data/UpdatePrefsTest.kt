/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.CheckResult
import fr.sygix.sygixos.domain.KnownUpdates
import fr.sygix.sygixos.domain.UpdateError
import fr.sygix.sygixos.domain.candidate
import fr.sygix.sygixos.data.FakeTransport.Reply
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class UpdatePrefsTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = UpdatePrefs(context)

    @Before
    fun reset() = runBlocking {
        context.dataStore.edit { it.clear() }
        Unit
    }

    @Test
    fun `defaults are prereleases off and never checked`() = runBlocking {
        assertEquals(UpdatePersisted(), prefs.data.first())
        assertFalse(prefs.data.first().includePrereleases)
        assertFalse(prefs.takeRelaunch())
    }

    @Test
    fun `every value survives a new instance`() = runBlocking {
        val known = KnownUpdates(bestFinal = candidate("v0.0.2"), bestAny = candidate("v0.0.3-rc.1"))
        prefs.setIncludePrereleases(true)
        prefs.setLastCheckAt(1234L)
        prefs.setRetryAt(5678L)
        prefs.setLastResult(CheckResult.Error(UpdateError.RateLimited(5678L)))
        prefs.setKnown(known)
        val restarted = UpdatePrefs(context).data.first()
        assertEquals(
            UpdatePersisted(
                includePrereleases = true,
                lastCheckAt = 1234L,
                lastResult = CheckResult.Error(UpdateError.RateLimited(5678L)),
                retryAt = 5678L,
                known = known,
            ),
            restarted,
        )
        assertEquals("https://github.com/Sygix/SygixOs/releases/tag/v0.0.3-rc.1", restarted.known.bestAny?.htmlUrl)
    }

    @Test
    fun `every check result round trips`() = runBlocking {
        listOf(
            CheckResult.Ok,
            CheckResult.Error(UpdateError.NoNetwork),
            CheckResult.Error(UpdateError.Timeout),
            CheckResult.Error(UpdateError.Unavailable(404)),
            CheckResult.Error(UpdateError.Unreadable),
        ).forEach { result ->
            prefs.setLastResult(result)
            assertEquals(result, UpdatePrefs(context).data.first().lastResult)
        }
    }

    @Test
    fun `relaunch flag is read once then cleared`() = runBlocking {
        prefs.setRelaunch(true)
        assertTrue(UpdatePrefs(context).takeRelaunch())
        assertFalse(UpdatePrefs(context).takeRelaunch())
        prefs.setRelaunch(false)
        assertFalse(prefs.takeRelaunch())
    }

    @Test
    fun `no request is sent before the persisted retry time, even by a new repository instance`() = runTest {
        val first = UpdateHarness(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)), StandardTestDispatcher(testScheduler), folder.root, UpdatePrefs(context))
        val reset = (first.now + UpdateHarness.HOUR) / 1000
        first.transport.on(UpdateHarness.LIST_URL, Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "$reset")))
        first.repository.check()
        val persisted = withContext(Dispatchers.Default) { withTimeout(20_000) { prefs.data.first { it.retryAt != null && it.lastResult != null } } }
        assertEquals(reset * 1000, persisted.retryAt)
        assertEquals(1, first.listCalls())

        val restarted = UpdateHarness(CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler)), StandardTestDispatcher(testScheduler), folder.root, UpdatePrefs(context))
        restarted.now = first.now + UpdateHarness.HOUR / 2
        restarted.publish("v0.0.2")
        val finished = async { restarted.repository.status.first { it.checking }; restarted.repository.status.first { !it.checking } }
        testScheduler.runCurrent()
        restarted.repository.check()
        val status = withContext(Dispatchers.Default) { withTimeout(20_000) { finished.await() } }
        assertEquals(0, restarted.listCalls())
        assertEquals(CheckResult.Error(UpdateError.RateLimited(reset * 1000)), status.lastResult)
    }

    @Test
    fun `unreadable known updates fall back to none`() {
        assertEquals(KnownUpdates.None, UpdatePrefs.decodeKnown("not json"))
    }
}
