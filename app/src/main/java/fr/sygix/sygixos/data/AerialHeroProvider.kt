package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem

class AerialHeroProvider : HeroContentProvider {

    override suspend fun load(): List<HeroItem> = AERIALS.mapIndexed { i, url ->
        HeroItem(id = "aerial-$i", title = "", videoUrl = url)
    }

    private companion object {
        val AERIALS = listOf(
            "https://sylvan.apple.com/Aerials/2x/Videos/DB_DeliverShort_A_Croped.mov",
            "https://sylvan.apple.com/Aerials/2x/Videos/A_Multicolored_City_Grids_Advanced.mov",
            "https://sylvan.apple.com/Aerials/2x/Videos/CHR_Aerials_Citygrid_16_1.mov",
        )
    }
}
