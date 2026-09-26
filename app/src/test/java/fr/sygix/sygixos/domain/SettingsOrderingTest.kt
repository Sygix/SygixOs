/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsOrderingTest {

    private fun apps(vararg labels: String) = labels.map { TvApp("pkg.${it.lowercase()}", it) }

    private fun List<TvApp>.labels() = map { it.label }

    @Test
    fun `sources publishing content come first by count, then the silent ones alphabetically`() {
        val list = apps("Delta", "Bravo", "Charlie", "Alpha")
        val counts = mapOf("pkg.delta" to 12, "pkg.bravo" to 0, "pkg.charlie" to 3, "pkg.alpha" to 0)
        assertEquals(listOf("Delta", "Charlie", "Alpha", "Bravo"), SettingsOrdering.sources(list, counts).labels())
    }

    @Test
    fun `sources without any count are fully alphabetical`() {
        val list = apps("Charlie", "alpha", "Bravo")
        assertEquals(listOf("alpha", "Bravo", "Charlie"), SettingsOrdering.sources(list, emptyMap()).labels())
    }

    @Test
    fun `label case does not influence the source order`() {
        val list = apps("bravo", "Alpha", "charlie", "Delta")
        assertEquals(listOf("Alpha", "bravo", "charlie", "Delta"), SettingsOrdering.sources(list, emptyMap()).labels())
    }

    @Test
    fun `sources with the same positive count are ordered alphabetically ignoring case`() {
        val list = apps("beta", "Alpha", "Gamma")
        val counts = mapOf("pkg.beta" to 5, "pkg.alpha" to 5, "pkg.gamma" to 7)
        assertEquals(listOf("Gamma", "Alpha", "beta"), SettingsOrdering.sources(list, counts).labels())
    }

    @Test
    fun `hidden apps with a date come first from the most recent, then undated ones alphabetically`() {
        val list = apps("Old", "zulu", "Recent", "Alpha")
        val dates = mapOf("pkg.old" to 10L, "pkg.zulu" to null, "pkg.recent" to 30L, "pkg.alpha" to null)
        assertEquals(listOf("Recent", "Old", "Alpha", "zulu"), SettingsOrdering.hidden(list, dates).labels())
    }

    @Test
    fun `hidden apps without any date are alphabetical`() {
        val list = apps("Charlie", "alpha", "Bravo")
        assertEquals(listOf("alpha", "Bravo", "Charlie"), SettingsOrdering.hidden(list, emptyMap()).labels())
    }

    @Test
    fun `hidden apps all dated follow their dates only`() {
        val list = apps("Alpha", "Bravo", "Charlie")
        val dates = mapOf("pkg.alpha" to 1L, "pkg.bravo" to 3L, "pkg.charlie" to 2L)
        assertEquals(listOf("Bravo", "Charlie", "Alpha"), SettingsOrdering.hidden(list, dates).labels())
    }
}
