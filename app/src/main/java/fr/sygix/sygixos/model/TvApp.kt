package fr.sygix.sygixos.model

data class TvApp(
    val packageName: String,
    val label: String,
    /** Activity de lancement TV, pour charger sa bannière. */
    val activityName: String? = null,
)
