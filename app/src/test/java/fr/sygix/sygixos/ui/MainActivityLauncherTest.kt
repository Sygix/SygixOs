/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui

import android.app.Activity
import android.app.Application
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.os.Looper
import android.provider.Settings
import androidx.datastore.preferences.core.edit
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.LauncherSystemIntents
import fr.sygix.sygixos.data.dataStore
import fr.sygix.sygixos.model.SystemControl
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.ui.home.LauncherSystemViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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
import org.robolectric.shadows.ShadowRoleManager

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
        ShadowRoleManager.reset()
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
        val job = CoroutineScope(Dispatchers.IO).launch { block() }
        awaitMain { job.isCompleted }
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
    }

    private fun ActivityController<MainActivity>.deliverFromBackground(intent: Intent) {
        pause().stop()
        restart()
        newIntent(intent)
        resume()
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
        assertFalse(launcher.state.value.showOnboarding)
        val request = shadowOf(controller.get()).lastRequestedPermission
        assertNotNull(request)
        controller.get().onRequestPermissionsResult(request.requestCode, request.requestedPermissions, intArrayOf(PackageManager.PERMISSION_DENIED))
        awaitMain { launcher.state.value.showOnboarding }
        assertTrue(launcher.state.value.showOnboarding)
    }

    @Test
    fun `role dialogue is started for a result by the activity`() {
        val roles = context.getSystemService(RoleManager::class.java)
        shadowOf(roles).addAvailableRole(RoleManager.ROLE_HOME)
        val controller = launch()
        val launcher = launcherOf(controller)
        assertEquals(SystemControlState.INACTIVE, launcher.state.value.homeRole)
        launcher.actions.activate(SystemControl.HOME_ROLE)
        val started = shadowOf(controller.get()).nextStartedActivityForResult
        assertNotNull(started)
        assertEquals("android.app.role.action.REQUEST_ROLE", started.intent.action)
        assertEquals(0, started.intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK)
        assertEquals(SystemControlState.PENDING, launcher.state.value.homeRole)
        controller.pause()
        controller.resume()
        assertEquals(SystemControlState.INACTIVE, launcher.state.value.homeRole)
        shadowOf(roles).addHeldRole(RoleManager.ROLE_HOME)
        controller.pause()
        controller.resume()
        assertEquals(SystemControlState.ACTIVE, launcher.state.value.homeRole)
    }

    @Test
    fun `role screen refused at once falls back to the home settings screen`() {
        val roles = context.getSystemService(RoleManager::class.java)
        shadowOf(roles).addAvailableRole(RoleManager.ROLE_HOME)
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(Settings.ACTION_HOME_SETTINGS),
            ResolveInfo().apply {
                activityInfo = ActivityInfo().apply {
                    packageName = "com.android.tv.settings"
                    name = "com.android.tv.settings.HomeSettings"
                    applicationInfo = ApplicationInfo().apply { packageName = "com.android.tv.settings" }
                }
            },
        )
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),
            ResolveInfo().apply {
                priority = 1_000
                isDefault = true
                activityInfo = ActivityInfo().apply {
                    packageName = "com.vendor.launcher"
                    name = "com.vendor.launcher.Home"
                    applicationInfo = ApplicationInfo().apply { packageName = "com.vendor.launcher" }
                }
            },
        )
        val controller = launch()
        val launcher = launcherOf(controller)
        launcher.actions.activate(SystemControl.HOME_ROLE)
        val started = shadowOf(controller.get()).nextStartedActivityForResult
        assertEquals("android.app.role.action.REQUEST_ROLE", started.intent.action)
        shadowOf(context as Application).clearNextStartedActivities()
        shadowOf(controller.get()).receiveResult(started.intent, Activity.RESULT_CANCELED, null)
        assertEquals(Settings.ACTION_HOME_SETTINGS, shadowOf(context as Application).nextStartedActivity?.action)
        assertEquals(SystemControlState.PENDING, launcher.state.value.homeRole)
    }

    private companion object {
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
    }
}
