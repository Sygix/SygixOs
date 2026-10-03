/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import androidx.test.core.app.ApplicationProvider
import android.content.Context
import fr.sygix.sygixos.model.TvApp
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LauncherPrefsTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private var now = 1_000L
    private val prefs = LauncherPrefs(context) { now }
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
    fun `unhide restores the app in the grid and clearing the hidden set clears everything`() = runBlocking {
        seedApps("com.a", "com.b")
        prefs.setPinned(setOf("com.a", "com.b"))
        repo.hideApp("com.a")
        assertEquals(listOf("com.b"), repo.catalog.first().grid.map { it.packageName })
        assertEquals(listOf("com.b"), repo.catalog.first().dock.map { it.packageName })
        repo.unhideApps(listOf("com.a"))
        assertTrue(repo.catalog.first().grid.map { it.packageName }.contains("com.a"))
        repo.hideApp("com.b")
        prefs.setHidden(emptySet())
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
        prefs.setHidden(emptySet())
        repo.setGridOrder(listOf("com.c", "com.a", "com.b"))
        assertEquals(listOf("com.c", "com.a", "com.b"), prefs.gridOrder.first())
    }

    @Test
    fun `hiding an app records the hiding date from the clock`() = runBlocking {
        seedApps("com.a", "com.b")
        now = 42_000L
        repo.hideApp("com.a")
        assertEquals(mapOf("com.a" to 42_000L), prefs.hiddenWithDates.first())
    }

    @Test
    fun `hide, unhide and hide again keeps a single date, the latest one`() = runBlocking {
        seedApps("com.a")
        now = 10L
        prefs.hideApps("com.a")
        prefs.unhideApps("com.a")
        assertEquals(emptyMap<String, Long?>(), prefs.hiddenWithDates.first())
        assertNull(context.dataStore.data.first()[datesKey])
        now = 20L
        prefs.hideApps("com.a")
        assertEquals(mapOf("com.a" to 20L), prefs.hiddenWithDates.first())
        assertEquals("com.a\t20", context.dataStore.data.first()[datesKey])
    }

    @Test
    fun `clearing the hidden set clears the dates too`() = runBlocking {
        prefs.hideApps("com.a", "com.b")
        prefs.setHidden(emptySet())
        assertEquals(emptyMap<String, Long?>(), prefs.hiddenWithDates.first())
        assertNull(context.dataStore.data.first()[datesKey])
    }

    @Test
    fun `apps hidden by a previous version stay hidden without a date and reading writes nothing`() = runBlocking {
        seedApps("com.a", "com.b", "com.c")
        context.dataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("hidden_apps")] = setOf("com.a", "com.b")
            prefs[stringSetPreferencesKey("pinned_apps")] = setOf("com.a", "com.c")
            prefs.remove(datesKey)
        }
        val before = context.dataStore.data.first()
        assertEquals(mapOf<String, Long?>("com.a" to null, "com.b" to null), prefs.hiddenWithDates.first())
        val catalog = repo.catalog.first()
        assertEquals(listOf("com.c"), catalog.grid.map { it.packageName })
        assertEquals(listOf("com.c"), catalog.dock.map { it.packageName })
        assertEquals(before, context.dataStore.data.first())
    }

    @Test
    fun `a date without a hidden package is ignored on read and dropped on the next write`() = runBlocking {
        context.dataStore.edit { prefs ->
            prefs[stringSetPreferencesKey("hidden_apps")] = setOf("com.a")
            prefs[datesKey] = "com.a\t5\ncom.orphan\t7"
        }
        assertEquals(mapOf<String, Long?>("com.a" to 5L), prefs.hiddenWithDates.first())
        now = 9L
        prefs.hideApps("com.b")
        assertEquals(mapOf<String, Long?>("com.a" to 5L, "com.b" to 9L), prefs.hiddenWithDates.first())
        assertFalse(context.dataStore.data.first()[datesKey].orEmpty().contains("com.orphan"))
    }

    @Test
    fun `toggling an app hides it with a date and unpins it, toggling again unhides it`() = runBlocking {
        seedApps("com.a", "com.b")
        prefs.setPinned(setOf("com.a", "com.b"))
        now = 30L
        repo.toggleHidden("com.a")
        assertEquals(mapOf<String, Long?>("com.a" to 30L), prefs.hiddenWithDates.first())
        assertEquals(setOf("com.b"), prefs.pinned.first())
        repo.toggleHidden("com.a")
        assertEquals(emptyMap<String, Long?>(), prefs.hiddenWithDates.first())
        assertNull(context.dataStore.data.first()[datesKey])
        assertTrue("com.a" in repo.catalog.first().grid.map { it.packageName })
    }

    private val datesKey = stringPreferencesKey("hidden_apps_dates")
}
