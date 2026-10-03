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
import kotlinx.coroutines.Job
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
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
        assertNull(prefs.takeRelaunch())
    }

    @Test
    fun `every value survives a new instance`() = runBlocking {
        val known = KnownUpdates(bestFinal = candidate("v0.0.2"), bestAny = candidate("v0.0.3-rc.1"))
        prefs.togglePrereleases()
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
            CheckResult.Error(UpdateError.SecureConnection),
            CheckResult.Error(UpdateError.Unavailable(404)),
            CheckResult.Error(UpdateError.Unreadable),
        ).forEach { result ->
            prefs.setLastResult(result)
            assertEquals(result, UpdatePrefs(context).data.first().lastResult)
        }
    }

    @Test
    fun `relaunch target is read once then cleared`() = runBlocking {
        prefs.setRelaunch(299L)
        assertEquals(299L, UpdatePrefs(context).takeRelaunch())
        assertNull(UpdatePrefs(context).takeRelaunch())
        prefs.setRelaunch(299L)
        prefs.setRelaunch(null)
        assertNull(prefs.takeRelaunch())
    }

    @Test
    fun `double toggle of prereleases loses nothing`() = runBlocking {
        coroutineScope {
            launch(Dispatchers.Default) { UpdatePrefs(context).togglePrereleases() }
            launch(Dispatchers.Default) { UpdatePrefs(context).togglePrereleases() }
        }
        assertFalse(prefs.data.first().includePrereleases)
        prefs.togglePrereleases()
        assertTrue(prefs.data.first().includePrereleases)
    }

    private suspend fun Job.joinAllChildren() {
        while (children.any { it.isActive }) children.toList().joinAll()
    }

    @Test
    fun `automatic check sends nothing before the persisted retry time, even from a new repository instance`() = runBlocking {
        val firstJob = SupervisorJob()
        val first = UpdateHarness(CoroutineScope(firstJob + Dispatchers.Default), Dispatchers.IO, folder.root, UpdatePrefs(context))
        val reset = (first.now + UpdateHarness.HOUR) / 1000
        first.transport.on(UpdateHarness.LIST_URL, Reply.Body(status = 403, headers = mapOf("x-ratelimit-remaining" to "0", "x-ratelimit-reset" to "$reset")))
        first.repository.check()
        firstJob.joinAllChildren()
        val persisted = prefs.data.first()
        assertEquals(reset * 1000, persisted.retryAt)
        assertEquals(CheckResult.Error(UpdateError.RateLimited(reset * 1000)), persisted.lastResult)
        assertEquals(1, first.listCalls())

        val restartedJob = SupervisorJob()
        val restarted = UpdateHarness(CoroutineScope(restartedJob + Dispatchers.Default), Dispatchers.IO, folder.root, UpdatePrefs(context))
        restarted.now = first.now + UpdateHarness.HOUR / 2
        restarted.publish("v0.0.2")
        restarted.repository.onHomeShown()
        restartedJob.joinAllChildren()
        assertEquals(0, restarted.listCalls())

        restarted.repository.check()
        restartedJob.joinAllChildren()
        assertEquals(1, restarted.listCalls())
        assertEquals(CheckResult.Ok, prefs.data.first().lastResult)
    }

    @Test
    fun `unreadable known updates fall back to none`() {
        assertEquals(KnownUpdates.None, UpdatePrefs.decodeKnown("not json"))
    }
}
