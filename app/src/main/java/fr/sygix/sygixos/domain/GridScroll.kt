/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

class GridScroll(
    private val topMargin: Float,
    private val rowHeight: Float,
    private val rowSpacing: Float,
    private val panelHeight: Float,
    private val margin: Float,
    private val rows: List<RowGeometry> = emptyList(),
) {
    data class RowGeometry(val height: Float, val titleHeight: Float = 0f)
    data class Anchor(val scroll: Float, val row: Int, val rowTop: Float)

    fun rowTop(row: Int, openRow: Int, origin: Float = 0f): Float =
        origin + topMargin +
            (if (rows.isEmpty()) row * (rowHeight + rowSpacing) else rows.take(row).sumOf { (it.height + it.titleHeight + rowSpacing).toDouble() }.toFloat() + rows[row].titleHeight) +
            if (openRow in 0..row) panelHeight else 0f

    fun rowAt(y: Float, openRow: Int, origin: Float = 0f): Int {
        var top = origin + topMargin
        for (row in 1 until rows.size) {
            top += rows[row - 1].height + rows[row - 1].titleHeight + rowSpacing
            if (top + (if (openRow in 0 until row) panelHeight else 0f) > y) return row - 1
        }
        return rows.lastIndex.coerceAtLeast(0)
    }

    fun window(scroll: Float, anchor: Anchor?, openRow: Int, viewport: Float, origin: Float = 0f): IntRange {
        if (rows.size <= EAGER_ROWS) return rows.indices
        val target = anchor?.scroll ?: scroll
        val first = rowAt(minOf(scroll, target), openRow, origin) - PREFETCH_ROWS
        val last = rowAt(maxOf(scroll, target) + viewport, openRow, origin) + PREFETCH_ROWS
        return first.coerceAtLeast(0)..last.coerceAtMost(rows.lastIndex)
    }

    fun next(anchor: Anchor?, row: Int, openRow: Int, viewport: Float, origin: Float = 0f): Anchor {
        val top = rowTop(row, openRow, origin)
        val blockTop = top - (if (openRow == row) panelHeight else 0f) - (rows.getOrNull(row)?.titleHeight ?: 0f)
        val minScroll = top + (rows.getOrNull(row)?.height ?: rowHeight) + margin - viewport
        val maxScroll = blockTop - topMargin
        val preferred = when {
            anchor == null -> origin
            anchor.row == row -> anchor.scroll + top - anchor.rowTop
            else -> anchor.scroll
        }
        val fitted = if (minScroll > maxScroll) maxScroll else preferred.coerceIn(minScroll, maxScroll)
        return Anchor(fitted.coerceAtLeast(origin), row, top)
    }

    companion object {
        const val EAGER_ROWS = 12
        const val PREFETCH_ROWS = 2
    }
}
