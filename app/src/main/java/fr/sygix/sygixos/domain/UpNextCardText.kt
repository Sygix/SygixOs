/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem

object UpNextCardText {
    fun episodeNumbers(item: UpNextItem): Pair<String, String>? {
        if (item.type != UpNextContentType.EPISODE || item.titleFromEpisode) return null
        val season = item.season ?: return null
        val episode = item.episode ?: return null
        return season.padStart(2, '0') to episode.padStart(2, '0')
    }

    fun episodeLine(item: UpNextItem, number: String?, separator: String): String? {
        if (item.type != UpNextContentType.EPISODE || item.titleFromEpisode) return null
        return listOfNotNull(number, item.episodeTitle).takeIf { it.isNotEmpty() }?.joinToString(separator)
    }

    fun progress(item: UpNextItem): Float? = item.progress.takeIf { item.watchNextType == ProgramKind.CONTINUE }
}
