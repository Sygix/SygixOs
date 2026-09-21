package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.AppCatalog
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

data class Catalog(val dock: List<TvApp>, val grid: List<TvApp>)

class AppCatalogRepository(
    private val appsSource: InstalledAppsSource,
    private val prefs: LauncherPrefs,
) {
    private val installed = MutableStateFlow<List<TvApp>?>(null)

    val catalog: Flow<Catalog> = combine(prefs.pinned, prefs.gridOrder, installed.filterNotNull()) { pinned, order, apps ->
        Catalog(
            dock = AppCatalog.dock(apps, pinned.toList()),
            grid = AppCatalog.grid(apps, order),
        )
    }

    /** Relit les apps installées (démarrage, retour au launcher). */
    suspend fun refreshApps() {
        installed.value = withContext(Dispatchers.IO) { runCatching { appsSource.load() }.getOrDefault(emptyList()) }
    }

    suspend fun togglePin(packageName: String) {
        val next = AppCatalog.togglePinned(prefs.pinned.first().toList(), packageName)
        prefs.setPinned(next.toSet())
    }

    /** L'ordre persisté est toujours la grille complète telle qu'affichée. */
    suspend fun moveInGrid(packageName: String, delta: Int) {
        val apps = installed.value ?: return
        val current = AppCatalog.grid(apps, prefs.gridOrder.first()).map { it.packageName }
        prefs.setGridOrder(AppCatalog.move(current, packageName, delta))
    }

    suspend fun setGridOrder(order: List<String>) = prefs.setGridOrder(order)
}
