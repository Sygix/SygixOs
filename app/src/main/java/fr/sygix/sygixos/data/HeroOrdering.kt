/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.HeroItem

internal object HeroOrdering {

    fun progressRatio(positionMillis: Long, durationMillis: Long): Float? =
        if (durationMillis > 0 && positionMillis in 1 until durationMillis) {
            (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
        } else {
            null
        }

    fun sort(items: List<HeroItem>): List<HeroItem> =
        items.sortedWith(
            compareByDescending<HeroItem> { it.progress != null }
                .thenByDescending { it.engagement },
        )
}
