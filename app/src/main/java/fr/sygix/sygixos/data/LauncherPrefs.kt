package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")

class LauncherPrefs(private val context: Context) {

    private val pinnedKey = stringSetPreferencesKey("pinned_apps")
    private val orderKey = stringPreferencesKey("grid_order")
    private val appsKey = stringPreferencesKey("cached_apps")

    val pinned: Flow<Set<String>> = context.dataStore.data.map { it[pinnedKey] ?: emptySet() }

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
