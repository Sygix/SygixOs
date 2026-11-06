/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.util.Locale

object RegularEpisode {
    private val Placeholders = setOf("tba", "tbd")

    fun eligible(season: Int?, title: String?, special: Boolean = false): Boolean =
        !special && season != 0 && title?.trim()?.lowercase(Locale.ROOT) !in Placeholders

    fun eligible(content: TrackingContent): Boolean = content.identity.type != TrackingType.EPISODE || eligible(content.season, content.episodeTitle)
}
