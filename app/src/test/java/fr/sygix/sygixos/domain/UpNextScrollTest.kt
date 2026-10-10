/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UpNextScrollTest {
    @Test
    fun `left and right move one card without wrapping at the edges`() {
        assertEquals(1, UpNextScroll.neighbor(0, 1, 5))
        assertEquals(0, UpNextScroll.neighbor(0, -1, 5))
        assertEquals(4, UpNextScroll.neighbor(4, 1, 5))
        assertEquals(3, UpNextScroll.neighbor(4, -1, 5))
        assertEquals(0, UpNextScroll.neighbor(0, 1, 1))
    }

    @Test
    fun `reveal scrolls just enough to bring the card between the left margin and the right edge`() {
        assertEquals(0, UpNextScroll.revealDelta(100, 396, 1824))
        assertEquals(0, UpNextScroll.revealDelta(1428, 396, 1824))
        assertEquals(36, UpNextScroll.revealDelta(1464, 396, 1824))
        assertEquals(-20, UpNextScroll.revealDelta(-20, 396, 1824))
    }

    @Test
    fun `offset of an unlaid card follows from a visible anchor`() {
        assertEquals(1428 + 444, UpNextScroll.offsetOf(6, 5, 1428, 396, 48))
        assertEquals(-444, UpNextScroll.offsetOf(1, 2, 0, 396, 48))
        assertEquals(36 + 444, UpNextScroll.revealDelta(UpNextScroll.offsetOf(6, 5, 1464, 396, 48), 396, 1824))
    }

    @Test
    fun `the focused card keeps its focus zoom overflow inside the right edge`() {
        assertEquals(16, UpNextScroll.focusOverflow(396, 1.08f))
        assertEquals(0, UpNextScroll.focusOverflow(396, 1f))
        assertEquals(36 + 16, UpNextScroll.revealDelta(1464, 396, 1824, 16))
        assertEquals(0, UpNextScroll.revealDelta(1412, 396, 1824, 16))
        assertEquals(-20, UpNextScroll.revealDelta(-20, 396, 1824, 16))
    }
}
