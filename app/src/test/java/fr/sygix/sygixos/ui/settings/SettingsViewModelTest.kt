/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.MainDispatcherRule
import fr.sygix.sygixos.await
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

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
        val state = vm.state.await { it.sources.size == 2 }
        assertEquals(listOf("A", "B"), state.sources.map { it.app.label })
        assertTrue(state.sources.all { it.enabled })
        assertTrue(state.hiddenApps.isEmpty())
    }

    @Test
    fun `toggleSource flips the enabled flag in state and persists`() = runBlocking {
        seedApps("com.a")
        val vm = buildViewModel()
        vm.state.await { it.sources.size == 1 }
        vm.toggleSource("com.a")
        assertFalse(vm.state.await { !it.sources.single().enabled }.sources.single().enabled)
        assertFalse(repo.isSourceEnabled("com.a"))
        vm.toggleSource("com.a")
        assertTrue(vm.state.await { it.sources.single().enabled }.sources.single().enabled)
        assertTrue(repo.isSourceEnabled("com.a"))
    }

    @Test
    fun `unhide removes one app, unhideAll clears the hidden list`() = runBlocking {
        seedApps("com.a", "com.b")
        val vm = buildViewModel()
        repo.hideApp("com.a")
        repo.hideApp("com.b")
        val hidden = vm.state.await { it.hiddenApps.size == 2 }
        assertEquals(setOf("A", "B"), hidden.hiddenApps.map { h -> h.label }.toSet())
        vm.unhide("com.a")
        assertEquals(listOf("B"), vm.state.await { it.hiddenApps.size == 1 }.hiddenApps.map { h -> h.label })
        vm.unhideAll()
        assertTrue(vm.state.await { it.hiddenApps.isEmpty() }.hiddenApps.isEmpty())
    }
}
