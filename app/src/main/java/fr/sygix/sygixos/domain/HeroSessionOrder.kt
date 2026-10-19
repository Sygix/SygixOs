/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import kotlin.random.Random

interface HeroOrder {
    var displayedId: String?
    fun arrange(items: List<HeroItem>): List<HeroItem>
}

class HeroSessionOrder(private val random: Random = Random.Default) : HeroOrder {
    private var order = emptyList<String>()
    override var displayedId: String? = null

    override fun arrange(items: List<HeroItem>): List<HeroItem> {
        val byId = items.associateBy { it.id }
        val kept = order.filter { it in byId }.toMutableList()
        val added = byId.keys.filter { it !in order }
        if (order.isEmpty()) kept += added.shuffled(random) else {
            for (id in added) {
                val lower = (kept.indexOf(displayedId) + 1).coerceAtLeast(0)
                kept.add(random.nextInt(lower, kept.size + 1), id)
            }
        }
        order = kept
        return kept.mapNotNull(byId::get)
    }
}
