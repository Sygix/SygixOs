/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem

class NatureFallbackProvider : HeroContentProvider {

    override suspend fun load(): List<HeroItem> = CLIPS.mapIndexed { i, url ->
        HeroItem(id = "nature-$i", title = "", videoUrl = url)
    }

    private companion object {
        val CLIPS = listOf(
            "https://videos.pexels.com/video-files/4763824/4763824-uhd_2560_1440_24fps.mp4",
            "https://videos.pexels.com/video-files/9981939/9981939-uhd_2560_1440_30fps.mp4",
            "https://videos.pexels.com/video-files/33945250/14403585_2560_1440_25fps.mp4",
            "https://videos.pexels.com/video-files/6440247/6440247-uhd_2560_1440_30fps.mp4",
            "https://videos.pexels.com/video-files/13874679/13874679-uhd_2560_1440_24fps.mp4",
            "https://videos.pexels.com/video-files/10234380/10234380-uhd_2560_1440_30fps.mp4",
            "https://videos.pexels.com/video-files/33157799/14131511_2560_1440_24fps.mp4",
            "https://videos.pexels.com/video-files/30623330/13107660_2560_1440_24fps.mp4",
            "https://videos.pexels.com/video-files/28787471/12478126_2560_1440_30fps.mp4",
            "https://videos.pexels.com/video-files/15796201/15796201-uhd_2560_1440_30fps.mp4",
            "https://videos.pexels.com/video-files/34445260/14595077_2560_1440_30fps.mp4",
        )
    }
}
