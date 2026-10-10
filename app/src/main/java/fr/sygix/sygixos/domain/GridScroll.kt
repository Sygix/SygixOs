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
}
