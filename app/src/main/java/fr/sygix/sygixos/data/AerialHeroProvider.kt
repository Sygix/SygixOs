package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem

class AerialHeroProvider : HeroContentProvider {

    override suspend fun load(): List<HeroItem> = AERIALS.mapIndexed { i, url ->
        HeroItem(id = "aerial-$i", title = "", videoUrl = url)
    }

    private companion object {
        // Aériens Apple (tvOS 17+), H.264 1080p, HTTPS — CDN sylvan/itunes-assets
        // (l'ancien chemin Aerials/2x/ est injoignable depuis certaines box/TV)
        val AERIALS = listOf(
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/Y009_C015_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/Y004_C015_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_A001_C004_1207W5_v23_SDR_FINAL_20180706_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_DB_D002_C003_PSNK_v04_SDR_PS_20180914_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_A007_C017_01156B_v02_SDR_PS_20180925_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_HK_H004_C008_PSNK_v19_SDR_PS_20180914_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_DB_D001_C005_COMP_PSNK_v12_SDR_PS_20180912_SDR_2K_AVC.mov",
            "https://sylvan.apple.com/itunes-assets/Aerials116/v4/97/e9/06/97e90616-1227-c670-6118-bd5ae991b43b/comp_DB_D008_C010_PSNK_v21_SDR_PS_20180914_F0F16157_SDR_2K_AVC.mov",
        )
    }
}
