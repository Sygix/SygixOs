/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Test

class AppCatalogTest {

    private val apps = listOf(
        TvApp("com.netflix", "Netflix"),
        TvApp("com.zeta", "zeta"),
        TvApp("com.alpha", "Alpha"),
    )

    @Test
    fun `grid is ordered alphabetically and keeps pinned apps`() {
        val grid = AppCatalog.grid(apps)
        assertEquals(listOf("com.alpha", "com.netflix", "com.zeta"), grid.map { it.packageName })
    }

    @Test
    fun `dock keeps pin order`() {
        val dock = AppCatalog.dock(apps, listOf("com.zeta", "com.netflix"))
        assertEquals(listOf("com.zeta", "com.netflix"), dock.map { it.packageName })
    }

    @Test
    fun `dock ignores unknown packages`() {
        val dock = AppCatalog.dock(apps, listOf("com.ghost", "com.netflix"))
        assertEquals(listOf("com.netflix"), dock.map { it.packageName })
    }

    @Test
    fun `grid follows persisted order then appends new apps alphabetically`() {
        val grid = AppCatalog.grid(apps, listOf("com.zeta", "com.ghost", "com.netflix"))
        assertEquals(listOf("com.zeta", "com.netflix", "com.alpha"), grid.map { it.packageName })
    }

    @Test
    fun `move inserts at target and clamps at edges`() {
        val order = listOf("a", "b", "c", "d", "e", "f")
        assertEquals(listOf("b", "a", "c", "d", "e", "f"), AppCatalog.move(order, "a", 1))
        assertEquals(listOf("b", "c", "d", "e", "f", "a"), AppCatalog.move(order, "a", 5))
        assertEquals(listOf("b", "c", "d", "e", "f", "a"), AppCatalog.move(order, "a", 99))
        assertEquals(listOf("a", "f", "b", "c", "d", "e"), AppCatalog.move(order, "f", -4))
        assertEquals(order, AppCatalog.move(order, "a", -1))
        assertEquals(order, AppCatalog.move(order, "zz", 1))
    }

    @Test
    fun `togglePin adds and removes`() {
        assertEquals(listOf("a"), AppCatalog.togglePinned(emptyList(), emptyList(), "a"))
        assertEquals(emptyList<String>(), AppCatalog.togglePinned(listOf("a"), listOf("a"), "a"))
    }

    @Test
    fun `dock holds six apps at most and keeps every stored pin`() {
        val many = (1..8).map { TvApp(packageName = "p$it", label = "P$it") }
        val pinned = many.map { it.packageName }
        assertEquals(pinned.take(6), AppCatalog.dock(many, pinned).map { it.packageName })
        val dock = pinned.take(6)
        assertEquals(pinned, AppCatalog.togglePinned(pinned, dock, "p7"))
        assertEquals(pinned - "p2", AppCatalog.togglePinned(pinned, dock, "p2"))
    }

    @Test
    fun `pinning is refused once the dock is full`() {
        val dock = (1..6).map { "p$it" }
        assertEquals(dock, AppCatalog.togglePinned(dock, dock, "new"))
        assertEquals(dock.take(5) + "new", AppCatalog.togglePinned(dock.take(5), dock.take(5), "new"))
    }

    @Test
    fun `pin state tells pinned, available and full`() {
        val six = (1..6).map { TvApp(packageName = "p$it", label = "P$it") }
        assertEquals(PinState.PINNED, AppCatalog.pinState(six, "p3"))
        assertEquals(PinState.DOCK_FULL, AppCatalog.pinState(six, "other"))
        assertEquals(PinState.AVAILABLE, AppCatalog.pinState(six.take(5), "other"))
    }
}
