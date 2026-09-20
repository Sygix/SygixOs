package fr.sygix.sygixos.data

import fr.sygix.sygixos.domain.AppCatalog
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class AppCatalogRepository(
    private val appsSource: InstalledAppsSource,
    private val pinnedStore: PinnedAppsStore,
) {
    val apps: Flow<List<TvApp>> = pinnedStore.pinned.combine(
        kotlinx.coroutines.flow.flow { emit(appsSource.load()) },
    ) { pinned, installed -> AppCatalog.order(installed, pinned.toList()) }

    suspend fun togglePin(currentPackages: List<String>, packageName: String) {
        val next = AppCatalog.togglePinned(pinnedStore.pinned.first().toList(), packageName)
        pinnedStore.setPinned(next.toSet())
    }
}
