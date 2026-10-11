/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import fr.sygix.sygixos.MainDispatcherRule
import fr.sygix.sygixos.await
import fr.sygix.sygixos.domain.HiddenGroup
import fr.sygix.sygixos.domain.HiddenPaneLayout
import fr.sygix.sygixos.model.UpNextContentType
import kotlinx.coroutines.delay
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
class HiddenGroupsViewModelTest {
    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val harness = SettingsHarness()

    @After
    fun clear() = harness.clear()

    private fun SettingsState.keys() = hiddenEntries.map { it.key }

    @Test
    fun `groups show three lines then the count of the others and expansion resets on entry`() = runBlocking {
        val labels = listOf("A", "B", "C", "D", "E")
        harness.install(*labels.toTypedArray())
        harness.hideInOrder(*labels.toTypedArray())
        val series = (1..4).map { harness.removeContent("S$it") }
        val vm = harness.viewModel
        vm.enterCategory(SettingsCategory.HIDDEN)
        val state = vm.state.await { it.hiddenRows.size == 5 && it.hiddenContents.size == 4 }
        assertEquals(
            listOf("unhide-all", "app:com.e", "app:com.d", "app:com.c", "more:APPS", "contents-title") +
                series.reversed().take(3).map { "content:${it.serialized}" } + "more:UPNEXT",
            state.keys(),
        )
        assertEquals(2, (state.hiddenEntries[4] as HiddenEntry.More).count)
        assertEquals(1, (state.hiddenEntries.last() as HiddenEntry.More).count)
        vm.expandHidden(HiddenGroup.APPS)
        val expanded = vm.state.await { HiddenGroup.APPS in it.hiddenExpanded }
        assertEquals("app:com.b", expanded.keys()[4])
        assertFalse(expanded.keys().contains("more:APPS"))
        vm.enterCategory(SettingsCategory.HIDDEN)
        assertTrue(vm.state.await { it.hiddenExpanded.isEmpty() }.keys().contains("more:APPS"))
    }

    @Test
    fun `restoring a content updates the groups and the focus goes to the entry now in its place`() = runBlocking {
        harness.install("A")
        harness.hideInOrder("A")
        val first = harness.removeContent("Premier", UpNextContentType.MOVIE)
        val second = harness.removeContent("Second")
        val vm = harness.viewModel
        vm.enterCategory(SettingsCategory.HIDDEN)
        val before = vm.state.await { it.hiddenContents.size == 2 && it.hiddenRows.size == 1 }.keys().filter { it != "contents-title" }
        vm.restoreUpNext(second)
        val after = vm.state.await { it.hiddenContents.size == 1 && it.hiddenRows.size == 1 }.keys().filter { it != "contents-title" }
        val index = before.indexOf("content:${second.serialized}")
        assertEquals("content:${first.serialized}", HiddenPaneLayout.refocus(after, "content:${second.serialized}", index))
        vm.restoreUpNext(first)
        val emptied = vm.state.await { it.hiddenContents.isEmpty() }
        assertFalse(emptied.keys().contains("contents-title"))
        assertTrue(emptied.canEnter(SettingsCategory.HIDDEN))
        vm.unhideAll()
        withTimeout(5_000) { while (harness.hiddenNow().isNotEmpty()) delay(10) }
        vm.enterCategory(SettingsCategory.HIDDEN)
        val empty = vm.state.await { it.hiddenRows.isEmpty() }
        assertTrue(empty.hiddenEntries.isEmpty())
        assertFalse(empty.canEnter(SettingsCategory.HIDDEN))
    }

    @Test
    fun `layout keeps at most three rows per collapsed group`() {
        assertEquals(3, HiddenPaneLayout.shown(4, expanded = false))
        assertEquals(1, HiddenPaneLayout.more(4, expanded = false))
        assertEquals(0, HiddenPaneLayout.more(3, expanded = false))
        assertEquals(4, HiddenPaneLayout.shown(4, expanded = true))
        assertEquals(null, HiddenPaneLayout.refocus(listOf("a"), null, 0))
        assertEquals("b", HiddenPaneLayout.refocus(listOf("a", "b"), "gone", 5))
    }
}
