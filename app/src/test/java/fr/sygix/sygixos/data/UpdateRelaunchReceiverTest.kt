/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.Application
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.ui.MainActivity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class UpdateRelaunchReceiverTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = UpdatePrefs(context)
    private val application get() = shadowOf(context as Application)

    @Before
    fun reset() = runBlocking {
        context.dataStore.edit { it.clear() }
        application.clearNextStartedActivities()
    }

    private fun relaunchKeyPresent(): Boolean = runBlocking {
        context.dataStore.data.first().asMap().keys.any { it.name == "update_relaunch" }
    }

    @Test
    fun `receiver is declared for package replaced and not exported`() {
        val receivers = context.packageManager.queryBroadcastReceivers(
            Intent(Intent.ACTION_MY_PACKAGE_REPLACED).setPackage(context.packageName),
            0,
        )
        assertTrue(receivers.any { it.activityInfo.name == UpdateRelaunchReceiver::class.java.name && !it.activityInfo.exported })
    }

    @Test
    fun `relaunch flag starts the home in a new task and is cleared`() = runBlocking {
        prefs.setRelaunch(true)
        UpdateRelaunchReceiver.relaunchIfRequested(context, UpdatePrefs(context))
        val started = application.nextStartedActivity
        assertEquals(MainActivity::class.java.name, started?.component?.className)
        assertTrue(started!!.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertFalse(relaunchKeyPresent())
    }

    @Test
    fun `no relaunch flag starts nothing and the flag is cleared`() = runBlocking {
        prefs.setRelaunch(false)
        UpdateRelaunchReceiver.relaunchIfRequested(context, UpdatePrefs(context))
        assertNull(application.nextStartedActivity)
        assertFalse(relaunchKeyPresent())
    }

    @Test
    fun `missing flag starts nothing`() = runBlocking {
        UpdateRelaunchReceiver.relaunchIfRequested(context, UpdatePrefs(context))
        assertNull(application.nextStartedActivity)
    }
}
