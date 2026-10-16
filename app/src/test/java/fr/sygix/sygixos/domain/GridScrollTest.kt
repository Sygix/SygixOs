/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    private val titled = GridScroll(
        topMargin = 40f, rowHeight = 86f, rowSpacing = 32f, panelHeight = 344f, margin = 32f,
        rows = listOf(GridScroll.RowGeometry(112f, 40f), GridScroll.RowGeometry(86f, 40f), GridScroll.RowGeometry(86f), GridScroll.RowGeometry(86f)),
    )
    private val titledAfter = GridScroll(
        topMargin = 40f, rowHeight = 86f, rowSpacing = 32f, panelHeight = 344f, margin = 32f,
        rows = listOf(GridScroll.RowGeometry(86f, 40f), GridScroll.RowGeometry(86f), GridScroll.RowGeometry(86f), GridScroll.RowGeometry(86f), GridScroll.RowGeometry(112f, 40f)),
    )

    @Test
    fun `row tops use real heights and section titles in both positions`() {
        assertEquals(80f, titled.rowTop(0, -1), 0f)
        assertEquals(264f, titled.rowTop(1, -1), 0f)
        assertEquals(382f, titled.rowTop(2, -1), 0f)
        assertEquals(80f, titledAfter.rowTop(0, -1), 0f)
        assertEquals(198f, titledAfter.rowTop(1, -1), 0f)
        assertEquals(592f, titledAfter.rowTop(4, -1), 0f)
    }

    @Test
    fun `first entry puts the top of the first title on the top margin`() {
        val origin = 540f
        for (grid in listOf(titled, titledAfter)) {
            val first = grid.next(null, 0, -1, viewport, origin)
            assertEquals(origin, first.scroll, 0f)
            assertEquals(40f, first.rowTop - 40f - first.scroll, 0f)
        }
    }

    @Test
    fun `focused block is title then panel then row with the title on the top margin`() {
        val first = titled.next(null, 0, -1, viewport)
        val apps = titled.next(first, 1, -1, viewport)
        assertEquals(0f, apps.scroll, 0f)
        val opened = titled.next(apps, 1, 1, viewport)
        val panelTop = opened.rowTop - 344f
        val titleTop = panelTop - 40f
        assertEquals(40f, titleTop - opened.scroll, 0f)
        assertTrue(titleTop < panelTop && panelTop < opened.rowTop)
    }

    @Test
    fun `up next after the apps aligns its bottom on the bottom margin with its title visible`() {
        val first = titledAfter.next(null, 0, -1, viewport)
        val row = titledAfter.next(first, 4, -1, viewport)
        assertEquals(196f, row.scroll, 0f)
        assertEquals(508f, row.rowTop + 112f - row.scroll, 0f)
        assertTrue(row.rowTop - 40f - row.scroll >= 40f)
        val back = titledAfter.next(row, 3, -1, viewport)
        assertEquals(196f, back.scroll, 0f)
    }

    private val deep = GridScroll(
        topMargin = 40f, rowHeight = 86f, rowSpacing = 32f, panelHeight = 344f, margin = 32f,
        rows = listOf(GridScroll.RowGeometry(86f, 40f)) + List(39) { GridScroll.RowGeometry(86f) },
    )

    @Test
    fun `row at a position is the inverse of row top and accounts for the open panel`() {
        for (row in listOf(0, 1, 7, 39)) {
            assertEquals(row, deep.rowAt(deep.rowTop(row, -1), -1))
            assertEquals(row, deep.rowAt(deep.rowTop(row, -1) - (if (row == 0) 40f else 0f), -1))
            assertEquals(row, deep.rowAt(deep.rowTop(row, 5), 5))
        }
        assertEquals(0, deep.rowAt(-500f, -1))
        assertEquals(39, deep.rowAt(100_000f, -1))
        assertEquals(5, deep.rowAt(deep.rowTop(5, 5) - 344f, 5))
        assertEquals(4, deep.rowAt(deep.rowTop(5, 5) - 345f, 5))
        assertEquals(1, titled.rowAt(titled.rowTop(1, -1) - 40f, -1))
        assertEquals(0, titled.rowAt(titled.rowTop(1, -1) - 41f, -1))
    }

    @Test
    fun `every row is composed up to the eager threshold`() {
        val rows = List(GridScroll.EAGER_ROWS) { GridScroll.RowGeometry(86f) }
        val eager = GridScroll(40f, 86f, 32f, 344f, 32f, rows)
        assertEquals(rows.indices, eager.window(0f, GridScroll.Anchor(100_000f, 11, 0f), -1, viewport))
        assertEquals(0 until 4, titled.window(5_000f, null, -1, viewport))
    }

    @Test
    fun `the window covers the screen at the current and target positions plus the prefetch rows`() {
        assertEquals(0..5, deep.window(0f, null, -1, viewport))
        val target = deep.next(null, 20, -1, viewport)
        assertEquals(2018f, target.scroll, 0f)
        assertEquals(0..23, deep.window(0f, target, -1, viewport))
        assertEquals(14..23, deep.window(target.scroll, target, -1, viewport))
        assertEquals(39, deep.window(10_000f, null, -1, viewport).last)
    }
}
