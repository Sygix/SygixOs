package fr.sygix.sygixos.domain

/** Largeur des tuiles du dock : celle de la grille jusqu'à `columns` apps, réduite au-delà pour tenir dans `available`. */
object DockLayout {

    fun tileWidth(available: Float, count: Int, spacing: Float, columns: Int): Float {
        if (count <= 0) return 0f
        val gridTile = (available - spacing * (columns - 1)) / columns
        val fitted = (available - spacing * (count - 1)) / count
        return minOf(gridTile, fitted)
    }

    fun dockWidth(tileWidth: Float, count: Int, spacing: Float, padding: Float): Float =
        tileWidth * count + spacing * (count - 1) + padding * 2
}
