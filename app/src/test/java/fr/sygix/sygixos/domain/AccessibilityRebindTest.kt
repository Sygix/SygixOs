/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccessibilityRebindTest {
    private val connected = MutableStateFlow(false)
    private var enabled = true
    private var toggles = 0
    private var connectOnToggle = false

    private fun TestScope.rebind() = AccessibilityRebind(
        enabled = { enabled },
        connected = connected,
        toggle = { toggles++; if (connectOnToggle) connected.value = true },
        scope = backgroundScope,
    )

    @Test
    fun `an enabled service bound after the component toggle ends connected with a single toggle`() = runTest {
        connectOnToggle = true
        val rebind = rebind()
        rebind.check()
        runCurrent()
        assertEquals(RebindStatus.CHECKING, rebind.status.value)
        advanceTimeBy(AccessibilityRebind.WAIT_MS + 1)
        runCurrent()
        assertEquals(1, toggles)
        assertEquals(RebindStatus.IDLE, rebind.status.value)
    }

    @Test
    fun `an enabled service still unbound after the toggle is blocked and never toggled twice`() = runTest {
        val rebind = rebind()
        rebind.check()
        advanceTimeBy(AccessibilityRebind.WAIT_MS * 2 + 1)
        runCurrent()
        assertEquals(RebindStatus.BLOCKED, rebind.status.value)
        rebind.check()
        advanceTimeBy(AccessibilityRebind.WAIT_MS * 3)
        runCurrent()
        assertEquals(1, toggles)
        assertEquals(RebindStatus.BLOCKED, rebind.status.value)
        connected.value = true
        rebind.check()
        assertEquals(RebindStatus.IDLE, rebind.status.value)
    }

    @Test
    fun `a service connected at launch or not enabled needs no toggle`() = runTest {
        connected.value = true
        val rebind = rebind()
        rebind.check()
        runCurrent()
        assertEquals(RebindStatus.IDLE, rebind.status.value)
        connected.value = false
        enabled = false
        rebind.check()
        advanceTimeBy(AccessibilityRebind.WAIT_MS * 3)
        runCurrent()
        assertEquals(0, toggles)
        assertEquals(RebindStatus.IDLE, rebind.status.value)
    }

    @Test
    fun `a late connection during the first wait avoids the toggle`() = runTest {
        val rebind = rebind()
        rebind.check()
        advanceTimeBy(500)
        connected.value = true
        runCurrent()
        assertEquals(0, toggles)
        assertEquals(RebindStatus.IDLE, rebind.status.value)
    }

    @Test
    fun `enabled services are read from the secure setting in full or short form`() {
        val cls = "fr.sygix.sygixos.data.HomeAccessibilityService"
        assertTrue(AccessibilityRebind.isEnabled("other/.A:fr.sygix.sygixos/$cls", "fr.sygix.sygixos", cls))
        assertTrue(AccessibilityRebind.isEnabled("fr.sygix.sygixos/.data.HomeAccessibilityService", "fr.sygix.sygixos", cls))
        assertFalse(AccessibilityRebind.isEnabled("fr.sygix.sygixos.perf/$cls", "fr.sygix.sygixos", cls))
        assertFalse(AccessibilityRebind.isEnabled(null, "fr.sygix.sygixos", cls))
        assertFalse(AccessibilityRebind.isEnabled("", "fr.sygix.sygixos", cls))
    }
}
