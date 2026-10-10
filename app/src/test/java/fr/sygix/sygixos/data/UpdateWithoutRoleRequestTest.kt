/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.domain.LauncherSystemController
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class UpdateWithoutRoleRequestTest {
    @get:Rule val folder = TemporaryFolder()
    private val context: Context = ApplicationProvider.getApplicationContext()

    private class RecordingHomeRole : HomeRoleGateway {
        var requests = 0
        override fun state() = SystemControlState.INACTIVE
        override fun request(): Boolean {
            requests++
            return true
        }
        override fun fallback(): Boolean {
            requests++
            return true
        }
    }

    private class Store : LauncherSystemStore {
        override val launcherSystem = MutableStateFlow(LauncherSystemPreferences())
        override suspend fun dismissLauncherOnboarding() = Unit
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) = Unit
        override suspend fun recordBootStartObserved(currentBoot: Int?) = Unit
    }

    private val roleActions = setOf("android.app.role.action.REQUEST_ROLE", Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS)

    @Test
    fun `self update up to the relaunch never requests the home role`() = runTest {
        val application = shadowOf(context as Application)
        application.clearNextStartedActivities()
        val role = RecordingHomeRole()
        val system = AndroidLauncherSystemGateway(context, role)
        val launcher = LauncherSystemController(Store(), system, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        launcher.onStartupFinished()
        launcher.onPermissionFinished()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val h = UpdateHarness(CoroutineScope(SupervisorJob() + dispatcher), dispatcher, folder.root.resolve("updates")).apply { publish("v0.0.2") }
        h.repository.status
        h.repository.check()
        advanceUntilIdle()
        h.repository.startUpdate("v0.0.2")
        h.foreground.isForeground = false
        advanceUntilIdle()
        h.repository.onInstallStatus(InstallStatus.PendingUserAction(1, Intent("confirm")))
        advanceUntilIdle()
        h.foreground.isForeground = true
        h.repository.onForeground()
        launcher.onResume()
        advanceUntilIdle()
        assertEquals(299L, (h.store as MemoryUpdateStore).relaunch)
        runBlocking { UpdateRelaunchReceiver.relaunchIfRequested(context, h.store, 299L) }
        val relaunch = application.nextStartedActivity
        assertEquals(MainActivity::class.java.name, relaunch?.component?.className)
        val relaunched = LauncherSystemController(Store(), system, backgroundScope, UnconfinedTestDispatcher(testScheduler))
        relaunched.onStartupFinished()
        relaunched.onPermissionFinished()
        relaunched.onResume()
        assertEquals(0, role.requests)
        assertEquals(listOf("confirm"), h.screens.launched.map { it.action })
        assertTrue(h.screens.launched.none { it.action in roleActions })
        assertTrue(relaunch?.action !in roleActions)
        assertEquals(null, application.nextStartedActivity)
        assertEquals(SystemControlState.INACTIVE, relaunched.state.value.homeRole)
    }
}
