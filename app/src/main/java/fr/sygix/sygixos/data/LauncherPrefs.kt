/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.BootStartState
import fr.sygix.sygixos.model.LauncherSystemPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@VisibleForTesting
internal val Context.dataStore by preferencesDataStore(name = "launcher")

class LauncherPrefs(
    private val context: Context,
    private val clock: () -> Long = System::currentTimeMillis,
) : LauncherSystemStore {

    private val upNextVisibleKey = booleanPreferencesKey("up_next_visible")
    private val upNextPositionKey = stringPreferencesKey("up_next_position")

    val upNextVisible: Flow<Boolean> = context.dataStore.data.map { it[upNextVisibleKey] ?: true }
    val upNextPosition: Flow<UpNextPosition> = context.dataStore.data.map { prefs ->
        UpNextPosition.entries.firstOrNull { it.name == prefs[upNextPositionKey] }
            ?: UpNextPosition.BEFORE_APPS
    }

    suspend fun setUpNextVisible(visible: Boolean) {
        context.dataStore.edit { it[upNextVisibleKey] = visible }
    }

    suspend fun setUpNextPosition(position: UpNextPosition) {
        context.dataStore.edit { it[upNextPositionKey] = position.name }
    }

    private val pinnedKey = stringSetPreferencesKey("pinned_apps")
    private val orderKey = stringPreferencesKey("grid_order")
    private val appsKey = stringPreferencesKey("cached_apps")
    private val disabledSourcesKey = stringSetPreferencesKey("disabled_sources")
    private val hiddenKey = stringSetPreferencesKey("hidden_apps")
    private val hiddenDatesKey = stringPreferencesKey("hidden_apps_dates")
    private val onboardingDismissedKey = booleanPreferencesKey("launcher_onboarding_dismissed")
    private val bootStartEnabledKey = booleanPreferencesKey("boot_start_enabled")
    private val bootStartEnabledAtKey = intPreferencesKey("boot_start_enabled_at_boot")
    private val bootStartObservedAtKey = intPreferencesKey("boot_start_observed_at_boot")

    override val launcherSystem: Flow<LauncherSystemPreferences> = context.dataStore.data.map { prefs ->
        LauncherSystemPreferences(
            onboardingDismissed = prefs[onboardingDismissedKey] ?: false,
            bootStart = BootStartState(
                enabled = prefs[bootStartEnabledKey] ?: false,
                enabledAtBoot = prefs[bootStartEnabledAtKey],
                observedAtBoot = prefs[bootStartObservedAtKey],
            ),
        )
    }

    override suspend fun dismissLauncherOnboarding() {
        context.dataStore.edit { it[onboardingDismissedKey] = true }
    }

    override suspend fun setBootStartEnabled(enabled: Boolean, currentBoot: Int?) {
        context.dataStore.edit { prefs ->
            if (enabled && prefs[bootStartEnabledKey] != true) {
                if (currentBoot != null) prefs[bootStartEnabledAtKey] = currentBoot
                else prefs.remove(bootStartEnabledAtKey)
                prefs.remove(bootStartObservedAtKey)
            }
            prefs[bootStartEnabledKey] = enabled
            if (!enabled) {
                prefs.remove(bootStartEnabledAtKey)
                prefs.remove(bootStartObservedAtKey)
            }
        }
    }

    override suspend fun recordBootStartObserved(currentBoot: Int?) {
        if (currentBoot == null) return
        context.dataStore.edit { it[bootStartObservedAtKey] = currentBoot }
    }

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
        context.dataStore.edit { prefs -> hide(prefs, packages.toSet(), now) }
    }

    suspend fun unhideApps(vararg packages: String) {
        context.dataStore.edit { prefs -> unhide(prefs, packages.toSet()) }
    }

    suspend fun toggleHidden(packageName: String) {
        val now = clock()
        context.dataStore.edit { prefs ->
            val packages = setOf(packageName)
            if (packageName in (prefs[hiddenKey] ?: emptySet())) unhide(prefs, packages) else hide(prefs, packages, now)
        }
    }

    private fun hide(prefs: MutablePreferences, packages: Set<String>, now: Long) {
        writeHidden(prefs, (prefs[hiddenKey] ?: emptySet()) + packages, hiddenDates(prefs) + packages.associateWith { now })
        prefs[pinnedKey] = (prefs[pinnedKey] ?: emptySet()) - packages
    }

    private fun unhide(prefs: MutablePreferences, packages: Set<String>) {
        writeHidden(prefs, (prefs[hiddenKey] ?: emptySet()) - packages, hiddenDates(prefs))
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
