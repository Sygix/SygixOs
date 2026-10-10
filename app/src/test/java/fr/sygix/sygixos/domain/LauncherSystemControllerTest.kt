/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.SystemControl
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSystemControllerTest {
    private class Store : LauncherSystemStore {
        override val launcherSystem = MutableStateFlow(LauncherSystemPreferences())
        var gate: CompletableDeferred<Unit>? = null
        var bootWrites = 0
        override suspend fun dismissLauncherOnboarding() {
            launcherSystem.value = launcherSystem.value.copy(onboardingDismissed = true)
        }
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) {
            bootWrites++
            gate?.await()
            launcherSystem.value = launcherSystem.value.copy(bootStart = launcherSystem.value.bootStart.copy(enabled = enabled, enabledAtBoot = currentBoot))
        }
        override suspend fun recordBootStartObserved(currentBoot: Int?) {
            launcherSystem.value = launcherSystem.value.copy(bootStart = launcherSystem.value.bootStart.copy(observedAtBoot = currentBoot))
        }
    }

    private class Platform : LauncherSystemGateway {
        var home = SystemControlState.INACTIVE
        var accessibility = SystemControlState.INACTIVE
        var overlay = SystemControlState.INACTIVE
        val opened = mutableListOf<String>()
        var succeeds = true
        var onOpen: () -> Unit = {}
        override fun homeRoleState() = home
        override fun accessibilityState() = accessibility
        override fun overlayState() = overlay
        override fun bootCount(): Int = 8
        override fun openHomeRole(): Boolean = record("home")
        override fun openHomeRoleFallback(): Boolean = record("home-fallback")
        override fun openAccessibility(): Boolean = record("accessibility")
        override fun openOverlay(): Boolean = record("overlay")
        private fun record(name: String): Boolean {
            opened += name
            onOpen()
            return succeeds
        }
    }

    private class FailingStore : LauncherSystemStore {
        override val launcherSystem = flow<LauncherSystemPreferences> { throw IOException("unreadable") }
        override suspend fun dismissLauncherOnboarding() = throw IOException("read only")
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) = throw IOException("read only")
        override suspend fun recordBootStartObserved(currentBoot: Int?) = throw IOException("read only")
    }

    private var now = 0L

    private fun TestScope.controller(store: LauncherSystemStore = Store(), platform: Platform = Platform()) =
        LauncherSystemController(store, platform, backgroundScope, UnconfinedTestDispatcher(testScheduler), clock = { now })

    @Test
    fun `role screen closing at once without the role offers the default apps screen`() = runTest {
        val platform = Platform()
        val controller = controller(platform = platform)
        now = 1_000
        controller.activate(SystemControl.HOME_ROLE)
        now = 1_150
        controller.onHomeRoleResult(granted = false)
        assertEquals(listOf("home", "home-fallback"), platform.opened)
        assertEquals(SystemControlState.PENDING, controller.state.value.homeRole)
        assertEquals(SystemControl.HOME_ROLE, controller.state.value.focusReturn)
        controller.onHomeRoleResult(granted = false)
        assertEquals(2, platform.opened.size)
    }

    @Test
    fun `slow refusal acceptance or a role granted meanwhile never opens the fallback`() = runTest {
        val platform = Platform()
        val controller = controller(platform = platform)
        now = 0
        controller.activate(SystemControl.HOME_ROLE)
        now = 5_000
        controller.onHomeRoleResult(granted = false)
        controller.onResume()
        now = 6_000
        controller.activate(SystemControl.HOME_ROLE)
        controller.onHomeRoleResult(granted = true)
        controller.onResume()
        platform.home = SystemControlState.INACTIVE
        controller.activate(SystemControl.HOME_ROLE)
        platform.home = SystemControlState.ACTIVE
        controller.onHomeRoleResult(granted = false)
        assertEquals(listOf("home", "home", "home"), platform.opened)
        controller.onHomeRoleResult(granted = false)
        assertEquals(3, platform.opened.size)
    }

    @Test
    fun `fallback that cannot open after an automatic refusal shows unavailable`() = runTest {
        val platform = Platform()
        val controller = controller(platform = platform)
        controller.activate(SystemControl.HOME_ROLE)
        platform.succeeds = false
        controller.onHomeRoleResult(granted = false)
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.homeRole)
        controller.onResume()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.homeRole)
    }

    @Test
    fun `unreadable or read only preferences never crash and fall back to defaults`() = runTest {
        val controller = controller(store = FailingStore())
        controller.onStartupFinished()
        controller.onPermissionFinished()
        assertTrue(controller.state.value.preferencesLoaded)
        assertTrue(controller.state.value.showOnboarding)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.bootEnabled)
        controller.recordBootObserved()
        controller.dismissOnboarding()
        assertFalse(controller.state.value.showOnboarding)
    }

    @Test
    fun `refused role request passes through pending and never claims success`() = runTest {
        val platform = Platform()
        val controller = controller(platform = platform)
        controller.activate(SystemControl.HOME_ROLE)
        assertEquals(listOf("home"), platform.opened)
        assertEquals(SystemControlState.PENDING, controller.state.value.homeRole)
        assertEquals(SystemControl.HOME_ROLE, controller.state.value.focusReturn)
        controller.onResume()
        assertEquals(SystemControlState.INACTIVE, controller.state.value.homeRole)
        platform.home = SystemControlState.ACTIVE
        controller.onResume()
        assertEquals(SystemControlState.ACTIVE, controller.state.value.homeRole)
    }

    @Test
    fun `held role is a no-op that stays active`() = runTest {
        val platform = Platform().apply { home = SystemControlState.ACTIVE }
        val controller = controller(platform = platform)
        controller.activate(SystemControl.HOME_ROLE)
        assertTrue(platform.opened.isEmpty())
        assertEquals(SystemControlState.ACTIVE, controller.state.value.homeRole)
        assertNull(controller.state.value.focusReturn)
    }

    @Test
    fun `role granted while requesting is reread instead of becoming unavailable`() = runTest {
        val platform = Platform().apply { succeeds = false }
        platform.onOpen = { platform.home = SystemControlState.ACTIVE }
        val controller = controller(platform = platform)
        controller.activate(SystemControl.HOME_ROLE)
        assertEquals(SystemControlState.ACTIVE, controller.state.value.homeRole)
        controller.onResume()
        assertEquals(SystemControlState.ACTIVE, controller.state.value.homeRole)
    }

    @Test
    fun `failed system screen stays unavailable after resume and becomes inert`() = runTest {
        val platform = Platform().apply { succeeds = false }
        val controller = controller(platform = platform)
        controller.activate(SystemControl.ACCESSIBILITY)
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.accessibility)
        assertNull(controller.state.value.focusReturn)
        controller.onResume()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.accessibility)
        controller.activate(SystemControl.ACCESSIBILITY)
        assertEquals(listOf("accessibility"), platform.opened)
        controller.activate(SystemControl.HOME_ROLE)
        controller.onResume()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.homeRole)
        platform.home = SystemControlState.ACTIVE
        controller.onResume()
        assertEquals(SystemControlState.ACTIVE, controller.state.value.homeRole)
    }

    @Test
    fun `unavailable controls never open anything`() = runTest {
        val platform = Platform().apply {
            home = SystemControlState.UNAVAILABLE
            accessibility = SystemControlState.UNAVAILABLE
        }
        val controller = controller(platform = platform)
        controller.activate(SystemControl.HOME_ROLE)
        controller.activate(SystemControl.ACCESSIBILITY)
        assertTrue(platform.opened.isEmpty())
    }

    @Test
    fun `enabled accessibility still opens system settings and rereads changes`() = runTest {
        val platform = Platform().apply { accessibility = SystemControlState.ACTIVE }
        val controller = controller(platform = platform)
        controller.activate(SystemControl.ACCESSIBILITY)
        assertEquals(SystemControlState.PENDING, controller.state.value.accessibility)
        controller.activate(SystemControl.ACCESSIBILITY)
        assertEquals(1, platform.opened.size)
        platform.accessibility = SystemControlState.INACTIVE
        controller.onResume()
        assertEquals(SystemControlState.INACTIVE, controller.state.value.accessibility)
    }

    @Test
    fun `focus return is one shot`() = runTest {
        val controller = controller()
        controller.activate(SystemControl.ACCESSIBILITY)
        assertEquals(SystemControl.ACCESSIBILITY, controller.state.value.focusReturn)
        controller.focusReturned()
        assertNull(controller.state.value.focusReturn)
        controller.onResume()
        assertNull(controller.state.value.focusReturn)
    }

    @Test
    fun `onboarding waits for preferences splash and permission and dismissal persists`() = runTest {
        val store = Store()
        val platform = Platform()
        val controller = controller(store, platform)
        assertFalse(controller.state.value.showOnboarding)
        controller.onStartupFinished()
        assertFalse(controller.state.value.showOnboarding)
        controller.onPermissionFinished()
        assertTrue(controller.state.value.showOnboarding)
        controller.dismissOnboarding()
        assertFalse(controller.state.value.showOnboarding)
        assertTrue(store.launcherSystem.value.onboardingDismissed)
        val recreated = controller(store, platform)
        recreated.onStartupFinished()
        recreated.onPermissionFinished()
        assertFalse(recreated.state.value.showOnboarding)
        assertTrue(platform.opened.isEmpty())
    }

    @Test
    fun `pending is not restored and late observation clears boot failure reactively`() = runTest {
        val store = Store()
        store.setBootStartEnabled(true, 7)
        val platform = Platform()
        val controller = controller(store, platform)
        assertTrue(controller.state.value.bootNotObserved)
        controller.activate(SystemControl.HOME_ROLE)
        val recreated = controller(store, platform)
        assertEquals(SystemControlState.INACTIVE, recreated.state.value.homeRole)
        controller.recordBootObserved()
        assertFalse(controller.state.value.bootNotObserved)
        assertFalse(recreated.state.value.bootNotObserved)
    }

    @Test
    fun `boot opt in asks for overlay permission and refusal keeps the choice`() = runTest {
        val store = Store()
        val platform = Platform()
        val controller = controller(store, platform)
        controller.activate(SystemControl.BOOT_START)
        assertTrue(controller.state.value.showOverlayConfirmation)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        assertTrue(platform.opened.isEmpty())
        controller.declineOverlay()
        assertFalse(controller.state.value.showOverlayConfirmation)
        assertTrue(controller.state.value.bootEnabled)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        assertEquals(SystemControl.BOOT_START, controller.state.value.focusReturn)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.showOverlayConfirmation)
        controller.activate(SystemControl.BOOT_START)
        assertTrue(controller.state.value.showOverlayConfirmation)
        controller.confirmOverlay()
        assertFalse(controller.state.value.showOverlayConfirmation)
        assertEquals(SystemControlState.PENDING, controller.state.value.overlay)
        assertEquals(listOf("overlay"), platform.opened)
        controller.onResume()
        assertEquals(SystemControlState.INACTIVE, controller.state.value.overlay)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        platform.overlay = SystemControlState.ACTIVE
        controller.onResume()
        controller.activate(SystemControl.BOOT_START)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.showOverlayConfirmation)
    }

    @Test
    fun `missing overlay screen refuses boot opt in without a confirmation`() = runTest {
        val store = Store()
        val platform = Platform().apply { overlay = SystemControlState.UNAVAILABLE }
        val controller = controller(store, platform)
        assertTrue(controller.state.value.bootBlocked)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.showOverlayConfirmation)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertEquals(0, store.bootWrites)
        assertTrue(platform.opened.isEmpty())
        platform.overlay = SystemControlState.ACTIVE
        controller.onResume()
        assertFalse(controller.state.value.bootBlocked)
        controller.activate(SystemControl.BOOT_START)
        assertTrue(controller.state.value.bootEnabled)
        assertFalse(controller.state.value.showOverlayConfirmation)
    }

    @Test
    fun `failed overlay opening turns boot start back off and blocks it until the process restarts`() = runTest {
        val store = Store()
        val platform = Platform().apply { succeeds = false }
        val controller = controller(store, platform)
        controller.activate(SystemControl.BOOT_START)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        controller.confirmOverlay()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.overlay)
        assertFalse(controller.state.value.showOverlayConfirmation)
        assertEquals(SystemControl.BOOT_START, controller.state.value.focusReturn)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertTrue(controller.state.value.bootBlocked)
        controller.onResume()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.overlay)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(controller.state.value.showOverlayConfirmation)
        assertEquals(1, platform.opened.size)
        platform.overlay = SystemControlState.ACTIVE
        controller.onResume()
        assertFalse(controller.state.value.bootBlocked)
        val restarted = controller(store, Platform())
        assertFalse(restarted.state.value.bootBlocked)
    }

    @Test
    fun `boot start already on stays switchable off when the overlay screen is missing`() = runTest {
        val store = Store()
        store.setBootStartEnabled(true, 7)
        val controller = controller(store, Platform().apply { overlay = SystemControlState.UNAVAILABLE })
        assertTrue(controller.state.value.bootEnabled)
        assertFalse(controller.state.value.bootBlocked)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertTrue(controller.state.value.bootBlocked)
    }

    @Test
    fun `rapid toggles follow each press while writes are pending`() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        val controller = controller(store)
        controller.activate(SystemControl.BOOT_START)
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.bootEnabled)
        assertEquals(2, store.bootWrites)
        store.gate!!.complete(Unit)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertFalse(controller.state.value.bootEnabled)
        controller.activate(SystemControl.BOOT_START)
        assertTrue(controller.state.value.bootEnabled)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
    }

    @Test
    fun `boot toggle records the current boot and never reports the current boot as failed`() = runTest {
        val store = Store()
        val controller = controller(store)
        controller.activate(SystemControl.BOOT_START)
        assertEquals(8, store.launcherSystem.value.bootStart.enabledAtBoot)
        assertFalse(controller.state.value.bootNotObserved)
        controller.declineOverlay()
        controller.activate(SystemControl.BOOT_START)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
    }

    @Test
    fun `foreground home is ignored during startup then closes onboarding and confirmation`() = runTest {
        val store = Store()
        val platform = Platform()
        val controller = controller(store, platform)
        controller.onHome()
        assertEquals(0L, controller.state.value.homeRequest)
        assertFalse(store.launcherSystem.value.onboardingDismissed)
        controller.onStartupFinished()
        controller.onPermissionFinished()
        assertTrue(controller.state.value.showOnboarding)
        controller.activate(SystemControl.BOOT_START)
        assertTrue(controller.state.value.showOverlayConfirmation)
        controller.onHome()
        assertEquals(1L, controller.state.value.homeRequest)
        assertFalse(controller.state.value.showOnboarding)
        assertFalse(controller.state.value.showOverlayConfirmation)
        assertTrue(store.launcherSystem.value.onboardingDismissed)
        controller.onHome()
        assertEquals(2L, controller.state.value.homeRequest)
        assertTrue(platform.opened.isEmpty())
    }
}
