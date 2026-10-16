/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Looper
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.LauncherSystemIntents
import fr.sygix.sygixos.data.dataStore
import fr.sygix.sygixos.ui.home.LauncherSystemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w960dp-h540dp-xhdpi")
class MainActivityLauncherTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs get() = (context as SygixOsApp).launcherPrefs
    private val homeIntent get() = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)

    private val launched = mutableListOf<ActivityController<MainActivity>>()

    @Before
    fun reset() {
        onIo {
            context.dataStore.edit { it.clear() }
            (context as SygixOsApp).updatePrefs.setLastCheckAt(System.currentTimeMillis())
        }
    }

    @After
    fun drain() {
        launched.forEach { controller ->
            runCatching { controller.pause() }
            runCatching { controller.stop() }
            runCatching { controller.destroy() }
        }
        onIo { context.dataStore.edit { } }
    }

    private fun launch(grant: Boolean = true, intent: Intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)): ActivityController<MainActivity> {
        if (grant) shadowOf(context as Application).grantPermissions(READ_TV_LISTINGS)
        return Robolectric.buildActivity(MainActivity::class.java, intent).setup().also { launched += it }
    }

    private fun onIo(block: suspend () -> Unit) {
        val scope = CoroutineScope(Dispatchers.IO)
        val work = scope.async { block() }
        try {
            awaitMain { work.isCompleted }
            runBlocking { work.await() }
        } finally {
            runBlocking { work.cancelAndJoin() }
            scope.cancel()
        }
    }

    private fun launcherOf(controller: ActivityController<MainActivity>) =
        ViewModelProvider(controller.get())[LauncherSystemViewModel::class.java]

    private fun awaitMain(condition: () -> Boolean) = runBlocking {
        withTimeout(5_000) {
            while (true) {
                shadowOf(Looper.getMainLooper()).idle()
                if (condition()) break
                delay(10)
            }
        }
    }

    private fun ActivityController<MainActivity>.deliverInFront(intent: Intent) {
        pause()
        newIntent(intent)
        resume()
        shadowOf(Looper.getMainLooper()).idle()
    }

    private fun ActivityController<MainActivity>.deliverFromBackground(intent: Intent) {
        pause().stop()
        restart()
        newIntent(intent)
        resume()
        shadowOf(Looper.getMainLooper()).idle()
    }

    @Test
    fun `home intent delivered while in front is handled although the activity is paused first`() {
        val controller = launch()
        val launcher = launcherOf(controller)
        launcher.onStartupFinished()
        controller.deliverInFront(homeIntent)
        assertEquals(1L, launcher.state.value.homeRequest)
        controller.deliverInFront(homeIntent)
        assertEquals(2L, launcher.state.value.homeRequest)
    }

    @Test
    fun `home intent bringing the launcher back from another app keeps the existing return rules`() {
        val controller = launch()
        val launcher = launcherOf(controller)
        launcher.onStartupFinished()
        controller.deliverFromBackground(homeIntent)
        assertEquals(0L, launcher.state.value.homeRequest)
        controller.deliverInFront(homeIntent)
        assertEquals(1L, launcher.state.value.homeRequest)
    }

    @Test
    fun `home intent during the splash changes nothing`() {
        val controller = launch()
        val launcher = launcherOf(controller)
        controller.deliverInFront(homeIntent)
        assertEquals(0L, launcher.state.value.homeRequest)
    }

    @Test
    fun `accessibility home trusts the foreground flag captured by the service`() {
        val controller = launch()
        val launcher = launcherOf(controller)
        launcher.onStartupFinished()
        controller.deliverFromBackground(LauncherSystemIntents.home(context, wasForeground = false))
        assertEquals(0L, launcher.state.value.homeRequest)
        controller.deliverFromBackground(LauncherSystemIntents.home(context, wasForeground = true))
        assertEquals(1L, launcher.state.value.homeRequest)
        val forged = Intent(context, MainActivity::class.java)
            .putExtra("fr.sygix.sygixos.LAUNCHER_SOURCE", "accessibility")
            .putExtra("fr.sygix.sygixos.LAUNCHER_FOREGROUND", true)
        controller.deliverFromBackground(forged)
        assertEquals(1L, launcher.state.value.homeRequest)
    }

    @Test
    fun `a boot opening received in front is never handled as the home key`() {
        val controller = launch()
        val launcher = launcherOf(controller)
        launcher.onStartupFinished()
        controller.deliverInFront(LauncherSystemIntents.boot(context))
        assertEquals(0L, launcher.state.value.homeRequest)
        controller.deliverInFront(LauncherSystemIntents.home(context, wasForeground = true))
        assertEquals(1L, launcher.state.value.homeRequest)
    }

    @Test
    fun `boot intents record the observed opening at creation and on new intent`() {
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 5)
        onIo { prefs.setBootStartEnabled(true, 4) }
        val controller = launch(intent = LauncherSystemIntents.boot(context))
        onIo { prefs.launcherSystem.first { it.bootStart.observedAtBoot == 5 } }
        Settings.Global.putInt(context.contentResolver, Settings.Global.BOOT_COUNT, 6)
        controller.deliverFromBackground(LauncherSystemIntents.boot(context))
        onIo { prefs.launcherSystem.first { it.bootStart.observedAtBoot == 6 } }
    }

    @Test
    fun `onboarding waits for the tv programs permission answer`() {
        onIo { prefs.setBootStartEnabled(true, null) }
        val controller = launch(grant = false)
        val launcher = launcherOf(controller)
        launcher.onStartupFinished()
        awaitMain { launcher.state.value.bootEnabled }
        assertFalse(launcher.state.value.guideShown)
        val request = shadowOf(controller.get()).lastRequestedPermission
        assertNotNull(request)
        controller.get().onRequestPermissionsResult(request.requestCode, request.requestedPermissions, intArrayOf(PackageManager.PERMISSION_DENIED))
        awaitMain { launcher.state.value.guideShown }
        assertTrue(launcher.state.value.guideShown)
    }

    private companion object {
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
    }
}
