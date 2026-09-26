/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = LauncherPrefs(context)
    private val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)

    @Before
    fun resetDataStore() = runBlocking {
        // Le DataStore est un singleton par process : réinitialiser pour rendre chaque
        // test indépendant des autres classes de tests.
        prefs.setDisabledSources(emptySet())
        prefs.setHidden(emptySet())
        prefs.setPinned(emptySet())
        prefs.setGridOrder(emptyList())
        prefs.setCachedApps(emptyList())
    }

    // Le viewModelScope tourne sur Dispatchers.Main (looper Robolectric en pause) :
    // pomper le looper laisse les coroutines du ViewModel se terminer.
    private fun pumpMain() {
        val shadow = shadowOf(Looper.getMainLooper())
        repeat(20) {
            shadow.idle()
            Thread.sleep(10)
        }
    }

    private fun buildViewModel() = SettingsViewModel(
        apps = repo,
        tvProvider = TvProviderHeroSource(context),
        pm = context.packageManager,
        selfPackage = context.packageName,
    )

    private suspend fun seedApps(vararg packages: String) {
        prefs.setCachedApps(packages.map { TvApp(it, it.removePrefix("com.").uppercase()) })
        repo.refreshApps()
    }

    @Test
    fun `state exposes sources sorted by label, all enabled by default`() = runBlocking {
        seedApps("com.b", "com.a")
        val vm = buildViewModel()
        pumpMain()
        val state = vm.state.value
        assertEquals(listOf("A", "B"), state.sources.map { it.app.label })
        assertTrue(state.sources.all { it.enabled })
        assertTrue(state.hiddenApps.isEmpty())
    }

    @Test
    fun `toggleSource flips the enabled flag in state and persists`() = runBlocking {
        seedApps("com.a")
        val vm = buildViewModel()
        pumpMain()
        vm.toggleSource("com.a")
        pumpMain()
        val state = vm.state.value
        assertFalse(state.sources.single().enabled)
        assertFalse(repo.isSourceEnabled("com.a"))
        vm.toggleSource("com.a")
        pumpMain()
        assertTrue(vm.state.value.sources.single().enabled)
        assertTrue(repo.isSourceEnabled("com.a"))
    }

    @Test
    fun `unhide removes one app, unhideAll clears the hidden list`() = runBlocking {
        seedApps("com.a", "com.b")
        val vm = buildViewModel()
        pumpMain()
        repo.hideApp("com.a")
        repo.hideApp("com.b")
        pumpMain()
        assertEquals(setOf("A", "B"), vm.state.value.hiddenApps.map { h -> h.label }.toSet())
        vm.unhide("com.a")
        pumpMain()
        assertEquals(listOf("B"), vm.state.value.hiddenApps.map { h -> h.label })
        vm.unhideAll()
        pumpMain()
        assertTrue(vm.state.value.hiddenApps.isEmpty())
    }
}
