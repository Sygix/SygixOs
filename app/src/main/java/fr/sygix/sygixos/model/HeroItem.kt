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
    val launchUri: String? = null,
    val engagement: Long = 0L,
    val kind: ProgramKind = ProgramKind.FEATURED,
    val season: String? = null,
    val episode: String? = null,
    val durationMillis: Long? = null,
    val positionMillis: Long? = null,
) {
    val progress: Float?
        get() = playbackRatio(positionMillis, durationMillis)
}

fun playbackRatio(positionMillis: Long?, durationMillis: Long?): Float? {
    if (positionMillis == null || durationMillis == null) return null
    return if (durationMillis > 0 && positionMillis in 1 until durationMillis) {
        (positionMillis.toFloat() / durationMillis).coerceIn(0f, 1f)
    } else {
        null
    }
}

enum class ProgramKind { FEATURED, CONTINUE, NEXT, NEW, WATCHLIST, WATCH_NEXT }
