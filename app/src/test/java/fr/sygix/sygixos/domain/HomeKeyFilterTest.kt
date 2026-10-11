/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeKeyFilterTest {
    private val opened = mutableListOf<Boolean>()
    private var foreground = true
    private var connected = true
    private val filter = HomeKeyFilter(foreground = { foreground }, openHome = { opened += it })

    private fun down(repeat: Int = 0) = filter.filter(HomeKeyEvent(down = true, repeatCount = repeat), connected)

    private fun up(canceled: Boolean = false) = filter.filter(HomeKeyEvent(down = false, canceled = canceled), connected)

    @Test
    fun `a short press is consumed and opens the home once on release without delay`() {
        assertTrue(down())
        assertTrue(opened.isEmpty())
        assertTrue(up())
        assertEquals(listOf(true), opened)
    }

    @Test
    fun `a long press with repeats is consumed entirely and opens the home once on release`() {
        assertTrue(down())
        repeat(5) { assertTrue(down(repeat = it + 1)) }
        assertTrue(opened.isEmpty())
        assertTrue(up())
        assertEquals(listOf(true), opened)
    }

    @Test
    fun `the foreground state is captured when the key goes down`() {
        down()
        foreground = false
        up()
        down()
        foreground = true
        up()
        assertEquals(listOf(true, false), opened)
    }

    @Test
    fun `a canceled release is consumed without opening anything`() {
        assertTrue(down())
        assertTrue(up(canceled = true))
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `other keys and a disconnected service are never consumed`() {
        assertFalse(filter.filter(HomeKeyEvent(home = false, down = true), true))
        assertFalse(filter.filter(HomeKeyEvent(home = false, down = false), true))
        connected = false
        assertFalse(down())
        assertFalse(down(repeat = 1))
        assertFalse(up())
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `a release whose press reached the system stays with the system and a failing opening never escapes`() {
        assertFalse(up())
        filter.reset()
        val failing = HomeKeyFilter({ true }) { throw SecurityException() }
        assertTrue(failing.filter(HomeKeyEvent(down = true), true))
        assertTrue(failing.filter(HomeKeyEvent(down = false), true))
        assertTrue(opened.isEmpty())
    }
}
