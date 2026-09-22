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

    val catalog: Flow<Catalog> = combine(prefs.pinned, prefs.gridOrder, installed.filterNotNull()) { pinned, order, apps ->
        Catalog(
            dock = AppCatalog.dock(apps, pinned.toList()),
            grid = AppCatalog.grid(apps, order),
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
        val next = AppCatalog.togglePinned(prefs.pinned.first().toList(), packageName)
        prefs.setPinned(next.toSet())
    }

    suspend fun moveInGrid(packageName: String, delta: Int) {
        val apps = installed.value ?: return
        val current = AppCatalog.grid(apps, prefs.gridOrder.first()).map { it.packageName }
        prefs.setGridOrder(AppCatalog.move(current, packageName, delta))
    }

    suspend fun setGridOrder(order: List<String>) = prefs.setGridOrder(order)
}
