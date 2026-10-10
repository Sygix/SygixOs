/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.SystemControlState
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidLauncherSystemGatewayTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val home = object : HomeRoleGateway {
        override fun state() = SystemControlState.INACTIVE
        override fun request() = true
        override fun fallback() = true
    }

    @Test
    fun `service must be enabled and running and states are always reread`() {
        var enabled = false
        var running = false
        val gateway = AndroidLauncherSystemGateway(context, home, { true }, { true }, { enabled }, { running }, { false }, { 2 })
        assertEquals(SystemControlState.INACTIVE, gateway.accessibilityState())
        enabled = true
        assertEquals(SystemControlState.INACTIVE, gateway.accessibilityState())
        running = true
        assertEquals(SystemControlState.ACTIVE, gateway.accessibilityState())
        enabled = false
        assertEquals(SystemControlState.INACTIVE, gateway.accessibilityState())
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
