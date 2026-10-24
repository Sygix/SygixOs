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

    companion object {
        val CLIPS = listOf(
            "https://videos.pexels.com/video-files/4763824/4763824-hd_1920_1080_24fps.mp4",
            "https://videos.pexels.com/video-files/9981939/9981939-hd_1920_1080_30fps.mp4",
            "https://videos.pexels.com/video-files/33945250/14403584_1920_1080_25fps.mp4",
            "https://videos.pexels.com/video-files/6440247/6440247-hd_1920_1080_30fps.mp4",
            "https://videos.pexels.com/video-files/13874679/13874679-hd_1920_1080_24fps.mp4",
            "https://videos.pexels.com/video-files/10234380/10234380-hd_1920_1080_30fps.mp4",
            "https://videos.pexels.com/video-files/33157799/14131510_1920_1080_24fps.mp4",
            "https://videos.pexels.com/video-files/30623330/13107659_1920_1080_24fps.mp4",
            "https://videos.pexels.com/video-files/28787471/12478125_1920_1080_30fps.mp4",
            "https://videos.pexels.com/video-files/15796201/15796201-hd_1920_1080_30fps.mp4",
            "https://videos.pexels.com/video-files/34445260/14595076_1920_1080_30fps.mp4",
        )
    }
}
