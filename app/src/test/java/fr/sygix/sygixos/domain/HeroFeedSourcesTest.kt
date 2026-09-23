/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import org.junit.Assert.assertEquals
import org.junit.Test

class HeroFeedSourcesTest {

    private fun feed(vararg packages: String?): HeroFeed =
        HeroFeed(
            items = packages.mapIndexed { i, pkg ->
                HeroItem(id = "p$i", title = "T$i", sourcePackage = pkg)
            },
            fromApps = true,
        )

    @Test
    fun `empty disabled set keeps every program`() {
        val f = feed("com.a", "com.b", null)
        assertEquals(f.items, filterBySources(f, emptySet()).items)
    }

    @Test
    fun `disabling a source removes its programs immediately`() {
        val filtered = filterBySources(feed("com.a", "com.b"), setOf("com.b"))
        assertEquals(listOf("p0"), filtered.items.map { it.id })
    }

    @Test
    fun `re-enabling a source brings its programs back`() {
        val filtered = filterBySources(feed("com.a", "com.b"), emptySet())
        assertEquals(2, filtered.items.size)
    }

    @Test
    fun `programs without source package are never filtered`() {
        val filtered = filterBySources(feed("com.a", null), setOf("com.a", "com.other"))
        assertEquals(listOf("p1"), filtered.items.map { it.id })
    }
}
