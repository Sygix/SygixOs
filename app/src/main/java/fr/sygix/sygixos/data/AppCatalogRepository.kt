/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

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

    val hidden: Flow<Set<String>> = prefs.hidden

    val disabledSources: Flow<Set<String>> = prefs.disabledSources

    val allApps: Flow<List<TvApp>> = installed.filterNotNull()

    val catalog: Flow<Catalog> = combine(prefs.pinned, prefs.gridOrder, prefs.hidden, installed.filterNotNull()) { pinned, order, hidden, apps ->
        val visible = apps.filter { it.packageName !in hidden }
        Catalog(
            dock = AppCatalog.dock(visible, pinned.toList()),
            grid = AppCatalog.grid(visible, order),
        )
    }

    suspend fun refreshApps() {
        if (installed.value == null) {
            prefs.cachedApps.first().takeIf { it.isNotEmpty() }?.let { installed.value = it }
        }
        val fresh = withContext(Dispatchers.IO) { runCatching { appsSource.load() }.getOrDefault(emptyList()) }
        if (fresh.isNotEmpty() || installed.value == null) {
            installed.value = fresh
            prefs.setCachedApps(fresh)
        }
    }

    suspend fun togglePin(packageName: String) {
        // Un seul edit : lecture et écriture dans la même transaction DataStore.
        prefs.updatePinned { pinned -> AppCatalog.togglePinned(pinned.toList(), packageName).toSet() }
    }

    suspend fun moveInGrid(packageName: String, delta: Int) {
        val apps = installed.value ?: return
        prefs.updateGridOrder { current, hidden ->
            val visibleOrder = AppCatalog.grid(apps.filter { it.packageName !in hidden }, current).map { it.packageName }
            withHiddenKept(apps, current, hidden, AppCatalog.move(visibleOrder, packageName, delta))
        }
    }

    suspend fun setGridOrder(order: List<String>) {
        val apps = installed.value ?: return prefs.setGridOrder(order)
        // Restauration depuis la liste visible : les packages cachés gardent leur position.
        prefs.updateGridOrder { current, hidden -> withHiddenKept(apps, current, hidden, order) }
    }

    private fun withHiddenKept(apps: List<TvApp>, current: List<String>, hidden: Set<String>, visibleOrder: List<String>): List<String> {
        if (hidden.isEmpty()) return visibleOrder
        val oldFull = AppCatalog.grid(apps, current).map { it.packageName }
        val remaining = visibleOrder.toMutableList()
        return oldFull.map { pkg -> if (pkg in hidden) pkg else remaining.removeFirstOrNull() }
            .filterNotNull() + remaining
    }

    suspend fun hideApp(packageName: String) {
        prefs.hideApps(packageName)
    }

    suspend fun unhideApp(packageName: String) {
        prefs.unhideApps(packageName)
    }

    suspend fun unhideAll() {
        prefs.setHidden(emptySet())
    }

    suspend fun isSourceEnabled(packageName: String): Boolean =
        packageName !in prefs.disabledSources.first()

    suspend fun setSourceEnabled(packageName: String, enabled: Boolean) {
        prefs.updateDisabledSources { disabled -> if (enabled) disabled - packageName else disabled + packageName }
    }

    // Bascule atomique : lecture et écriture dans la même transaction DataStore.
    suspend fun toggleSource(packageName: String) {
        prefs.updateDisabledSources { disabled ->
            if (packageName in disabled) disabled - packageName else disabled + packageName
        }
    }
}
