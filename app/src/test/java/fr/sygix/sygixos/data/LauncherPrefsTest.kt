/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
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

@RunWith(RobolectricTestRunner::class)
class LauncherPrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val prefs = LauncherPrefs(context)
    private val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)

    @Before
    fun resetState() = runBlocking {
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

    @Test
    fun `moveInGrid skips hidden apps and keeps their position in the stored order`() = runBlocking {
        seedApps("com.a", "com.b", "com.c")
        repo.hideApp("com.b")
        assertEquals(listOf("com.a", "com.c"), repo.catalog.first().grid.map { it.packageName })
        repo.moveInGrid("com.a", 1)
        assertEquals(listOf("com.c", "com.a"), repo.catalog.first().grid.map { it.packageName })
        assertEquals(listOf("com.c", "com.b", "com.a"), prefs.gridOrder.first())
        repo.moveInGrid("com.a", -1)
        assertEquals(listOf("com.a", "com.c"), repo.catalog.first().grid.map { it.packageName })
        assertEquals(listOf("com.a", "com.b", "com.c"), prefs.gridOrder.first())
    }

    @Test
    fun `setGridOrder from the visible order keeps hidden apps at their position`() = runBlocking {
        seedApps("com.a", "com.b", "com.c")
        repo.hideApp("com.b")
        repo.moveInGrid("com.a", 1)
        repo.setGridOrder(listOf("com.a", "com.c"))
        assertEquals(listOf("com.a", "com.b", "com.c"), prefs.gridOrder.first())
        assertEquals(listOf("com.a", "com.c"), repo.catalog.first().grid.map { it.packageName })
        repo.unhideAll()
        repo.setGridOrder(listOf("com.c", "com.a", "com.b"))
        assertEquals(listOf("com.c", "com.a", "com.b"), prefs.gridOrder.first())
    }
}
