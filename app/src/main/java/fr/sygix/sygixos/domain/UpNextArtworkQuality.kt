/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object UpNextArtworkQuality {
    fun usable(decodedWidth: Int, displayedWidth: Int): Boolean =
        displayedWidth > 0 && decodedWidth.toLong() >= displayedWidth.toLong() * 2

    fun portrait(width: Int, height: Int): Boolean = height > width

    fun placeholderIcon(maxIcon: Float, artworkHeight: Float): Float = minOf(maxIcon, artworkHeight / 2f)
}
