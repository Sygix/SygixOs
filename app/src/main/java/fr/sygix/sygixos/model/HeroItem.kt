/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.model

data class HeroItem(
    val id: String,
    val title: String,
    val videoUrl: String? = null,
    val imageUrl: String? = null,
    val sourcePackage: String? = null,
    val sourceLabel: String? = null,
    val progress: Float? = null,
    val launchUri: String? = null,
    val engagement: Long = 0L,
    val kind: ProgramKind = ProgramKind.FEATURED,
    val season: String? = null,
    val episode: String? = null,
    val durationMillis: Long? = null,
    val positionMillis: Long? = null,
)

enum class ProgramKind { FEATURED, CONTINUE, NEXT, NEW, WATCHLIST, WATCH_NEXT }
