/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FocusFallbackTest {

    private val apps = listOf("a", "b", "c")

    @Test
    fun `the focused app stays the target while it is present`() {
        assertEquals("b", FocusFallback.entry(apps, "b", 1))
        assertEquals("b", FocusFallback.entry(listOf("b", "a", "c"), "b", 1))
    }

    @Test
    fun `nothing focused yet targets the first tile`() {
        assertEquals("a", FocusFallback.entry(apps, null, 0))
    }

    @Test
    fun `a focused app gone from the middle hands over to the next one`() {
        assertEquals("c", FocusFallback.entry(listOf("a", "c"), "b", 1))
    }

    @Test
    fun `a focused app gone from the end hands over to the previous one`() {
        assertEquals("b", FocusFallback.entry(listOf("a", "b"), "c", 2))
    }

    @Test
    fun `an empty list has no target`() {
        assertNull(FocusFallback.entry(emptyList(), "a", 0))
        assertNull(FocusFallback.entry(emptyList(), null, 3))
    }

    @Test
    fun `refocus targets the neighbour only in an active zone that had a focused tile`() {
        assertEquals("c", FocusFallback.refocus(listOf("a", "c"), "b", 1, active = true))
        assertNull(FocusFallback.refocus(listOf("a", "c"), "b", 1, active = false))
        assertNull(FocusFallback.refocus(apps, null, 0, active = true))
        assertNull(FocusFallback.refocus(emptyList(), "b", 1, active = true))
    }

    private fun sections(cards: List<String>, apps: List<String>, first: Boolean = true) =
        GridSections(cards, apps, upNextShown = cards.isNotEmpty(), upNextFirst = first)

    @Test
    fun `grid entry keeps a present card or tile`() {
        val grid = sections(listOf("c1", "c2"), apps)
        assertEquals("c2", FocusFallback.gridEntry(grid, "c2", 1, FocusFallback.CardVisit(1, afterApps = false)))
        assertEquals("b", FocusFallback.gridEntry(grid, "b", 3, null))
        assertEquals("c1", FocusFallback.gridEntry(grid, null, 0, null))
    }

    @Test
    fun `a vanished card hands over to the next card or the previous one when it was last`() {
        assertEquals("c3", FocusFallback.gridEntry(sections(listOf("c1", "c3"), apps), "c2", 1, FocusFallback.CardVisit(1, afterApps = false)))
        assertEquals("c2", FocusFallback.gridEntry(sections(listOf("c1", "c2"), apps), "c3", 2, FocusFallback.CardVisit(2, afterApps = false)))
        assertEquals("c2", FocusFallback.gridEntry(sections(listOf("c1", "c2"), apps, first = false), "c3", 5, FocusFallback.CardVisit(2, afterApps = true)))
    }

    @Test
    fun `an emptied row falls back to the first app before the apps and the last app after them`() {
        assertEquals("a", FocusFallback.gridEntry(sections(emptyList(), apps), "c2", 1, FocusFallback.CardVisit(1, afterApps = false)))
        assertEquals("c", FocusFallback.gridEntry(sections(emptyList(), apps), "c2", 4, FocusFallback.CardVisit(1, afterApps = true)))
        assertNull(FocusFallback.gridEntry(sections(emptyList(), emptyList()), "c2", 1, FocusFallback.CardVisit(1, afterApps = false)))
    }

    @Test
    fun `a vanished tile keeps the generic linear fallback`() {
        assertEquals("c", FocusFallback.gridEntry(sections(listOf("c1"), listOf("a", "c")), "b", 2, null))
        assertEquals("c1", FocusFallback.gridEntry(sections(listOf("c1"), listOf("a"), first = false), "b", 1, null))
    }

    @Test
    fun `a card visit follows its card when the row is reordered`() {
        val visit = FocusFallback.CardVisit(2, afterApps = false)
        assertEquals(visit.copy(index = 0), FocusFallback.track(listOf("c3", "c1", "c2"), "c3", visit))
        assertEquals(visit, FocusFallback.track(listOf("c1", "c2"), "c3", visit))
        assertNull(FocusFallback.track(listOf("c1"), "c1", null))
    }
}
