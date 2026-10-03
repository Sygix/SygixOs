/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ListRevealTest {

    private val order = listOf("com.zeta", "com.a", "com.b")

    @Test
    fun `a focused row out of view is revealed at its new index, after the leading items`() {
        assertEquals(1, ListReveal.indexToReveal(order, "com.zeta", listOf("com.a", "com.b"), leadingItems = 1))
        assertEquals(3, ListReveal.indexToReveal(order, "com.b", listOf("header"), leadingItems = 1))
    }

    @Test
    fun `a visible, unknown or missing focus needs no scroll`() {
        assertNull(ListReveal.indexToReveal(order, "com.zeta", listOf("header", "com.zeta"), leadingItems = 1))
        assertNull(ListReveal.indexToReveal(order, "com.gone", emptyList(), leadingItems = 1))
        assertNull(ListReveal.indexToReveal(order, null, emptyList(), leadingItems = 1))
    }
}
