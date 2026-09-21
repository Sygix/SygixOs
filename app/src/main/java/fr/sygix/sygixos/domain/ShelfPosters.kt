package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

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
}
