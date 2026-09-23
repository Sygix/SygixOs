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
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherPrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = LauncherPrefs(context)
    private val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)

    @Test
    fun `sources toggling and app hiding persist in DataStore`() = runBlocking {
        assertEquals(emptySet<String>(), prefs.disabledSources.first())
        assertTrue(repo.isSourceEnabled("com.any"))

        prefs.setCachedApps(listOf(TvApp("com.a", "A"), TvApp("com.b", "B")))
        repo.refreshApps()
        repo.setSourceEnabled("com.a", false)
        assertFalse(repo.isSourceEnabled("com.a"))
        assertTrue(repo.isSourceEnabled("com.b"))

        // Nouvelle instance = relecture disque : la désactivation survit au processus.
        val reloaded = LauncherPrefs(context)
        val disabled = reloaded.disabledSources.first()
        assertTrue(disabled.contains("com.a"))
        assertFalse(disabled.contains("com.b"))

        // Une source inconnue apparaît activée par défaut (réinstallation).
        prefs.setCachedApps(listOf(TvApp("com.a", "A"), TvApp("com.b", "B"), TvApp("com.new", "N")))
        repo.refreshApps()
        assertTrue(repo.isSourceEnabled("com.new"))

        // Cacher une app la retire aussi des épingles, atomiquement.
        prefs.setPinned(setOf("com.a", "com.b"))
        repo.hideApp("com.a")
        assertTrue(prefs.hidden.first().contains("com.a"))
        assertFalse(prefs.pinned.first().contains("com.a"))

        assertEquals(listOf("com.b"), repo.catalog.first().grid.map { it.packageName })
        assertEquals(listOf("com.b"), repo.catalog.first().dock.map { it.packageName })

        // L'app cachée réapparue dans le cache reste cachée ; la source désactivée aussi.
        prefs.setCachedApps(listOf(TvApp("com.a", "A"), TvApp("com.b", "B")))
        repo.refreshApps()
        assertTrue(prefs.hidden.first().contains("com.a"))
        assertTrue(repo.isSourceEnabled("com.b"))
        repo.setSourceEnabled("com.a", false)
        assertTrue(prefs.hidden.first().contains("com.a"))

        repo.unhideApp("com.a")
        assertTrue(repo.catalog.first().grid.map { it.packageName }.contains("com.a"))
        repo.unhideAll()
        assertEquals(emptySet<String>(), prefs.hidden.first())

        repo.setSourceEnabled("com.a", true)
        assertEquals(emptySet<String>(), prefs.disabledSources.first())
        assertTrue(repo.isSourceEnabled("com.a"))

        // togglePin + moveInGrid : écritures atomiques sur pinned/gridOrder.
        repo.togglePin("com.b")
        assertEquals(emptySet<String>(), prefs.pinned.first())
        repo.togglePin("com.b")
        assertEquals(setOf("com.b"), prefs.pinned.first())

        repo.moveInGrid("com.a", 1)
        assertEquals(listOf("com.b", "com.a"), prefs.gridOrder.first())
    }
}
