/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import org.junit.Assert.*
import org.junit.Test

class QrGridTest {
    @Test fun modulesUseWholePixelsAndStayCenteredInsideTheCanvas() {
        val qr = requireNotNull(QrCode.encode("https://www.betaseries.com/device"))
        for (pixels in listOf(160, 213, 240, 360)) {
            val grid = qr.grid(pixels)
            assertEquals(pixels / qr.size, grid.module)
            assertTrue(grid.module >= 1)
            assertTrue(grid.origin + grid.module * qr.size <= pixels)
            assertTrue(pixels - grid.module * qr.size - 2 * grid.origin in 0..1)
        }
    }
}
