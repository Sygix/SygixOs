package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.AppCatalog
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

data class Catalog(val dock: List<TvApp>, val grid: List<TvApp>)

class AppCatalogRepository(
    private val appsSource: InstalledAppsSource,
    private val pinnedStore: PinnedAppsStore,
) {
    val catalog: Flow<Catalog> = pinnedStore.pinned.combine(
        kotlinx.coroutines.flow.flow { emit(appsSource.load()) },
    ) { pinned, installed ->
        Catalog(
            dock = AppCatalog.dock(installed, pinned.toList()),
            grid = AppCatalog.grid(installed, pinned.toList()),
        )
    }

    suspend fun togglePin(currentPackages: List<String>, packageName: String) {
        val next = AppCatalog.togglePinned(pinnedStore.pinned.first().toList(), packageName)
        pinnedStore.setPinned(next.toSet())
    }
}
