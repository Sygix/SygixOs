/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
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
        prefs.updateGridOrder { current ->
            AppCatalog.move(AppCatalog.grid(apps, current).map { it.packageName }, packageName, delta)
        }
    }

    suspend fun setGridOrder(order: List<String>) = prefs.setGridOrder(order)

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
}
