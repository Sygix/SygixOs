/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object ListReveal {

    fun indexToReveal(order: List<String>, focused: String?, visibleKeys: Collection<Any?>, leadingItems: Int): Int? {
        val position = focused?.let(order::indexOf)?.takeIf { it >= 0 } ?: return null
        return if (focused in visibleKeys) null else position + leadingItems
    }
}
