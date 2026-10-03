/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DockLayoutTest {

    private val available = 864f
    private val tile = 120f
    private val spacing = 14f
    private val padding = 10f

    @Test
    fun `dock wraps fixed size tiles whatever their count`() {
        assertEquals(140f, DockLayout.dockWidth(1, tile, spacing, padding, available))
        assertEquals(120f * 3 + 14f * 2 + 20f, DockLayout.dockWidth(3, tile, spacing, padding, available))
        assertEquals(120f * 6 + 14f * 5 + 20f, DockLayout.dockWidth(6, tile, spacing, padding, available))
    }

    @Test
    fun `dock never grows past the available width`() {
        assertEquals(available, DockLayout.dockWidth(8, tile, spacing, padding, available))
        assertFalse(DockLayout.overflows(6, tile, spacing, padding, available))
        assertTrue(DockLayout.overflows(7, tile, spacing, padding, available))
    }

    @Test
    fun `empty dock has no width`() {
        assertEquals(0f, DockLayout.contentWidth(0, tile, spacing, padding))
    }
}
