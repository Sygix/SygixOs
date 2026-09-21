package fr.sygix.sygixos.domain

/** Visuels du héro et du Top Shelf : en dessous, l'image ou la vidéo est écartée (upscale flou). */
object VisualQuality {
    const val MIN_WIDTH_PX = 1080
    /** Premiers visuels du héro gardés en mémoire, les autres seulement sur disque. */
    const val HERO_IN_MEMORY = 4
    /** Le TV Provider peut publier des centaines de programmes : on ne valide que le début du héro... */
    const val HERO_VALIDATED = 24
    /** ...et quelques affiches par app, à la demande, quand le focus s'y pose. */
    const val SHELF_VALIDATED_PER_APP = 8
}
