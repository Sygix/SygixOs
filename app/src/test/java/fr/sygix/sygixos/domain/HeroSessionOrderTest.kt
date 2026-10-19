/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.HeroItem
import kotlin.random.Random
import org.junit.Assert.*
import org.junit.Test

class HeroSessionOrderTest {
    private val items = (1..20).map { HeroItem("$it", "Fiction $it") }
    @Test fun `seed changes initial order but reload does not`() {
        val one = HeroSessionOrder(Random(1))
        val first = one.arrange(items)
        assertNotEquals(first, HeroSessionOrder(Random(2)).arrange(items))
        assertEquals(first, one.arrange(items.reversed()))
    }
    @Test fun `insertions follow displayed program and removals preserve relative order`() {
        val session = HeroSessionOrder(Random(1))
        val first = session.arrange(items)
        session.displayedId = first[10].id
        val added = HeroItem("new", "New fiction")
        val next = session.arrange(items + added)
        assertEquals(first, next.filterNot { it == added })
        assertTrue(next.indexOf(added) > next.indexOfFirst { it.id == session.displayedId })
        assertEquals(next.filterNot { it == first[4] }, session.arrange((items + added).filterNot { it == first[4] }))
        assertEquals(emptyList<HeroItem>(), session.arrange(emptyList()))
    }
}
