package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem

/** Contenu du héro : programmes des apps installées, ou fallback vidéo. */
data class HeroFeed(val items: List<HeroItem>, val fromApps: Boolean) {
    companion object {
        val Empty = HeroFeed(emptyList(), fromApps = false)
    }
}
