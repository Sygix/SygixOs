/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object FocusFallback {

    data class CardVisit(val index: Int, val afterApps: Boolean)

    fun entry(packages: List<String>, focused: String?, index: Int): String? =
        focused?.takeIf { it in packages }
            ?: packages.getOrNull(index.coerceAtMost(packages.lastIndex).coerceAtLeast(0))

    fun refocus(packages: List<String>, focused: String?, index: Int, active: Boolean): String? =
        if (active && focused != null) entry(packages, focused, index) else null

    fun gridEntry(sections: GridSections, focused: String?, index: Int, card: CardVisit?): String? = when {
        card == null || focused in sections.upNextKeys -> entry(sections.focusOrder, focused, index)
        sections.upNextKeys.isNotEmpty() -> entry(sections.upNextKeys, null, card.index)
        else -> entry(sections.apps, null, if (card.afterApps) sections.apps.lastIndex else 0)
    }

    fun track(cards: List<String>, focused: String?, card: CardVisit?): CardVisit? {
        val index = cards.indexOf(focused)
        return if (card == null || index < 0) card else card.copy(index = index)
    }
}
