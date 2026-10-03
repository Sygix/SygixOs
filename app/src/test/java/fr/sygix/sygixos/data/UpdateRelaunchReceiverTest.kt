/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.ui.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowLooper

@RunWith(RobolectricTestRunner::class)
class UpdateRelaunchReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = UpdatePrefs(context)
    private val application get() = shadowOf(context as android.app.Application)

    @Before
    fun reset() = runBlocking {
        context.dataStore.edit { it.clear() }
        application.clearNextStartedActivities()
    }

    private fun deliver() {
        context.sendBroadcast(Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(context.packageName))
        ShadowLooper.idleMainLooper()
    }

    private fun waitForFlagCleared() = runBlocking {
        withContext(Dispatchers.Default) {
            withTimeout(5_000) {
                context.dataStore.data.first { prefs -> prefs.asMap().keys.none { it.name == "update_relaunch" } }
            }
        }
    }

    private fun awaitStartedActivity(): Intent? = runBlocking {
        withContext(Dispatchers.Default) {
            withTimeoutOrNull(5_000) {
                var started: Intent? = null
                while (started == null) {
                    started = application.peekNextStartedActivity()
                    if (started == null) delay(10)
                }
                started
            }
        }
    }

    @Test
    fun `receiver is declared for package replaced`() {
        val receivers = context.packageManager.queryBroadcastReceivers(
            Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(context.packageName),
            0,
        )
        assertTrue(receivers.any { it.activityInfo.name == UpdateRelaunchReceiver::class.java.name && !it.activityInfo.exported })
    }

    @Test
    fun `relaunch flag starts the home and is cleared`() {
        runBlocking { prefs.setRelaunch(true) }
        deliver()
        waitForFlagCleared()
        val started = awaitStartedActivity()
        assertEquals(MainActivity::class.java.name, started?.component?.className)
        assertTrue(started!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertFalse(runBlocking { prefs.takeRelaunch() })
    }

    @Test
    fun `no relaunch flag starts nothing and the flag stays cleared`() {
        runBlocking { prefs.setRelaunch(false) }
        deliver()
        waitForFlagCleared()
        ShadowLooper.idleMainLooper()
        assertNull(application.nextStartedActivity)
    }
}
