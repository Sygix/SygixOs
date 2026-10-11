/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import android.provider.Settings
import fr.sygix.sygixos.domain.AccessibilityCheck
import fr.sygix.sygixos.model.SystemControlState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class AndroidLauncherSystemGatewayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val home = object : HomeRoleGateway {
        override fun state() = SystemControlState.INACTIVE
        override fun request() = true
        override fun fallback() = true
    }

    @Test
    fun `service must be enabled and running, pending while the launch check runs and blocked after it`() = runTest {
        var enabled = false
        val connected = MutableStateFlow(false)
        val check = AccessibilityCheck({ enabled }, connected, backgroundScope)
        val gateway = AndroidLauncherSystemGateway(context, home, { true }, { true }, { enabled }, { connected.value }, { false }, { 2 }, check)
        assertEquals(SystemControlState.INACTIVE, gateway.accessibilityState())
        enabled = true
        assertEquals(SystemControlState.PENDING, gateway.accessibilityState())
        gateway.verifyAccessibility()
        runCurrent()
        assertEquals(SystemControlState.PENDING, gateway.accessibilityState())
        advanceTimeBy(AccessibilityCheck.WAIT_MS + 1)
        runCurrent()
        assertEquals(SystemControlState.BLOCKED, gateway.accessibilityState())
        connected.value = true
        assertEquals(SystemControlState.ACTIVE, gateway.accessibilityState())
        enabled = false
        assertEquals(SystemControlState.INACTIVE, gateway.accessibilityState())
    }

    @Test
    fun `the secure setting decides whether the service is enabled`() {
        val resolver = context.contentResolver
        Settings.Secure.putString(resolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "")
        assertFalse(AndroidLauncherSystemGateway.serviceEnabledIn(context))
        Settings.Secure.putString(
            resolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
            "${context.packageName}/${HomeAccessibilityService::class.java.name}",
        )
        assertTrue(AndroidLauncherSystemGateway.serviceEnabledIn(context))
    }

    @Test
    fun `overlay opt in opens only when explicitly requested and never implies grant`() {
        var granted = false
        val opened = mutableListOf<Intent>()
        val gateway = AndroidLauncherSystemGateway(context, home, { true }, { opened.add(it); true }, { false }, { false }, { granted }, { 2 })
        assertEquals(SystemControlState.INACTIVE, gateway.overlayState())
        assertTrue(opened.isEmpty())
        assertTrue(gateway.openOverlay())
        assertEquals("package:${context.packageName}", opened.single().data.toString())
        assertEquals(SystemControlState.INACTIVE, gateway.overlayState())
        granted = true
        assertEquals(SystemControlState.ACTIVE, gateway.overlayState())
    }

    @Test
    fun `missing settings and denied provider access are safe and do not open screens`() {
        var opens = 0
        val gateway = AndroidLauncherSystemGateway(context, home, { throw SecurityException() }, { opens++; true }, { throw SecurityException() }, { false }, { throw SecurityException() }, { throw SecurityException() })
        assertEquals(SystemControlState.UNAVAILABLE, gateway.accessibilityState())
        assertEquals(SystemControlState.UNAVAILABLE, gateway.overlayState())
        assertNull(gateway.bootCount())
        assertFalse(gateway.openAccessibility())
        assertFalse(gateway.openOverlay())
        assertEquals(0, opens)
    }

    @Test
    fun `settings launch failure cannot crash or report permission as granted`() {
        val gateway = AndroidLauncherSystemGateway(context, home, { true }, { throw SecurityException() }, { false }, { false }, { false }, { -1 })
        assertFalse(gateway.openAccessibility())
        assertFalse(gateway.openOverlay())
        assertEquals(SystemControlState.INACTIVE, gateway.overlayState())
        assertNull(gateway.bootCount())
    }
}
