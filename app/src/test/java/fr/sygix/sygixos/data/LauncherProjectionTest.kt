/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import fr.sygix.sygixos.installLeanbackActivity

import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextPosition
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LauncherProjectionTest {
    private class Store : DataStore<Preferences> {
        var writes = 0
        override val data = MutableStateFlow(emptyPreferences())
        override suspend fun updateData(transform: suspend (Preferences) -> Preferences): Preferences =
            transform(data.value).also { data.value = it; writes++ }
    }

    @Test fun unrelatedWritesDoNotRepublishSourceOrPlacementPreferences() = runTest {
        val prefs = LauncherPrefs(Store())
        val sources = mutableListOf<Set<String>>()
        val positions = mutableListOf<UpNextPosition>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { prefs.disabledSources.collect { sources += it } }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { prefs.upNextPosition.collect { positions += it } }
        prefs.updatePinned { setOf("com.app") }
        prefs.setUpNextVisible(false)
        prefs.updateDisabledSources { setOf("com.disabled") }
        prefs.setUpNextPosition(UpNextPosition.AFTER_APPS)
        assertEquals(listOf(emptySet<String>(), setOf("com.disabled")), sources)
        assertEquals(listOf(UpNextPosition.BEFORE_APPS, UpNextPosition.AFTER_APPS), positions)
    }

    @Test fun memoryAndLauncherWritesLeaveCatalogueProjectionsUnchanged() = runTest {
        val prefs = LauncherPrefs(Store())
        val flows: List<Flow<*>> = listOf(prefs.pinned, prefs.hidden, prefs.hiddenWithDates, prefs.gridOrder, prefs.opened, prefs.removed)
        val emissions = IntArray(flows.size)
        flows.forEachIndexed { index, flow ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { flow.collect { emissions[index]++ } }
        }
        prefs.markLauncherGuideSeen()
        prefs.setBootStartEnabled(true, 1)
        prefs.setUpNextPosition(UpNextPosition.AFTER_APPS)
        assertEquals(List(flows.size) { 1 }, emissions.toList())
        prefs.hideApps("com.hidden")
        assertEquals(listOf(1, 2, 2, 1, 1, 1), emissions.toList())
    }

    @Test fun refreshingUnchangedAppsWritesNothingWhileAnInstallIsPublishedAndCached() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = Store()
        val prefs = LauncherPrefs(store)
        val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)
        val grids = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.catalog.collect { c -> grids += c.grid.map { it.packageName } } }
        fun install(pkg: String) = context.packageManager.installLeanbackActivity(
            ResolveInfo().apply {
                nonLocalizedLabel = pkg
                activityInfo = ActivityInfo().apply { packageName = pkg; name = "$pkg.Main"; applicationInfo = ApplicationInfo().apply { packageName = pkg } }
            },
        )
        install("com.a")
        repo.refreshApps()
        val writes = store.writes
        repo.refreshApps()
        assertEquals(writes, store.writes)
        install("com.b")
        repo.refreshApps()
        assertEquals(listOf(listOf("com.a"), listOf("com.a", "com.b")), grids)
        assertEquals(listOf("com.a", "com.b"), prefs.cachedApps.first().map { it.packageName })
    }

    @Test fun hidingAnAppThatIsNotInstalledDoesNotRepublishTheCatalog() = runTest {
        val prefs = LauncherPrefs(Store())
        val repo = AppCatalogRepository(InstalledAppsSource(ApplicationProvider.getApplicationContext()), prefs)
        prefs.setCachedApps(listOf(TvApp("com.app", "App")))
        repo.refreshApps()
        val grids = mutableListOf<List<String>>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { repo.catalog.collect { c -> grids += c.grid.map { it.packageName } } }
        prefs.hideApps("com.absent")
        prefs.hideApps("com.app")
        assertEquals(listOf(listOf("com.app"), emptyList()), grids)
    }
}
