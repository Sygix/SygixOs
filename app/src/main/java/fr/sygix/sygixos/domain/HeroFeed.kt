/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

data class HeroFeed(val items: List<HeroItem>, val fromApps: Boolean, val errors: List<Throwable> = emptyList(), val shelfItems: List<HeroItem>? = null) {
    val publications: List<HeroItem> get() = (items + shelfItems.orEmpty()).distinctBy { it.id }
    companion object {
        val Empty = HeroFeed(emptyList(), fromApps = false)
    }
}

fun filterBySources(feed: HeroFeed, disabled: Set<String>): HeroFeed =
    feed.copy(items = feed.items.filter { it.tracking != null || it.sourcePackage == null || it.sourcePackage !in disabled },
        shelfItems = feed.shelfItems?.filter { it.sourcePackage == null || it.sourcePackage !in disabled })

fun HeroFeed.withSources(disabled: Set<String>, fallback: List<HeroItem>): HeroFeed {
    if (!fromApps) return this
    val filtered = filterBySources(this, disabled)
    return if (filtered.items.isNotEmpty()) filtered else filtered.copy(items = fallback, fromApps = false)
}
