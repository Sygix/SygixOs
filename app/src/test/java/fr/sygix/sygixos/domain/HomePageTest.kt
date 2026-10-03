/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomePageTest {

    private val origin = 1080f

    @Test
    fun `the page rests on the hero whenever the grid is not active`() {
        assertEquals(0f, HomePage.target(false, null, origin), 0f)
        assertEquals(0f, HomePage.target(false, GridScroll.Anchor(1500f, 5, 1900f), origin), 0f)
    }

    @Test
    fun `the grid starts at its origin and returns to its last position`() {
        assertEquals(origin, HomePage.target(true, null, origin), 0f)
        assertEquals(1500f, HomePage.target(true, GridScroll.Anchor(1500f, 5, 1900f), origin), 0f)
    }

    @Test
    fun `the page never goes above the grid while the grid is active`() {
        assertEquals(origin, HomePage.target(true, GridScroll.Anchor(200f, 0, 240f), origin), 0f)
    }

    @Test
    fun `the first position snaps, zone changes and moves within a zone are told apart`() {
        assertEquals(HomePage.Transition.SNAP, HomePage.transition(null, true))
        assertEquals(HomePage.Transition.SNAP, HomePage.transition(null, false))
        assertEquals(HomePage.Transition.ZONE, HomePage.transition(false, true))
        assertEquals(HomePage.Transition.ZONE, HomePage.transition(true, false))
        assertEquals(HomePage.Transition.SAME_ZONE, HomePage.transition(true, true))
        assertEquals(HomePage.Transition.SAME_ZONE, HomePage.transition(false, false))
    }

    @Test
    fun `the hero stays on screen until the page has scrolled past its full height`() {
        assertTrue(HomePage.heroOnScreen(0, origin))
        assertTrue(HomePage.heroOnScreen(1079, origin))
        assertFalse(HomePage.heroOnScreen(1080, origin))
        assertFalse(HomePage.heroOnScreen(2400, origin))
        assertFalse(HomePage.heroOnScreen(1080, 1080.4f))
    }
}
