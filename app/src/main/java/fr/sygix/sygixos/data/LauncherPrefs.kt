/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.dataStore by preferencesDataStore(name = "launcher")

class LauncherPrefs(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) {

    private val pinnedKey = stringSetPreferencesKey("pinned_apps")
    private val orderKey = stringPreferencesKey("grid_order")
    private val appsKey = stringPreferencesKey("cached_apps")
    private val disabledSourcesKey = stringSetPreferencesKey("disabled_sources")
    private val hiddenKey = stringSetPreferencesKey("hidden_apps")
    private val hiddenDatesKey = stringPreferencesKey("hidden_apps_dates")

    val pinned: Flow<Set<String>> = context.dataStore.data.map { it[pinnedKey] ?: emptySet() }

    // Ensemble des apps sources désactivées ; vide/absent = toutes activées (défaut,
    // une app installée plus tard est donc activée sans migration).
    val disabledSources: Flow<Set<String>> = context.dataStore.data.map { it[disabledSourcesKey] ?: emptySet() }

    val hidden: Flow<Set<String>> = context.dataStore.data.map { it[hiddenKey] ?: emptySet() }

    val hiddenWithDates: Flow<Map<String, Long?>> = context.dataStore.data.map { prefs ->
        val dates = hiddenDates(prefs)
        (prefs[hiddenKey] ?: emptySet()).associateWith { dates[it] }
    }

    suspend fun setDisabledSources(disabled: Set<String>) {
        context.dataStore.edit { it[disabledSourcesKey] = disabled }
    }

    suspend fun updateDisabledSources(transform: (Set<String>) -> Set<String>) {
        context.dataStore.edit { prefs -> prefs[disabledSourcesKey] = transform(prefs[disabledSourcesKey] ?: emptySet()) }
    }

    suspend fun updatePinned(transform: (Set<String>) -> Set<String>) {
        context.dataStore.edit { prefs -> prefs[pinnedKey] = transform(prefs[pinnedKey] ?: emptySet()) }
    }

    suspend fun updateGridOrder(transform: (order: List<String>, hidden: Set<String>) -> List<String>) {
        context.dataStore.edit { prefs ->
            val current = prefs[orderKey]?.split(LINE)?.filter { it.isNotEmpty() } ?: emptyList()
            prefs[orderKey] = transform(current, prefs[hiddenKey] ?: emptySet()).joinToString(LINE)
        }
    }

    suspend fun setHidden(hidden: Set<String>) {
        context.dataStore.edit { prefs -> writeHidden(prefs, hidden, hiddenDates(prefs)) }
    }

    val gridOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[orderKey]?.split(LINE)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    val cachedApps: Flow<List<TvApp>> = context.dataStore.data.map { prefs ->
        prefs[appsKey]?.split(LINE).orEmpty().mapNotNull { line ->
            val parts = line.split(FIELD)
            if (parts.size < 2 || parts[0].isEmpty()) null
            else TvApp(packageName = parts[0], label = parts[1], activityName = parts.getOrNull(2)?.takeIf { it.isNotEmpty() })
        }
    }

    suspend fun setPinned(packages: Set<String>) {
        context.dataStore.edit { it[pinnedKey] = packages }
    }

    suspend fun hideApps(vararg packages: String) {
        val now = clock()
        context.dataStore.edit { prefs ->
            writeHidden(prefs, (prefs[hiddenKey] ?: emptySet()) + packages, hiddenDates(prefs) + packages.associateWith { now })
            prefs[pinnedKey] = (prefs[pinnedKey] ?: emptySet()) - packages.toSet()
        }
    }

    suspend fun unhideApps(vararg packages: String) {
        context.dataStore.edit { prefs ->
            writeHidden(prefs, (prefs[hiddenKey] ?: emptySet()) - packages.toSet(), hiddenDates(prefs))
        }
    }

    private fun hiddenDates(prefs: Preferences): Map<String, Long> =
        prefs[hiddenDatesKey]?.split(LINE).orEmpty().mapNotNull { line ->
            val parts = line.split(FIELD)
            val date = parts.getOrNull(1)?.toLongOrNull()
            if (parts[0].isEmpty() || date == null) null else parts[0] to date
        }.toMap()

    private fun writeHidden(prefs: MutablePreferences, hidden: Set<String>, dates: Map<String, Long>) {
        prefs[hiddenKey] = hidden
        val kept = dates.filterKeys { it in hidden }
        if (kept.isEmpty()) {
            prefs.remove(hiddenDatesKey)
        } else {
            prefs[hiddenDatesKey] = kept.entries.joinToString(LINE) { (pkg, date) -> "$pkg$FIELD$date" }
        }
    }

    suspend fun setGridOrder(order: List<String>) {
        context.dataStore.edit { it[orderKey] = order.joinToString(LINE) }
    }

    suspend fun setCachedApps(apps: List<TvApp>) {
        context.dataStore.edit { prefs ->
            prefs[appsKey] = apps.joinToString(LINE) { listOf(it.packageName, it.label, it.activityName.orEmpty()).joinToString(FIELD) }
        }
    }

    private companion object {
        const val LINE = "\n"
        const val FIELD = "\t"
    }
}
