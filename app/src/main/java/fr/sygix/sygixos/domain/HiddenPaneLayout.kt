/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

enum class HiddenGroup { APPS, UPNEXT }

object HiddenPaneLayout {
    const val COLLAPSED_ROWS = 3

    fun shown(count: Int, expanded: Boolean): Int = if (expanded) count else count.coerceAtMost(COLLAPSED_ROWS)

    fun more(count: Int, expanded: Boolean): Int = count - shown(count, expanded)

    fun refocus(order: List<String>, focused: String?, index: Int): String? =
        if (focused == null) null else FocusFallback.entry(order, focused, index)
}
