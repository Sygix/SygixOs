package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

/** Posters d'une app pour le panneau Top Shelf, filtrés côté launcher (le TV Provider refuse toute clause de sélection). */
object ShelfPosters {

    fun forPackage(programs: List<HeroItem>, packageName: String): List<String> =
        programs.asSequence()
            .filter { it.sourcePackage == packageName }
            .mapNotNull { it.imageUrl }
            .distinct()
            .toList()
}
