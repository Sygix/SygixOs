/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class GridTileTest {

    @Test
    fun `a 1080p TV has 332 px wide focused grid tiles`() {
        assertEquals(332, Dimens.gridTileWidthPx(960, 2f))
    }

    @Test
    fun `a denser screen has wider tiles`() {
        assertEquals(664, Dimens.gridTileWidthPx(960, 4f))
    }
}
