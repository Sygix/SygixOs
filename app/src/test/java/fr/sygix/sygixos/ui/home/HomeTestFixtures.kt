package fr.sygix.sygixos.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalContext
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp

internal fun app(pkg: String, label: String) = TvApp(packageName = pkg, label = label)

internal fun heroItem(
    id: String,
    title: String,
    imageUrl: String? = null,
    sourcePackage: String? = null,
    progress: Float? = null,
) = HeroItem(
    id = id,
    title = title,
    imageUrl = imageUrl,
    sourcePackage = sourcePackage,
    progress = progress,
)

internal fun catalogOf(dock: List<TvApp>, grid: List<TvApp>) = Catalog(dock = dock, grid = grid)

internal fun heroStateOf(
    items: List<HeroItem>,
    validated: Set<String> = emptySet(),
    checked: Set<String> = emptySet(),
    fromApps: Boolean = true,
) = HeroState(items = items, fromApps = fromApps, loading = false, validated = validated, checked = checked)

@Composable
internal fun TestHome(
    catalog: Catalog,
    hero: HeroState,
    initialZone: Zone = Zone.HERO,
    onTogglePin: (TvApp) -> Unit = {},
) {
    val artwork = AppArtworkSource(LocalContext.current.packageManager)
    CompositionLocalProvider(LocalAppArtwork provides artwork) {
        MaterialTheme {
            LauncherHome(
                catalog = catalog,
                hero = hero,
                onTogglePin = onTogglePin,
                initialZone = initialZone,
                glassBlur = false,
            )
        }
    }
}
