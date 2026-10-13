/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import fr.sygix.sygixos.domain.DefaultLauncherMemory
import fr.sygix.sygixos.domain.DefaultLauncherStore
import fr.sygix.sygixos.model.SetupStep
import kotlinx.coroutines.flow.first

class DefaultLauncherPrefs(private val context: Context) : DefaultLauncherStore {
    private val previous = stringPreferencesKey("previous_home_component")
    private val changes = stringSetPreferencesKey("default_launcher_modifications")
    override suspend fun loadMemory(): DefaultLauncherMemory {
        val prefs = context.dataStore.data.first()
        return DefaultLauncherMemory(prefs[previous], prefs[changes].orEmpty().mapNotNull { name -> SetupStep.entries.firstOrNull { it.name == name } }.toSet())
    }

    override suspend fun rememberPrevious(component: String) {
        require(PrivilegedShell.validComponent(component))
        context.dataStore.edit { if (it[previous] == null) it[previous] = component }
    }

    override suspend fun record(step: SetupStep) {
        context.dataStore.edit { it[changes] = it[changes].orEmpty() + step.name }
    }
}
