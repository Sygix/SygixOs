package fr.sygix.sygixos.domain

/** Visuels du héro et du Top Shelf : en dessous, l'image ou la vidéo est écartée (upscale flou). */
object VisualQuality {
    const val MIN_WIDTH_PX = 1080
    /** Premiers visuels du héro gardés en mémoire, les autres seulement sur disque. */
    const val HERO_IN_MEMORY = 4
}
