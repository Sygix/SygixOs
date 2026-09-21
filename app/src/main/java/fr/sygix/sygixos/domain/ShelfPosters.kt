package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

/** Posters validés d'une app pour le panneau Top Shelf, filtrés côté launcher (le TV Provider refuse toute clause de sélection). */
object ShelfPosters {

    fun forPackage(programs: List<HeroItem>, validated: Set<String>, packageName: String): List<String> =
        programs.asSequence()
            .filter { it.sourcePackage == packageName }
            .mapNotNull { it.imageUrl }
            .filter { it in validated }
            .distinct()
            .toList()
}
