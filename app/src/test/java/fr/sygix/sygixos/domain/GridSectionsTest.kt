/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.UpNextContentType
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextPosition
import fr.sygix.sygixos.model.UpNextSourceEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GridSectionsTest {
    private val apps = (1..7).map { "app$it" }
    private fun card(id: Long) = UpNextItem(id, UpNextSourceEntry("src"), UpNextContentType.MOVIE, "Film $id")
    private fun state(content: UpNextContent, position: UpNextPosition = UpNextPosition.BEFORE_APPS) = UpNextState(content, position)
    private val items = UpNextContent.Items(listOf(card(1), card(2)))

    @Test
    fun `focus order and row indices follow the configured position`() {
        val before = GridSections.of(state(items), apps)
        assertEquals(listOf("src:1", "src:2") + apps, before.focusOrder)
        assertEquals(1, before.firstAppRow)
        assertEquals(0, before.upNextRow(2))
        assertEquals(2, before.appIndexOffset)
        assertEquals(1, before.cardOrderIndex(1))
        val after = GridSections.of(state(items, UpNextPosition.AFTER_APPS), apps)
        assertEquals(apps + listOf("src:1", "src:2"), after.focusOrder)
        assertEquals(0, after.firstAppRow)
        assertEquals(2, after.upNextRow(2))
        assertEquals(0, after.appIndexOffset)
        assertEquals(apps.size + 1, after.cardOrderIndex(1))
    }

    @Test
    fun `hidden pending skeleton and error rows expose the expected focus targets`() {
        val hidden = GridSections.of(null, apps)
        assertFalse(hidden.upNextShown)
        assertEquals(apps, hidden.focusOrder)
        assertEquals(0, hidden.firstAppRow)
        assertFalse(GridSections.of(state(UpNextContent.Pending), apps).upNextShown)
        val skeleton = GridSections.of(state(UpNextContent.Skeleton), apps)
        assertTrue(skeleton.upNextShown)
        assertEquals(apps, skeleton.focusOrder)
        assertEquals(listOf(GridSections.ERROR_KEY), GridSections.of(state(UpNextContent.Error), apps).upNextKeys)
    }

    @Test
    fun `without apps the row is first whatever the position`() {
        assertTrue(GridSections.of(state(items, UpNextPosition.AFTER_APPS), emptyList()).upNextFirst)
        assertFalse(GridSections.of(state(items, UpNextPosition.AFTER_APPS), apps).upNextFirst)
    }

    @Test
    fun `row of a key counts the up next row in both positions`() {
        val before = GridSections.of(state(items), apps)
        assertEquals(0, before.rowOf("src:2", 5))
        assertEquals(1, before.rowOf("app1", 5))
        assertEquals(2, before.rowOf("app6", 5))
        assertEquals(-1, before.rowOf("unknown", 5))
        val after = GridSections.of(state(items, UpNextPosition.AFTER_APPS), apps)
        assertEquals(0, after.rowOf("app1", 5))
        assertEquals(1, after.rowOf("app6", 5))
        assertEquals(2, after.rowOf("src:1", 5))
    }

    @Test
    fun `row geometry puts each title in the first row of its section in configured order`() {
        val before = GridSections.of(state(items), apps).rows(2, 112f, 86f, 40f)
        assertEquals(listOf(GridScroll.RowGeometry(112f, 40f), GridScroll.RowGeometry(86f, 40f), GridScroll.RowGeometry(86f, 0f)), before)
        val after = GridSections.of(state(items, UpNextPosition.AFTER_APPS), apps).rows(2, 112f, 86f, 40f)
        assertEquals(listOf(GridScroll.RowGeometry(86f, 40f), GridScroll.RowGeometry(86f, 0f), GridScroll.RowGeometry(112f, 40f)), after)
        assertEquals(listOf(GridScroll.RowGeometry(86f, 40f), GridScroll.RowGeometry(86f, 0f)), GridSections.of(null, apps).rows(2, 112f, 86f, 40f))
    }

    @Test
    fun `empty apps message fills the space left between the margins`() {
        assertEquals(460f, GridSections.of(null, emptyList()).emptyAppsHeight(540f, 40f, 112f, 40f, 32f), 0f)
        assertEquals(276f, GridSections.of(state(items), emptyList()).emptyAppsHeight(540f, 40f, 112f, 40f, 32f), 0f)
        assertEquals(0f, GridSections.of(state(items), emptyList()).emptyAppsHeight(100f, 40f, 112f, 40f, 32f), 0f)
    }

    @Test
    fun `grid has a focus target only with apps cards or the error card`() {
        assertTrue(GridSections.hasFocusTarget(1, null))
        assertFalse(GridSections.hasFocusTarget(0, null))
        assertTrue(GridSections.hasFocusTarget(0, state(items)))
        assertTrue(GridSections.hasFocusTarget(0, state(UpNextContent.Error)))
        assertFalse(GridSections.hasFocusTarget(0, state(UpNextContent.Skeleton)))
        assertFalse(GridSections.hasFocusTarget(0, state(UpNextContent.Items(emptyList()))))
    }

    @Test
    fun `up from the first app row targets the last visited card or the first one only when a focusable row is above`() {
        val before = GridSections.of(state(items), apps)
        assertEquals(GridSections.Up.Card("src:2"), before.upFrom("app3", 5, "src:2"))
        assertEquals(GridSections.Up.Card("src:1"), before.upFrom("app3", 5, null))
        assertEquals(GridSections.Up.Card("src:1"), before.upFrom("app1", 5, "gone"))
        assertEquals(GridSections.Up.Default, before.upFrom("app6", 5, null))
        assertEquals(GridSections.Up.Exit, before.upFrom("src:2", 5, null))
        assertEquals(GridSections.Up.Exit, GridSections.of(state(UpNextContent.Skeleton), apps).upFrom("app1", 5, null))
        assertEquals(GridSections.Up.Exit, GridSections.of(null, apps).upFrom("app1", 5, "src:1"))
        assertEquals(GridSections.Up.Card(GridSections.ERROR_KEY), GridSections.of(state(UpNextContent.Error), apps).upFrom("app2", 5, "src:1"))
        val after = GridSections.of(state(items, UpNextPosition.AFTER_APPS), apps)
        assertEquals(GridSections.Up.Exit, after.upFrom("app1", 5, "src:1"))
        assertEquals(GridSections.Up.Default, after.upFrom("src:1", 5, null))
        assertEquals(GridSections.Up.Exit, before.upFrom(null, 5, null))
    }
}
