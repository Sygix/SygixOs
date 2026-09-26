/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
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

// Chaque test reconstruit prefs/repo : Robolectric fournit une application (et donc un
// DataStore) fraîche par méthode de test, les tests restent indépendants entre eux.
@RunWith(RobolectricTestRunner::class)
class LauncherPrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = LauncherPrefs(context)
    private val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)

    @Before
    fun resetState() = runBlocking {
        // Le DataStore est un singleton par process : réinitialiser les clés pour rendre
        // chaque test indépendant de l'ordre d'exécution.
        prefs.setDisabledSources(emptySet())
        prefs.setHidden(emptySet())
        prefs.setPinned(emptySet())
        prefs.setGridOrder(emptyList())
        prefs.setCachedApps(emptyList())
    }

    private suspend fun seedApps(vararg packages: String) {
        prefs.setCachedApps(packages.map { TvApp(it, it) })
        repo.refreshApps()
    }

    @Test
    fun `sources are enabled by default and unknown source appears enabled`() = runBlocking {
        assertEquals(emptySet<String>(), prefs.disabledSources.first())
        assertTrue(repo.isSourceEnabled("com.any"))
    }

    @Test
    fun `toggling a source disables it and re-enabling restores the default`() = runBlocking {
        seedApps("com.a", "com.b")
        repo.setSourceEnabled("com.a", false)
        assertFalse(repo.isSourceEnabled("com.a"))
        assertTrue(repo.isSourceEnabled("com.b"))
        repo.setSourceEnabled("com.a", true)
        assertEquals(emptySet<String>(), prefs.disabledSources.first())
        assertTrue(repo.isSourceEnabled("com.a"))
    }

    @Test
    fun `hiding an app removes its pin atomically`() = runBlocking {
        seedApps("com.a", "com.b")
        prefs.setPinned(setOf("com.a", "com.b"))
        repo.hideApp("com.a")
        assertTrue(prefs.hidden.first().contains("com.a"))
        assertFalse(prefs.pinned.first().contains("com.a"))
    }

    @Test
    fun `unhide restores the app in grid and dock, unhideAll clears everything`() = runBlocking {
        seedApps("com.a", "com.b")
        prefs.setPinned(setOf("com.a", "com.b"))
        repo.hideApp("com.a")
        assertEquals(listOf("com.b"), repo.catalog.first().grid.map { it.packageName })
        assertEquals(listOf("com.b"), repo.catalog.first().dock.map { it.packageName })
        repo.unhideApp("com.a")
        assertTrue(repo.catalog.first().grid.map { it.packageName }.contains("com.a"))
        repo.unhideAll()
        assertEquals(emptySet<String>(), prefs.hidden.first())
    }

    @Test
    fun `app reappearing in the cache stays hidden and source state is kept`() = runBlocking {
        seedApps("com.a", "com.b")
        repo.setSourceEnabled("com.a", false)
        repo.hideApp("com.a")
        // Re-scan identique : l'app cachée reste cachée, la source reste désactivée.
        seedApps("com.a", "com.b")
        assertTrue(prefs.hidden.first().contains("com.a"))
        assertFalse(repo.isSourceEnabled("com.a"))
    }

    @Test
    fun `togglePin and moveInGrid write pinned and gridOrder`() = runBlocking {
        seedApps("com.a", "com.b")
        repo.togglePin("com.b")
        assertEquals(setOf("com.b"), prefs.pinned.first())
        repo.togglePin("com.b")
        assertEquals(emptySet<String>(), prefs.pinned.first())
        repo.togglePin("com.b")
        repo.moveInGrid("com.a", 1)
        assertEquals(listOf("com.b", "com.a"), prefs.gridOrder.first())
    }
}
