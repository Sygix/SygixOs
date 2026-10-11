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
class AccessibilityCheckTest {
    private val connected = MutableStateFlow(false)
    private var enabled = true

    private fun TestScope.accessibilityCheck() = AccessibilityCheck(
        enabled = { enabled },
        connected = connected,
        scope = backgroundScope,
    )

    @Test
    fun `an enabled service still unbound after the bounded wait is blocked without waiting again`() = runTest {
        val check = accessibilityCheck()
        check.check()
        runCurrent()
        assertEquals(AccessibilityCheckStatus.CHECKING, check.status.value)
        advanceTimeBy(AccessibilityCheck.WAIT_MS - 1)
        runCurrent()
        assertEquals(AccessibilityCheckStatus.CHECKING, check.status.value)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(AccessibilityCheckStatus.BLOCKED, check.status.value)
        check.check()
        runCurrent()
        assertEquals(AccessibilityCheckStatus.BLOCKED, check.status.value)
        connected.value = true
        check.check()
        assertEquals(AccessibilityCheckStatus.IDLE, check.status.value)
    }

    @Test
    fun `a service connected at launch or not enabled is never blocked`() = runTest {
        connected.value = true
        val check = accessibilityCheck()
        check.check()
        runCurrent()
        assertEquals(AccessibilityCheckStatus.IDLE, check.status.value)
        connected.value = false
        enabled = false
        check.check()
        advanceTimeBy(AccessibilityCheck.WAIT_MS * 3)
        runCurrent()
        assertEquals(AccessibilityCheckStatus.IDLE, check.status.value)
    }

    @Test
    fun `a connection during the wait ends the check without blocking`() = runTest {
        val check = accessibilityCheck()
        check.check()
        advanceTimeBy(500)
        connected.value = true
        runCurrent()
        assertEquals(AccessibilityCheckStatus.IDLE, check.status.value)
        advanceTimeBy(AccessibilityCheck.WAIT_MS * 2)
        runCurrent()
        assertEquals(AccessibilityCheckStatus.IDLE, check.status.value)
    }

    @Test
    fun `a blocked service disabled then enabled again is checked anew`() = runTest {
        val check = accessibilityCheck()
        check.check()
        advanceTimeBy(AccessibilityCheck.WAIT_MS + 1)
        runCurrent()
        assertEquals(AccessibilityCheckStatus.BLOCKED, check.status.value)
        enabled = false
        check.check()
        assertEquals(AccessibilityCheckStatus.IDLE, check.status.value)
        enabled = true
        check.check()
        runCurrent()
        assertEquals(AccessibilityCheckStatus.CHECKING, check.status.value)
    }

    @Test
    fun `enabled services are read from the secure setting in full or short form`() {
        val cls = "fr.sygix.sygixos.data.HomeAccessibilityService"
        assertTrue(AccessibilityCheck.isEnabled("other/.A:fr.sygix.sygixos/$cls", "fr.sygix.sygixos", cls))
        assertTrue(AccessibilityCheck.isEnabled("fr.sygix.sygixos/.data.HomeAccessibilityService", "fr.sygix.sygixos", cls))
        assertFalse(AccessibilityCheck.isEnabled("fr.sygix.sygixos.perf/$cls", "fr.sygix.sygixos", cls))
        assertFalse(AccessibilityCheck.isEnabled(null, "fr.sygix.sygixos", cls))
        assertFalse(AccessibilityCheck.isEnabled("", "fr.sygix.sygixos", cls))
    }
}
