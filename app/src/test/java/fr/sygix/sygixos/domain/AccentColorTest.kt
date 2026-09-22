/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AccentColorTest {

    private fun red(c: Int) = (c shr 16) and 0xFF
    private fun green(c: Int) = (c shr 8) and 0xFF
    private fun blue(c: Int) = c and 0xFF

    @Test
    fun `saturated brand color dominates a mostly white banner`() {
        val pixels = IntArray(100) { if (it < 90) 0xFFFFFFFF.toInt() else 0xFFE50914.toInt() }
        val accent = AccentColor.compute(pixels)
        assertTrue(red(accent) > 200)
        assertTrue(green(accent) < 120 && blue(accent) < 120)
    }

    @Test
    fun `neutral image gives the neutral fallback`() {
        val pixels = IntArray(50) { 0xFF202020.toInt() }
        assertEquals(0xFFB4B4BE.toInt(), AccentColor.vivid(AccentColor.compute(pixels)))
    }

    @Test
    fun `vivid keeps hue and lifts saturation and value`() {
        val vivid = AccentColor.vivid(0xFF402020.toInt())
        assertTrue(red(vivid) >= 200)
        assertTrue(red(vivid) > green(vivid) && red(vivid) > blue(vivid))
        assertEquals(green(vivid), blue(vivid))
    }
}
