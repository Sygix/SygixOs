/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

data class ArtworkDimensions(val width: Int, val height: Int) {
    val valid get() = width > 0 && height > 0
}

sealed interface ArtworkPolicy {
    fun accepts(dimensions: ArtworkDimensions): Boolean

    data object Hero : ArtworkPolicy {
        override fun accepts(dimensions: ArtworkDimensions): Boolean {
            if (!dimensions.valid || dimensions.width < dimensions.height) return false
            val scale = minOf(1.0, 1920.0 / dimensions.width, 1080.0 / dimensions.height)
            return dimensions.width * scale >= 640
        }
    }

    data class Shelf(val width: Int) : ArtworkPolicy {
        override fun accepts(dimensions: ArtworkDimensions) = width > 0 && dimensions.valid && dimensions.width >= width && dimensions.width >= dimensions.height
    }

    data class Card(val width: Int) : ArtworkPolicy {
        override fun accepts(dimensions: ArtworkDimensions) = width > 0 && dimensions.valid && dimensions.width >= width
    }
}

object ArtworkAge {
    const val FRESH_MS = 24 * 60 * 60_000L
    const val RETAIN_MS = 7 * FRESH_MS

    fun usable(fetchedAt: Long, now: Long) = fetchedAt > 0 && now >= fetchedAt && now - fetchedAt < RETAIN_MS
    fun fresh(fetchedAt: Long, now: Long) = usable(fetchedAt, now) && now - fetchedAt < FRESH_MS
}
