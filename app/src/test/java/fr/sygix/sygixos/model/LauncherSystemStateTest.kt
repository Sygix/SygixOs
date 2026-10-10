/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LauncherSystemStateTest {
    @Test
    fun `rows keep the mockup order with switch only on boot start`() {
        val rows = LauncherSystemState(homeRole = SystemControlState.ACTIVE, accessibility = SystemControlState.PENDING, bootEnabled = true).rows
        assertEquals(listOf(SystemControl.HOME_ROLE, SystemControl.BOOT_START, SystemControl.ACCESSIBILITY), rows.map { it.control })
        assertEquals(listOf(SystemControlState.ACTIVE, null, SystemControlState.PENDING), rows.map { it.state })
        assertEquals(listOf(null, true, null), rows.map { it.checked })
    }

    @Test
    fun `unavailable rows show their reason and are dimmed`() {
        val rows = LauncherSystemState(overlay = SystemControlState.INACTIVE).rows
        assertEquals(SystemRowDetail.HOME_ROLE_UNAVAILABLE, rows[0].detail)
        assertEquals(SystemRowDetail.ACCESSIBILITY_UNAVAILABLE, rows[2].detail)
        assertTrue(rows[0].dimmed)
        assertFalse(rows[1].dimmed)
        val available = LauncherSystemState(homeRole = SystemControlState.INACTIVE, accessibility = SystemControlState.ACTIVE).rows
        assertEquals(SystemRowDetail.HOME_ROLE, available[0].detail)
        assertEquals(SystemRowDetail.ACCESSIBILITY, available[2].detail)
        assertFalse(available[0].dimmed)
    }

    @Test
    fun `not observed boot changes only the detail while the option is on`() {
        assertEquals(SystemRowDetail.BOOT_NOT_OBSERVED, LauncherSystemState(bootEnabled = true, bootNotObserved = true).rows[1].detail)
        assertEquals(SystemRowDetail.BOOT_START, LauncherSystemState(overlay = SystemControlState.INACTIVE, bootEnabled = false, bootNotObserved = true).rows[1].detail)
        assertEquals(SystemRowDetail.BOOT_START, LauncherSystemState(bootEnabled = true).rows[1].detail)
        assertEquals(true, LauncherSystemState(bootEnabled = true, bootNotObserved = true).rows[1].checked)
    }

    @Test
    fun `boot row is unavailable while off without an overlay screen`() {
        val blocked = LauncherSystemState(overlay = SystemControlState.UNAVAILABLE).rows[1]
        assertEquals(SystemRowDetail.BOOT_UNAVAILABLE, blocked.detail)
        assertEquals(SystemControlState.UNAVAILABLE, blocked.state)
        assertNull(blocked.checked)
        assertTrue(blocked.dimmed)
        val on = LauncherSystemState(overlay = SystemControlState.UNAVAILABLE, bootEnabled = true).rows[1]
        assertEquals(true, on.checked)
        assertNull(on.state)
        assertEquals(false, LauncherSystemState(overlay = SystemControlState.INACTIVE).rows[1].checked)
    }

    @Test
    fun `pending covers every system screen and overlays cover both panels`() {
        assertFalse(LauncherSystemState(homeRole = SystemControlState.INACTIVE).systemPending)
        assertTrue(LauncherSystemState(homeRole = SystemControlState.PENDING).systemPending)
        assertTrue(LauncherSystemState(accessibility = SystemControlState.PENDING).systemPending)
        assertTrue(LauncherSystemState(overlay = SystemControlState.PENDING).systemPending)
        assertFalse(LauncherSystemState().overlayShown)
        assertTrue(LauncherSystemState(showOnboarding = true).overlayShown)
        assertTrue(LauncherSystemState(showOverlayConfirmation = true).overlayShown)
        assertNull(LauncherSystemState().focusReturn)
    }
}
