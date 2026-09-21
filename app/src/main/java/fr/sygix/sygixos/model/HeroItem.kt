package fr.sygix.sygixos.model

data class HeroItem(
    val id: String,
    val title: String,
    val videoUrl: String? = null,
    val imageUrl: String? = null,
    val sourcePackage: String? = null,
)
