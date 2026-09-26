/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object FocusFallback {

    fun entry(packages: List<String>, focused: String?, index: Int): String? =
        focused?.takeIf { it in packages }
            ?: packages.getOrNull(index.coerceAtMost(packages.lastIndex).coerceAtLeast(0))

    fun refocus(packages: List<String>, focused: String?, index: Int, active: Boolean): String? =
        if (active && focused != null) entry(packages, focused, index) else null
}
