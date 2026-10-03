/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.assertTrue
import org.junit.Test

class GlassContrastTest {

    private fun contrast(a: Color, b: Color): Float {
        val la = a.luminance()
        val lb = b.luminance()
        return (maxOf(la, lb) + 0.05f) / (minOf(la, lb) + 0.05f)
    }

    @Test
    fun `menu text stays readable on the darkest case of a white tile behind`() {
        val panel = SygixColors.MenuTint.compositeOver(Color.White)
        assertTrue(contrast(SygixColors.OnDark, panel) >= 4.5f)
        assertTrue(contrast(SygixColors.OnDarkSecondary.compositeOver(panel), panel) >= 3f)
    }

    @Test
    fun `focused pill text is dark on light`() {
        assertTrue(contrast(SygixColors.OnPill, SygixColors.PillFocus) >= 4.5f)
        assertTrue(contrast(SygixColors.OnPillSecondary, SygixColors.PillFocus) >= 4.5f)
    }

    @Test
    fun `menu panel is darker than the dock glass`() {
        val menu = SygixColors.MenuTint.compositeOver(Color.White).luminance()
        val dock = SygixColors.GlassTint.compositeOver(Color.White).luminance()
        assertTrue(menu < dock)
        val menuFallback = GlassLook.Menu.fallback.compositeOver(Color.White).luminance()
        val dockFallback = GlassLook.Dock.fallback.compositeOver(Color.White).luminance()
        assertTrue(menuFallback < dockFallback)
    }
}
