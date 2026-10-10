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
    private val filter = HomeKeyFilter(longPressMs = 500, foreground = { foreground }, openHome = { opened += it })

    private fun down(at: Long, repeat: Int = 0, longPress: Boolean = false) =
        filter.filter(HomeKeyEvent(down = true, repeatCount = repeat, longPress = longPress, eventTime = at))

    private fun up(at: Long, canceled: Boolean = false) = filter.filter(HomeKeyEvent(down = false, canceled = canceled, eventTime = at))

    @Test
    fun `short press opens the home after release without consuming any event`() {
        assertFalse(down(0))
        assertTrue(opened.isEmpty())
        assertFalse(up(120))
        assertEquals(listOf(true), opened)
    }

    @Test
    fun `foreground state is captured when the key goes down`() {
        foreground = true
        down(0)
        foreground = false
        up(100)
        assertEquals(listOf(true), opened)
        down(1_000)
        foreground = true
        up(1_100)
        assertEquals(listOf(true, false), opened)
    }

    @Test
    fun `long press with repeats reaches the system untouched`() {
        assertFalse(down(0))
        assertFalse(down(400, repeat = 1))
        assertFalse(down(450, repeat = 2, longPress = true))
        assertFalse(up(900))
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `press held past the long press timeout never opens the home`() {
        down(0)
        assertFalse(up(500))
        down(1_000, longPress = true)
        up(1_100)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `canceled release or release without its down never opens the home`() {
        down(0)
        up(100, canceled = true)
        up(300)
        down(700, repeat = 3)
        up(800)
        assertTrue(opened.isEmpty())
    }

    @Test
    fun `reset forgets a pending press and failing opening never escapes`() {
        down(0)
        filter.reset()
        up(100)
        assertTrue(opened.isEmpty())
        val failing = HomeKeyFilter(500, { true }) { throw SecurityException() }
        failing.filter(HomeKeyEvent(down = true, eventTime = 0))
        assertFalse(failing.filter(HomeKeyEvent(down = false, eventTime = 50)))
    }
}
