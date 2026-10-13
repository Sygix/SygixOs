/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.LauncherSystemPreferences
import fr.sygix.sygixos.model.GuideOrigin
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.CompletableDeferred
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
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
        var seenWrites = 0
        override suspend fun markLauncherGuideSeen() {
            seenWrites++
            launcherSystem.value = launcherSystem.value.copy(guideSeen = true)
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
        var accessibility = SystemControlState.INACTIVE
        var overlay = SystemControlState.INACTIVE
        val opened = mutableListOf<String>()
        var succeeds = true
        var verifications = 0
        val changes = MutableSharedFlow<Unit>()
        override fun verifyAccessibility() { verifications++ }
        override fun accessibilityChanges(): Flow<Unit> = changes
        override fun accessibilityState() = accessibility
        override fun overlayState() = overlay
        override fun bootCount(): Int = 8
        override fun openAccessibility(): Boolean = record("accessibility")
        override fun openOverlay(): Boolean = record("overlay")
        private fun record(name: String): Boolean {
            opened += name
            return succeeds
        }
    }

    private class FailingStore : LauncherSystemStore {
        override val launcherSystem = flow<LauncherSystemPreferences> { throw IOException("unreadable") }
        override suspend fun markLauncherGuideSeen() = throw IOException("read only")
        override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) = throw IOException("read only")
        override suspend fun recordBootStartObserved(currentBoot: Int?) = throw IOException("read only")
    }

    private fun TestScope.controller(store: LauncherSystemStore = Store(), platform: Platform = Platform()) =
        LauncherSystemController(store, platform, backgroundScope, UnconfinedTestDispatcher(testScheduler))

    @Test
    fun `unreadable or read only preferences never crash and fall back to defaults`() = runTest {
        val controller = controller(store = FailingStore())
        controller.onStartupFinished()
        controller.onPermissionFinished()
        assertTrue(controller.state.value.preferencesLoaded)
        assertTrue(controller.state.value.guideShown)
        controller.activate(GuideStep.BOOT)
        assertFalse(controller.state.value.bootEnabled)
        controller.recordBootObserved()
        controller.closeGuide()
        assertFalse(controller.state.value.guideShown)
    }

    @Test
    fun `failed system screen stays unavailable after resume and becomes inert`() = runTest {
        val platform = Platform().apply { succeeds = false }
        val controller = controller(platform = platform)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.accessibility)
        assertNull(controller.state.value.focusReturn)
        controller.onResume()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.accessibility)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(listOf("accessibility"), platform.opened)
        platform.accessibility = SystemControlState.ACTIVE
        controller.onResume()
        assertEquals(SystemControlState.ACTIVE, controller.state.value.accessibility)
    }

    @Test
    fun `unavailable controls never open anything`() = runTest {
        val platform = Platform().apply {
            accessibility = SystemControlState.UNAVAILABLE
        }
        val controller = controller(platform = platform)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertTrue(platform.opened.isEmpty())
    }

    @Test
    fun `enabled accessibility still opens system settings and rereads changes`() = runTest {
        val platform = Platform().apply { accessibility = SystemControlState.ACTIVE }
        val controller = controller(platform = platform)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(SystemControlState.PENDING, controller.state.value.accessibility)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(1, platform.opened.size)
        platform.accessibility = SystemControlState.INACTIVE
        controller.onResume()
        assertEquals(SystemControlState.INACTIVE, controller.state.value.accessibility)
    }

    @Test
    fun `focus return is one shot`() = runTest {
        val controller = controller()
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(GuideStep.ACCESSIBILITY, controller.state.value.focusReturn)
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
        assertFalse(controller.state.value.guideShown)
        controller.onStartupFinished()
        assertFalse(controller.state.value.guideShown)
        controller.onPermissionFinished()
        assertTrue(controller.state.value.guideShown)
        controller.closeGuide()
        assertFalse(controller.state.value.guideShown)
        assertTrue(store.launcherSystem.value.guideSeen)
        val recreated = controller(store, platform)
        recreated.onStartupFinished()
        recreated.onPermissionFinished()
        assertFalse(recreated.state.value.guideShown)
        assertTrue(platform.opened.isEmpty())
    }

    @Test
    fun `pending is not restored and late observation clears boot failure reactively`() = runTest {
        val store = Store()
        store.setBootStartEnabled(true, 7)
        val platform = Platform()
        val controller = controller(store, platform)
        assertTrue(controller.state.value.bootNotObserved)
        controller.activate(GuideStep.ACCESSIBILITY)
        val recreated = controller(store, platform)
        assertEquals(SystemControlState.INACTIVE, recreated.state.value.accessibility)
        controller.recordBootObserved()
        assertFalse(controller.state.value.bootNotObserved)
        assertFalse(recreated.state.value.bootNotObserved)
    }

    @Test
    fun `boot opt in shows the overlay sub step without opening anything and refusal keeps the choice`() = runTest {
        val store = Store()
        val platform = Platform()
        val controller = controller(store, platform)
        controller.activate(GuideStep.BOOT)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        assertTrue(platform.opened.isEmpty())
        assertTrue(controller.state.value.bootEnabled)
        controller.activate(GuideStep.OVERLAY)
        assertEquals(SystemControlState.PENDING, controller.state.value.overlay)
        assertEquals(GuideStep.OVERLAY, controller.state.value.focusReturn)
        assertEquals(listOf("overlay"), platform.opened)
        controller.onResume()
        assertEquals(SystemControlState.INACTIVE, controller.state.value.overlay)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        platform.overlay = SystemControlState.ACTIVE
        controller.onResume()
        controller.activate(GuideStep.OVERLAY)
        assertEquals(1, platform.opened.size)
        controller.activate(GuideStep.BOOT)
        assertFalse(controller.state.value.bootEnabled)
    }

    @Test
    fun `missing overlay screen refuses boot opt in`() = runTest {
        val store = Store()
        val platform = Platform().apply { overlay = SystemControlState.UNAVAILABLE }
        val controller = controller(store, platform)
        assertTrue(controller.state.value.bootBlocked)
        controller.activate(GuideStep.BOOT)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertEquals(0, store.bootWrites)
        assertTrue(platform.opened.isEmpty())
        platform.overlay = SystemControlState.ACTIVE
        controller.onResume()
        assertFalse(controller.state.value.bootBlocked)
        controller.activate(GuideStep.BOOT)
        assertTrue(controller.state.value.bootEnabled)
    }

    @Test
    fun `failed overlay opening turns boot start back off and blocks it until the process restarts`() = runTest {
        val store = Store()
        val platform = Platform().apply { succeeds = false }
        val controller = controller(store, platform)
        controller.activate(GuideStep.BOOT)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
        controller.activate(GuideStep.OVERLAY)
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.overlay)
        assertEquals(GuideStep.BOOT, controller.state.value.focusReturn)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertTrue(controller.state.value.bootBlocked)
        controller.onResume()
        assertEquals(SystemControlState.UNAVAILABLE, controller.state.value.overlay)
        controller.activate(GuideStep.BOOT)
        assertFalse(controller.state.value.bootEnabled)
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
        controller.activate(GuideStep.BOOT)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertTrue(controller.state.value.bootBlocked)
    }

    @Test
    fun `rapid toggles follow each press while writes are pending`() = runTest {
        val store = Store().apply { gate = CompletableDeferred() }
        val controller = controller(store)
        controller.activate(GuideStep.BOOT)
        controller.activate(GuideStep.BOOT)
        assertFalse(controller.state.value.bootEnabled)
        assertEquals(2, store.bootWrites)
        store.gate!!.complete(Unit)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
        assertFalse(controller.state.value.bootEnabled)
        controller.activate(GuideStep.BOOT)
        assertTrue(controller.state.value.bootEnabled)
        assertTrue(store.launcherSystem.value.bootStart.enabled)
    }

    @Test
    fun `boot toggle records the current boot and never reports the current boot as failed`() = runTest {
        val store = Store()
        val controller = controller(store)
        controller.activate(GuideStep.BOOT)
        assertEquals(8, store.launcherSystem.value.bootStart.enabledAtBoot)
        assertFalse(controller.state.value.bootNotObserved)
        controller.activate(GuideStep.BOOT)
        assertFalse(controller.state.value.bootEnabled)
        assertFalse(store.launcherSystem.value.bootStart.enabled)
    }

    @Test
    fun `foreground home is ignored during startup then closes the automatic guide for good`() = runTest {
        val store = Store()
        val platform = Platform()
        val controller = controller(store, platform)
        controller.onHome()
        assertEquals(0L, controller.state.value.homeRequest)
        assertFalse(store.launcherSystem.value.guideSeen)
        controller.onStartupFinished()
        controller.onPermissionFinished()
        assertEquals(GuideOrigin.AUTOMATIC, controller.state.value.guide)
        controller.onHome()
        assertEquals(1L, controller.state.value.homeRequest)
        assertFalse(controller.state.value.guideShown)
        assertTrue(store.launcherSystem.value.guideSeen)
        controller.onHome()
        assertEquals(2L, controller.state.value.homeRequest)
        assertTrue(platform.opened.isEmpty())
    }

    @Test
    fun `the guide opened from the settings closes back to the line without marking the automatic guide seen`() = runTest {
        val store = Store().apply { launcherSystem.value = launcherSystem.value.copy(guideSeen = true) }
        val controller = controller(store)
        controller.onStartupFinished()
        controller.onPermissionFinished()
        assertNull(controller.state.value.guide)
        controller.openGuide()
        assertEquals(GuideOrigin.SETTINGS, controller.state.value.guide)
        controller.closeGuide()
        assertNull(controller.state.value.guide)
        assertEquals(1L, controller.state.value.settingsFocus)
        assertEquals(0, store.seenWrites)
        controller.openGuide()
        controller.onHome()
        assertNull(controller.state.value.guide)
        assertEquals(0, store.seenWrites)
    }

    @Test
    fun `the automatic guide closed from settings origin never shows again and a new seen flag is written once`() = runTest {
        val store = Store()
        val controller = controller(store)
        controller.onStartupFinished()
        controller.onPermissionFinished()
        controller.closeGuide()
        controller.closeGuide()
        assertEquals(1, store.seenWrites)
        assertNull(controller.state.value.guide)
    }

    @Test
    fun `accessibility is verified on resume and reread on connection changes unless its screen is open`() = runTest {
        val platform = Platform()
        val controller = controller(platform = platform)
        val before = platform.verifications
        controller.onResume()
        assertEquals(before + 1, platform.verifications)
        platform.accessibility = SystemControlState.BLOCKED
        platform.changes.emit(Unit)
        assertEquals(SystemControlState.BLOCKED, controller.state.value.accessibility)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(SystemControlState.PENDING, controller.state.value.accessibility)
        platform.accessibility = SystemControlState.ACTIVE
        platform.changes.emit(Unit)
        assertEquals(SystemControlState.PENDING, controller.state.value.accessibility)
        controller.activate(GuideStep.ACCESSIBILITY)
        assertEquals(1, platform.opened.size)
        controller.onResume()
        assertEquals(SystemControlState.ACTIVE, controller.state.value.accessibility)
    }
}
