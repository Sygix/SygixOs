/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

data class HeroFeed(val items: List<HeroItem>, val fromApps: Boolean) {
    companion object {
        val Empty = HeroFeed(emptyList(), fromApps = false)
    }
}
