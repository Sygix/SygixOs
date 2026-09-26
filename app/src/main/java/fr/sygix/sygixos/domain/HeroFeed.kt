/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

data class HeroFeed(val items: List<HeroItem>, val fromApps: Boolean) {
    companion object {
        val Empty = HeroFeed(emptyList(), fromApps = false)
    }
}

// Filtrage réactif par apps sources désactivées ; ensemble vide = toutes activées (défaut).
fun filterBySources(feed: HeroFeed, disabled: Set<String>): HeroFeed =
    feed.copy(items = feed.items.filter { it.sourcePackage == null || it.sourcePackage !in disabled })

fun HeroFeed.withSources(disabled: Set<String>, fallback: List<HeroItem>): HeroFeed {
    if (!fromApps) return this
    val filtered = filterBySources(this, disabled)
    return if (filtered.items.isNotEmpty()) filtered else HeroFeed(fallback, fromApps = false)
}
