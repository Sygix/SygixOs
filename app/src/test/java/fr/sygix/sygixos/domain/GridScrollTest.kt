/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class GridScrollTest {

    private val grid = GridScroll(topMargin = 40f, rowHeight = 90f, rowSpacing = 20f, panelHeight = 344f, margin = 20f)
    private val viewport = 540f

    @Test
    fun `row top accounts for a panel opened on or above the row`() {
        assertEquals(40f, grid.rowTop(0, -1), 0f)
        assertEquals(260f, grid.rowTop(2, -1), 0f)
        assertEquals(604f, grid.rowTop(2, 2), 0f)
        assertEquals(604f, grid.rowTop(2, 0), 0f)
        assertEquals(260f, grid.rowTop(2, 3), 0f)
    }

    @Test
    fun `visible rows do not scroll and a row below the fold aligns to the bottom margin`() {
        val first = grid.next(null, 0, -1, viewport)
        assertEquals(0f, first.scroll, 0f)
        val fourth = grid.next(first, 4, -1, viewport)
        assertEquals(50f, fourth.scroll, 0f)
        val back = grid.next(fourth, 1, -1, viewport)
        assertEquals(50f, back.scroll, 0f)
    }

    @Test
    fun `panel on a top row moves the row as little as possible while keeping the panel below the top margin`() {
        val focused = grid.next(null, 1, -1, viewport)
        val opened = grid.next(focused, 1, 1, viewport)
        assertEquals(110f, opened.scroll, 0f)
        assertEquals(494f, opened.rowTop, 0f)
    }

    @Test
    fun `panel on a deep row keeps the focused row in place while opening and closing`() {
        val focused = grid.next(null, 5, -1, viewport)
        assertEquals(160f, focused.scroll, 0f)
        val opened = grid.next(focused, 5, 5, viewport)
        assertEquals(504f, opened.scroll, 0f)
        assertEquals(opened.rowTop - opened.scroll, focused.rowTop - focused.scroll, 0f)
        val closed = grid.next(opened, 5, -1, viewport)
        assertEquals(160f, closed.scroll, 0f)
    }

    @Test
    fun `block taller than the viewport aligns the panel to the top margin`() {
        val opened = grid.next(null, 1, 1, 400f)
        assertEquals(110f, opened.scroll, 0f)
    }

    @Test
    fun `scroll never goes negative`() {
        val opened = grid.next(null, 0, 0, viewport)
        assertEquals(0f, opened.scroll, 0f)
    }

    @Test
    fun `with an origin the grid starts there and every position is shifted by it`() {
        val origin = 540f
        assertEquals(580f, grid.rowTop(0, -1, origin), 0f)
        val first = grid.next(null, 0, -1, viewport, origin)
        assertEquals(origin, first.scroll, 0f)
        val fourth = grid.next(first, 4, -1, viewport, origin)
        assertEquals(origin + 50f, fourth.scroll, 0f)
        val deep = grid.next(grid.next(null, 5, -1, viewport, origin), 5, 5, viewport, origin)
        assertEquals(origin + 504f, deep.scroll, 0f)
    }

    @Test
    fun `with an origin the scroll never goes above the grid`() {
        val origin = 540f
        val opened = grid.next(null, 0, 0, viewport, origin)
        assertEquals(origin, opened.scroll, 0f)
        val back = grid.next(GridScroll.Anchor(0f, 3, 0f), 0, -1, viewport, origin)
        assertEquals(origin, back.scroll, 0f)
    }
}
