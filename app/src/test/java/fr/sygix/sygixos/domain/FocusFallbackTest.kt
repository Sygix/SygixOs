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
}
