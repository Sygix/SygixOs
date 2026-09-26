/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.MainDispatcherRule
import fr.sygix.sygixos.await
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onStart
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
    private var now = 1_000L
    private val prefs = LauncherPrefs(context) { now }
    private val repo = AppCatalogRepository(InstalledAppsSource(context), prefs)

    @Before
    fun resetDataStore() = runBlocking {
        prefs.setDisabledSources(emptySet())
        prefs.setHidden(emptySet())
        prefs.setPinned(emptySet())
        prefs.setGridOrder(emptyList())
        prefs.setCachedApps(emptyList())
    }

    private fun buildViewModel(counts: Flow<Map<String, Int>> = flowOf(emptyMap())) = SettingsViewModel(
        apps = repo,
        programCounts = counts,
        pm = context.packageManager,
        selfPackage = context.packageName,
    )

    private suspend fun seedApps(vararg packages: String) {
        prefs.setCachedApps(packages.map { TvApp(it, it.removePrefix("com.").replaceFirstChar(Char::uppercase)) })
        repo.refreshApps()
    }

    private suspend fun hideInOrder(vararg packages: String) {
        packages.forEach { pkg ->
            now += 1_000
            repo.hideApp(pkg)
        }
    }

    private fun SettingsState.hiddenLabels() = hiddenRows.map { it.app.label }

    private fun SettingsState.sourceLabels() = sources.map { it.app.label }

    @Test
    fun `state exposes sources sorted by label, all enabled by default`() = runBlocking {
        seedApps("com.b", "com.a")
        val vm = buildViewModel()
        val state = vm.state.await { it.sources.size == 2 }
        assertEquals(listOf("A", "B"), state.sourceLabels())
        assertTrue(state.sources.all { it.enabled })
        assertTrue(state.hiddenRows.isEmpty())
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
    fun `hidden rows are ordered by hiding date, most recent first`() = runBlocking {
        seedApps("com.alpha", "com.beta", "com.gamma")
        hideInOrder("com.beta", "com.alpha", "com.gamma")
        val vm = buildViewModel()
        vm.enterCategory(SettingsCategory.HIDDEN)
        assertEquals(listOf("Gamma", "Alpha", "Beta"), vm.state.await { it.hiddenRows.size == 3 }.hiddenLabels())
    }

    @Test
    fun `an unhidden row stays listed as visible, can be hidden again in place, and leaves on refresh`() = runBlocking {
        seedApps("com.a", "com.b")
        hideInOrder("com.a", "com.b")
        val vm = buildViewModel()
        vm.enterCategory(SettingsCategory.HIDDEN)
        vm.state.await { it.hiddenRows.size == 2 }

        vm.unhide("com.a")
        val unhidden = vm.state.await { s -> s.hiddenRows.any { it.app.packageName == "com.a" && !it.hidden } }
        assertEquals(listOf("B", "A"), unhidden.hiddenLabels())
        assertFalse("com.a" in prefs.hidden.first())

        vm.hide("com.a")
        val hiddenAgain = vm.state.await { s -> s.hiddenRows.all { it.hidden } }
        assertEquals(listOf("B", "A"), hiddenAgain.hiddenLabels())

        vm.unhide("com.a")
        vm.state.await { s -> s.hiddenRows.any { !it.hidden } }
        vm.refreshHiddenRows()
        assertEquals(listOf("B"), vm.state.await { it.hiddenRows.size == 1 }.hiddenLabels())
    }

    @Test
    fun `unhideAll keeps every row listed as visible until the list is recomputed`() = runBlocking {
        seedApps("com.a", "com.b")
        hideInOrder("com.a", "com.b")
        val vm = buildViewModel()
        vm.enterCategory(SettingsCategory.HIDDEN)
        vm.state.await { it.hiddenRows.size == 2 }

        vm.unhideAll()
        val state = vm.state.await { s -> s.hiddenRows.none { it.hidden } }
        assertEquals(listOf("B", "A"), state.hiddenLabels())
        assertEquals(emptySet<String>(), prefs.hidden.first())

        vm.enterCategory(SettingsCategory.HIDDEN)
        assertTrue(vm.state.await { it.hiddenRows.isEmpty() }.hiddenRows.isEmpty())
    }

    @Test
    fun `sources are ordered by the counts known when the category is entered`() = runBlocking {
        seedApps("com.a", "com.b", "com.c", "com.d")
        val counts = MutableSharedFlow<Map<String, Int>>(replay = 1)
        counts.emit(mapOf("com.b" to 12, "com.d" to 3))
        val vm = buildViewModel(counts)
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("B", "D", "A", "C"), vm.state.await { it.sources.size == 4 }.sourceLabels())

        counts.emit(mapOf("com.b" to 12, "com.d" to 20))
        vm.counts.await { it?.get("com.d") == 20 }
        assertEquals(listOf("B", "D", "A", "C"), vm.state.value.sourceLabels())

        vm.enterCategory(SettingsCategory.HIDDEN)
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("D", "B", "A", "C"), vm.state.await { it.sources.first().app.label == "D" }.sourceLabels())
    }

    @Test
    fun `toggling a source does not move its row`() = runBlocking {
        seedApps("com.a", "com.b")
        val vm = buildViewModel(flowOf(mapOf("com.b" to 2)))
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("B", "A"), vm.state.await { it.sources.size == 2 }.sourceLabels())
        vm.toggleSource("com.b")
        assertEquals(listOf("B", "A"), vm.state.await { !it.sources.first().enabled }.sourceLabels())
    }

    @Test
    fun `counts arriving after the entry resort the sources once, later counts do not`() = runBlocking {
        seedApps("com.alpha", "com.beta", "com.zeta")
        val counts = MutableSharedFlow<Map<String, Int>>()
        val vm = buildViewModel(counts)
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("Alpha", "Beta", "Zeta"), vm.state.await { it.sources.size == 3 }.sourceLabels())

        counts.emit(mapOf("com.zeta" to 9))
        assertEquals(listOf("Zeta", "Alpha", "Beta"), vm.state.await { it.sources.first().app.label == "Zeta" }.sourceLabels())

        counts.emit(mapOf("com.zeta" to 9, "com.alpha" to 20))
        vm.counts.await { it?.get("com.alpha") == 20 }
        assertEquals(listOf("Zeta", "Alpha", "Beta"), vm.state.value.sourceLabels())
    }

    @Test
    fun `an empty count keeps the sources alphabetical`() = runBlocking {
        seedApps("com.b", "com.a")
        val vm = buildViewModel(flowOf(emptyMap()))
        vm.enterCategory(SettingsCategory.SOURCES)
        vm.counts.await { it != null }
        assertEquals(listOf("A", "B"), vm.state.await { it.sources.size == 2 }.sourceLabels())
    }

    @Test
    fun `counting starts when the view model is created, without any settings collector`() = runBlocking {
        var collected = false
        val counts = flow { emit(mapOf("com.a" to 4)) }.onStart { collected = true }
        val vm = buildViewModel(counts)
        assertTrue(collected)
        assertEquals(mapOf("com.a" to 4), vm.counts.value)
    }
}
