/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

enum class TrackingProviderKind(val id: String, val displayName: String) {
    BETASERIES("betaseries", "BetaSeries"),
    TRAKT("trakt", "Trakt");

    companion object {
        fun fromId(id: String): TrackingProviderKind? = entries.firstOrNull { it.id == id }
        fun label(id: String): String = fromId(id)?.displayName ?: id
    }
}
