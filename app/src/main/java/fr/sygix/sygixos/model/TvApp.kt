package fr.sygix.sygixos.model

data class TvApp(
    val packageName: String,
    val label: String,
    val activityName: String? = null,
)
