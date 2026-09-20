package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "launcher")

class PinnedAppsStore(private val context: Context) {

    private val key = stringSetPreferencesKey("pinned_apps")

    val pinned: Flow<Set<String>> = context.dataStore.data.map { it[key] ?: emptySet() }

    suspend fun setPinned(packages: Set<String>) {
        context.dataStore.edit { it[key] = packages }
    }
}
