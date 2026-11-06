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

    @Test
    fun `the last card aligns its slot on the right grid margin and other cards keep the zoom inset`() {
        assertEquals(96, UpNextScroll.endInset(7, 8, 16, 96))
        assertEquals(16, UpNextScroll.endInset(6, 8, 16, 96))
        val viewportEnd = 1920 - 96
        val delta = UpNextScroll.revealDelta(1428, 396, viewportEnd, 96)
        assertEquals(96, delta)
        assertEquals(1920 - 96, 96 + 1428 - delta + 396)
    }

    @Test
    fun `a row that fits between the margins never scrolls`() {
        val viewportEnd = 1920 - 96
        listOf(0, 444, 888).forEachIndexed { index, offset ->
            val inset = UpNextScroll.endInset(index, 3, 16, 96)
            assertEquals(0, UpNextScroll.revealDelta(offset, 396, viewportEnd, inset))
        }
    }

    @Test
    fun `reveal distance is measured from a laid card and is zero once the row is empty`() {
        val viewport = UpNextScroll.Viewport(end = 1824, spacing = 24, endPadding = 96)
        assertEquals(0, UpNextScroll.revealDistance(3, 6, emptyList(), viewport, 1.08f))
        val laid = listOf(UpNextScroll.Laid(0, 96, 396), UpNextScroll.Laid(1, 516, 396))
        assertEquals(96 + 4 * 420 + 396 + 16 - 1824, UpNextScroll.revealDistance(4, 6, laid, viewport, 1.08f))
        assertEquals(96 + 5 * 420 + 396 + 96 - 1824, UpNextScroll.revealDistance(5, 6, laid, viewport, 1.08f))
        assertEquals(-20, UpNextScroll.revealDistance(1, 6, listOf(UpNextScroll.Laid(1, -20, 396)), viewport, 1.08f))
    }
}
