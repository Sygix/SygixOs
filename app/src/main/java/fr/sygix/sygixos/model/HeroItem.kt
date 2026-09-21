package fr.sygix.sygixos.model

data class HeroItem(
    val id: String,
    val title: String,
    val videoUrl: String? = null,
    val imageUrl: String? = null,
    val sourcePackage: String? = null,
    /** Progression de lecture entre 0f et 1f, null si inconnue. */
    val progress: Float? = null,
    /** URI d'intent publiée par l'app source (ouvre la fiche du contenu). */
    val launchUri: String? = null,
    /** Date d'engagement (epoch ms) pour l'ordre du carrousel. */
    val engagement: Long = 0L,
)
