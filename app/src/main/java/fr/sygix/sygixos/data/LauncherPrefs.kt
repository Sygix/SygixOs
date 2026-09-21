package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")

/** Préférences locales du launcher : apps épinglées (dock) et ordre de la grille. */
class LauncherPrefs(private val context: Context) {

    private val pinnedKey = stringSetPreferencesKey("pinned_apps")
    private val orderKey = stringPreferencesKey("grid_order")

    val pinned: Flow<Set<String>> = context.dataStore.data.map { it[pinnedKey] ?: emptySet() }

    val gridOrder: Flow<List<String>> = context.dataStore.data.map { prefs ->
        prefs[orderKey]?.split(ORDER_SEPARATOR)?.filter { it.isNotEmpty() } ?: emptyList()
    }

    suspend fun setPinned(packages: Set<String>) {
        context.dataStore.edit { it[pinnedKey] = packages }
    }

    suspend fun setGridOrder(order: List<String>) {
        context.dataStore.edit { it[orderKey] = order.joinToString(ORDER_SEPARATOR) }
    }

    private companion object {
        const val ORDER_SEPARATOR = "\n"
    }
}
