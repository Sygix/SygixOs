/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramArtwork

object ShelfPosters {

    fun candidates(programs: List<HeroItem>, packageName: String, limit: Int): List<String> =
        programs.asSequence()
            .filter { it.sourcePackage == packageName }
            .mapNotNull { it.imageUrl }
            .distinct()
            .take(limit)
            .toList()

    fun forPackage(programs: List<HeroItem>, validated: Set<String>, packageName: String): List<String> =
        programs.asSequence()
            .filter { it.sourcePackage == packageName }
            .mapNotNull { it.imageUrl }
            .filter { it in validated }
            .distinct()
            .toList()

    fun sources(programs: List<HeroItem>): Map<String, ProgramArtwork> =
        programs.mapNotNull { item -> item.imageUrl?.let { it to item.artwork } }.toMap()
}
