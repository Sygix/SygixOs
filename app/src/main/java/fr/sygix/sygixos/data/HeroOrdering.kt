/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.playbackRatio

internal object HeroOrdering {

    fun progressRatio(positionMillis: Long, durationMillis: Long): Float? = playbackRatio(positionMillis, durationMillis)

    fun sort(items: List<HeroItem>): List<HeroItem> =
        items.sortedWith(
            compareByDescending<HeroItem> { it.progress != null }
                .thenByDescending { it.engagement },
        )
}
