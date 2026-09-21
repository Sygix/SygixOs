package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem

/**
 * Fond vidéo de secours : clips nature libres de droits (licence Pexels),
 * paysage 2560x1440 H.264, 24-30 fps, lus en streaming. L'identifiant Pexels
 * permet de re-résoudre une URL via https://www.pexels.com/download/video/<id>/?h=1440&w=2560
 */
class NatureFallbackProvider : HeroContentProvider {

    override suspend fun load(): List<HeroItem> = CLIPS.mapIndexed { i, url ->
        HeroItem(id = "nature-$i", title = "", videoUrl = url)
    }

    private companion object {
        val CLIPS = listOf(
            // 4763824 — survol d'un sommet
            "https://videos.pexels.com/video-files/4763824/4763824-uhd_2560_1440_24fps.mp4",
            // 9981939 — côte rocheuse vue du ciel
            "https://videos.pexels.com/video-files/9981939/9981939-uhd_2560_1440_30fps.mp4",
            // 33945250 — paysage verdoyant
            "https://videos.pexels.com/video-files/33945250/14403585_2560_1440_25fps.mp4",
            // 6440247 — littoral vu d'en haut
            "https://videos.pexels.com/video-files/6440247/6440247-uhd_2560_1440_30fps.mp4",
            // 13874679 — forêt profonde
            "https://videos.pexels.com/video-files/13874679/13874679-uhd_2560_1440_24fps.mp4",
            // 10234380 — falaises et formations rocheuses
            "https://videos.pexels.com/video-files/10234380/10234380-uhd_2560_1440_30fps.mp4",
            // 33157799 — forêt et chaîne de montagnes
            "https://videos.pexels.com/video-files/33157799/14131511_2560_1440_24fps.mp4",
            // 30623330 — vagues sur les rochers
            "https://videos.pexels.com/video-files/30623330/13107660_2560_1440_24fps.mp4",
            // 28787471 — forêt d'automne
            "https://videos.pexels.com/video-files/28787471/12478126_2560_1440_30fps.mp4",
            // 15796201 — océan et rivage
            "https://videos.pexels.com/video-files/15796201/15796201-uhd_2560_1440_30fps.mp4",
            // 34445260 — campagne roumaine
            "https://videos.pexels.com/video-files/34445260/14595077_2560_1440_30fps.mp4",
        )
    }
}
