package fr.sygix.sygixos.model

data class HeroItem(
    val id: String,
    val title: String,
    /** Vidéo plein écran (aperçu publié par l'app, ou clip nature du fallback). */
    val videoUrl: String? = null,
    /** Visuel paysage de préférence (poster 16:9, sinon vignette, sinon poster portrait). */
    val imageUrl: String? = null,
    val sourcePackage: String? = null,
    /** Nom affichable de l'app source. */
    val sourceLabel: String? = null,
    /** Progression de lecture entre 0f et 1f, null si inconnue. */
    val progress: Float? = null,
    /** URI d'intent publiée par l'app source (ouvre la fiche du contenu). */
    val launchUri: String? = null,
    /** Date d'engagement (epoch ms) pour l'ordre du diaporama. */
    val engagement: Long = 0L,
)
