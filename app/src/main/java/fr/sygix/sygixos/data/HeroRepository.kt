/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.domain.HeroFeed

class HeroRepository(
    private val programs: HeroContentProvider,
    private val fallback: HeroContentProvider,
) {
    suspend fun load(): HeroFeed {
        val published = runCatching { programs.load() }.getOrDefault(emptyList())
        if (published.isNotEmpty()) return HeroFeed(published, fromApps = true)
        return HeroFeed(runCatching { fallback.load() }.getOrDefault(emptyList()), fromApps = false)
    }
}
