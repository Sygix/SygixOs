/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import fr.sygix.sygixos.domain.DefaultLauncherMemory
import fr.sygix.sygixos.domain.DefaultLauncherStore
import fr.sygix.sygixos.model.RepairRecord
import fr.sygix.sygixos.model.SetupStep
import kotlinx.coroutines.flow.first

class DefaultLauncherPrefs(private val context: Context) : DefaultLauncherStore {
    private val previous = stringPreferencesKey("previous_home_component")
    private val changes = stringSetPreferencesKey("default_launcher_modifications")
    private val repairStep = stringPreferencesKey("default_launcher_last_repair_step")
    private val repairTime = longPreferencesKey("default_launcher_last_repair_time")
    private fun repairBoot(step: SetupStep) = intPreferencesKey("default_launcher_repair_boot_${step.name}")

    override suspend fun loadMemory(): DefaultLauncherMemory {
        val prefs = context.dataStore.data.first()
        val last = prefs[repairStep]?.let { name -> SetupStep.entries.firstOrNull { it.name == name } }?.let { step -> prefs[repairTime]?.let { RepairRecord(step, it) } }
        return DefaultLauncherMemory(
            prefs[previous],
            prefs[changes].orEmpty().mapNotNull { name -> SetupStep.entries.firstOrNull { it.name == name } }.toSet(),
            last,
            SetupStep.entries.mapNotNull { step -> prefs[repairBoot(step)]?.let { step to it } }.toMap(),
        )
    }

    override suspend fun markRepairAttempt(step: SetupStep, boot: Int) {
        context.dataStore.edit { it[repairBoot(step)] = boot }
    }

    override suspend fun recordRepair(record: RepairRecord) {
        context.dataStore.edit {
            it[repairStep] = record.step.name
            it[repairTime] = record.timeMs
        }
    }

    override suspend fun rememberPrevious(component: String) {
        require(PrivilegedShell.validComponent(component))
        context.dataStore.edit { if (it[previous] == null) it[previous] = component }
    }

    override suspend fun record(step: SetupStep) {
        context.dataStore.edit { it[changes] = it[changes].orEmpty() + step.name }
    }
}
