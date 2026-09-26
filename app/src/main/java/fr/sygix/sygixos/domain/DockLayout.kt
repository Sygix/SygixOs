/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object DockLayout {

    fun tileWidth(available: Float, count: Int, spacing: Float, columns: Int): Float {
        if (count <= 0) return 0f
        val gridTile = (available - spacing * (columns - 1)) / columns
        val fitted = (available - spacing * (count - 1)) / count
        return minOf(gridTile, fitted)
    }

    fun dockWidth(tileWidth: Float, count: Int, spacing: Float, padding: Float): Float =
        tileWidth * count + spacing * (count - 1) + padding * 2
}
