/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import fr.sygix.sygixos.MainDispatcherRule
import fr.sygix.sygixos.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val harnesses = mutableListOf<SettingsHarness>()

    @After
    fun clearViewModels() = harnesses.forEach { it.clear() }

    private fun harness(counts: Flow<Map<String, Int>> = flowOf(emptyMap())) =
        SettingsHarness(counts).also { harnesses += it }

    private fun CoroutineScope.counting(vm: SettingsViewModel): Job =
        launch(Dispatchers.Unconfined) { vm.counts.collect { } }

    private fun SettingsState.hiddenLabels() = hiddenRows.map { it.app.label }

    private fun SettingsState.sourceLabels() = sources.map { it.app.label }

    @Test
    fun `state exposes sources sorted by label, all enabled by default`() = runBlocking {
        val h = harness()
        h.install("B", "A")
        val state = h.viewModel.state.await { it.sources.size == 2 }
        assertEquals(listOf("A", "B"), state.sourceLabels())
        assertTrue(state.sources.all { it.enabled })
        assertTrue(state.hiddenRows.isEmpty())
    }

    @Test
    fun `toggleSource flips the enabled flag in state and persists`() = runBlocking {
        val h = harness()
        h.install("A")
        val vm = h.viewModel
        vm.state.await { it.sources.size == 1 }
        vm.toggleSource("com.a")
        assertFalse(vm.state.await { !it.sources.single().enabled }.sources.single().enabled)
        assertFalse(h.repo.isSourceEnabled("com.a"))
        vm.toggleSource("com.a")
        assertTrue(vm.state.await { it.sources.single().enabled }.sources.single().enabled)
        assertTrue(h.repo.isSourceEnabled("com.a"))
    }

    @Test
    fun `hidden rows are ordered by hiding date, most recent first`() = runBlocking {
        val h = harness()
        h.install("Alpha", "Beta", "Gamma")
        h.hideInOrder("Beta", "Alpha", "Gamma")
        h.viewModel.enterCategory(SettingsCategory.HIDDEN)
        assertEquals(listOf("Gamma", "Alpha", "Beta"), h.viewModel.state.await { it.hiddenRows.size == 3 }.hiddenLabels())
    }

    @Test
    fun `toggling a hidden row unhides it in place, hides it again with a new date, and it leaves on refresh`() = runBlocking {
        val h = harness()
        h.install("A", "B")
        h.hideInOrder("A", "B")
        val vm = h.viewModel
        vm.enterCategory(SettingsCategory.HIDDEN)
        vm.state.await { it.hiddenRows.size == 2 }

        vm.toggleHidden("com.a")
        val unhidden = vm.state.await { s -> s.hiddenRows.any { it.app.packageName == "com.a" && !it.hidden } }
        assertEquals(listOf("B", "A"), unhidden.hiddenLabels())
        assertFalse("com.a" in h.hiddenNow())

        h.now = 9_000L
        vm.toggleHidden("com.a")
        val hiddenAgain = vm.state.await { s -> s.hiddenRows.all { it.hidden } }
        assertEquals(listOf("B", "A"), hiddenAgain.hiddenLabels())
        assertEquals(9_000L, h.prefs.hiddenWithDates.first()["com.a"])

        vm.toggleHidden("com.a")
        vm.state.await { s -> s.hiddenRows.any { !it.hidden } }
        vm.refreshHiddenRows()
        assertEquals(listOf("B"), vm.state.await { it.hiddenRows.size == 1 }.hiddenLabels())
    }

    @Test
    fun `unhideAll keeps every row listed as visible until the list is recomputed`() = runBlocking {
        val h = harness()
        h.install("A", "B")
        h.hideInOrder("A", "B")
        val vm = h.viewModel
        vm.enterCategory(SettingsCategory.HIDDEN)
        vm.state.await { it.hiddenRows.size == 2 }

        vm.unhideAll()
        val state = vm.state.await { s -> s.hiddenRows.none { it.hidden } }
        assertEquals(listOf("B", "A"), state.hiddenLabels())
        assertEquals(emptySet<String>(), h.hiddenNow())

        vm.enterCategory(SettingsCategory.HIDDEN)
        assertTrue(vm.state.await { it.hiddenRows.isEmpty() }.hiddenRows.isEmpty())
    }

    @Test
    fun `unhideAll only unhides the listed apps, an uninstalled hidden app stays hidden after reinstall`() = runBlocking {
        val h = harness()
        h.install("Alpha", "Beta", "Keep")
        h.hideInOrder("Alpha", "Beta")
        h.uninstall("Beta")
        val vm = h.viewModel
        vm.enterCategory(SettingsCategory.HIDDEN)
        assertEquals(listOf("Alpha"), vm.state.await { it.hiddenRows.size == 1 && it.sources.size == 2 }.hiddenLabels())

        vm.unhideAll()
        vm.state.await { s -> s.hiddenRows.none { it.hidden } }
        assertEquals(setOf("com.beta"), h.hiddenNow())

        h.install("Beta")
        val grid = h.repo.catalog.first { it.grid.size == 2 }.grid.map { it.packageName }
        assertEquals(listOf("com.alpha", "com.keep"), grid.sorted())
        vm.enterCategory(SettingsCategory.HIDDEN)
        assertEquals(listOf("Beta"), vm.state.await { it.hiddenRows.size == 1 && it.hiddenRows.single().app.label == "Beta" }.hiddenLabels())
    }

    @Test
    fun `sources are ordered by the counts known when the category is entered`() = runBlocking {
        val counts = MutableSharedFlow<Map<String, Int>>(replay = 1)
        counts.emit(mapOf("com.b" to 12, "com.d" to 3))
        val h = harness(counts)
        h.install("A", "B", "C", "D")
        val vm = h.viewModel
        val counting = counting(vm)
        vm.counts.await { it != null }
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("B", "D", "A", "C"), vm.state.await { it.sources.size == 4 }.sourceLabels())

        counts.emit(mapOf("com.b" to 12, "com.d" to 20))
        vm.counts.await { it?.get("com.d") == 20 }
        assertEquals(listOf("B", "D", "A", "C"), vm.state.value.sourceLabels())

        vm.enterCategory(SettingsCategory.HIDDEN)
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("D", "B", "A", "C"), vm.state.await { it.sources.first().app.label == "D" }.sourceLabels())
        counting.cancel()
    }

    @Test
    fun `toggling a source does not move its row`() = runBlocking {
        val h = harness(flowOf(mapOf("com.b" to 2)))
        h.install("A", "B")
        val vm = h.viewModel
        val counting = counting(vm)
        vm.counts.await { it != null }
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("B", "A"), vm.state.await { it.sources.size == 2 }.sourceLabels())
        vm.toggleSource("com.b")
        assertEquals(listOf("B", "A"), vm.state.await { !it.sources.first().enabled }.sourceLabels())
        counting.cancel()
    }

    @Test
    fun `counts arriving after the entry resort the sources once, later counts do not`() = runBlocking {
        val counts = MutableSharedFlow<Map<String, Int>>()
        val h = harness(counts)
        h.install("Alpha", "Beta", "Zeta")
        val vm = h.viewModel
        val counting = counting(vm)
        vm.enterCategory(SettingsCategory.SOURCES)
        assertEquals(listOf("Alpha", "Beta", "Zeta"), vm.state.await { it.sources.size == 3 }.sourceLabels())

        counts.emit(mapOf("com.zeta" to 9))
        assertEquals(listOf("Zeta", "Alpha", "Beta"), vm.state.await { it.sources.first().app.label == "Zeta" }.sourceLabels())

        counts.emit(mapOf("com.zeta" to 9, "com.alpha" to 20))
        vm.counts.await { it?.get("com.alpha") == 20 }
        assertEquals(listOf("Zeta", "Alpha", "Beta"), vm.state.value.sourceLabels())
        counting.cancel()
    }

    @Test
    fun `an empty count keeps the sources alphabetical`() = runBlocking {
        val h = harness(flowOf(emptyMap()))
        h.install("B", "A")
        val vm = h.viewModel
        val counting = counting(vm)
        vm.enterCategory(SettingsCategory.SOURCES)
        vm.counts.await { it != null }
        assertEquals(listOf("A", "B"), vm.state.await { it.sources.size == 2 }.sourceLabels())
        counting.cancel()
    }

    @Test
    fun `counting runs only while collected and counts afresh when collected again`() = runBlocking {
        var starts = 0
        var stops = 0
        val counts = flow {
            starts++
            emit(mapOf("com.a" to starts))
            awaitCancellation()
        }.onCompletion { stops++ }
        val h = harness(counts)
        h.install("A")
        val vm = h.viewModel
        vm.state.await { it.sources.size == 1 }
        assertEquals(0, starts)

        val foreground = counting(vm)
        assertEquals(mapOf("com.a" to 1), vm.counts.await { it != null })
        foreground.cancel()
        withTimeout(5_000) { while (stops == 0) delay(10) }
        assertEquals(1, starts)

        val back = counting(vm)
        assertEquals(mapOf("com.a" to 2), vm.counts.await { it?.get("com.a") == 2 })
        assertEquals(2, starts)
        back.cancel()
    }
}
