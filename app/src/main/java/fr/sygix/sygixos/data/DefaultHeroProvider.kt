package fr.sygix.sygixos.data

import android.content.Context
import fr.sygix.sygixos.domain.HeroContentProvider
import fr.sygix.sygixos.model.HeroItem

/**
 * Chaîne de sources du héro : d'abord ce que les apps installées publient
 * dans le TV Provider système, fallback vidéos aériennes si rien.
 */
class DefaultHeroProvider(context: Context) : HeroContentProvider {

    private val tvProvider = TvProviderHeroSource(context.applicationContext)
    private val aerial = AerialHeroProvider()

    override suspend fun load(): List<HeroItem> =
        runCatching { tvProvider.load() }.getOrDefault(emptyList())
            .ifEmpty { runCatching { aerial.load() }.getOrDefault(emptyList()) }
}
