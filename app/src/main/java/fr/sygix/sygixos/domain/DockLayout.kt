/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object DockLayout {

    fun contentWidth(count: Int, tile: Float, spacing: Float, padding: Float): Float =
        if (count <= 0) 0f else tile * count + spacing * (count - 1) + padding * 2

    fun dockWidth(count: Int, tile: Float, spacing: Float, padding: Float, available: Float): Float =
        minOf(contentWidth(count, tile, spacing, padding), available)

    fun overflows(count: Int, tile: Float, spacing: Float, padding: Float, available: Float): Boolean =
        contentWidth(count, tile, spacing, padding) > available
}
