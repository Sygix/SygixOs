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
) {
    data class Anchor(val scroll: Float, val row: Int, val rowTop: Float)

    fun rowTop(row: Int, openRow: Int): Float =
        topMargin + row * (rowHeight + rowSpacing) + if (openRow in 0..row) panelHeight else 0f

    fun next(anchor: Anchor?, row: Int, openRow: Int, viewport: Float): Anchor {
        val top = rowTop(row, openRow)
        val blockTop = if (openRow == row) top - panelHeight else top
        val minScroll = top + rowHeight + margin - viewport
        val maxScroll = blockTop - topMargin
        val preferred = when {
            anchor == null -> 0f
            anchor.row == row -> anchor.scroll + top - anchor.rowTop
            else -> anchor.scroll
        }
        val fitted = if (minScroll > maxScroll) maxScroll else preferred.coerceIn(minScroll, maxScroll)
        return Anchor(fitted.coerceAtLeast(0f), row, top)
    }
}
