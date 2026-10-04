/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramKind

data class HeroHeader(val kind: ProgramKind, val app: String?)

data class HeroDetails(val season: String?, val episode: String?, val minutes: Int?)

data class HoursMinutes(val hours: Int, val minutes: Int)

object HeroCaption {

    private const val MINUTE_MS = 60_000L

    fun header(item: HeroItem): HeroHeader? {
        val app = item.sourceLabel?.takeIf { it.isNotBlank() }
        val qualified = item.kind != ProgramKind.FEATURED && item.kind != ProgramKind.WATCH_NEXT
        if (!qualified && app == null) return null
        return HeroHeader(item.kind.takeIf { qualified } ?: ProgramKind.FEATURED, app)
    }

    fun details(item: HeroItem): HeroDetails? {
        val season = item.season?.trim()?.takeIf { it.isNotEmpty() }
        val episode = item.episode?.trim()?.takeIf { it.isNotEmpty() }
        val minutes = item.durationMillis?.takeIf { it > 0 }?.let(::durationMinutes)
        if (season == null && episode == null && minutes == null) return null
        return HeroDetails(season, episode, minutes)
    }

    fun remainingMinutes(item: HeroItem): Int? {
        if (item.progress == null) return null
        val duration = item.durationMillis ?: return null
        val position = item.positionMillis ?: return null
        val left = duration - position
        if (left <= 0) return null
        return ((left + MINUTE_MS - 1) / MINUTE_MS).toInt()
    }

    fun durationMinutes(millis: Long): Int = maxOf(1L, (millis + MINUTE_MS / 2) / MINUTE_MS).toInt()

    fun split(minutes: Int): HoursMinutes = HoursMinutes(minutes / 60, minutes % 60)
}
