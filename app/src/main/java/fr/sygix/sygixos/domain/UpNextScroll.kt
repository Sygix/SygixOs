/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlin.math.ceil

object UpNextScroll {
    fun neighbor(index: Int, step: Int, count: Int): Int = (index + step).coerceIn(0, (count - 1).coerceAtLeast(0))

    fun offsetOf(index: Int, anchorIndex: Int, anchorOffset: Int, itemSize: Int, spacing: Int): Int =
        anchorOffset + (index - anchorIndex) * (itemSize + spacing)

    fun focusOverflow(size: Int, scale: Float): Int = ceil(size * (scale - 1f) / 2f).toInt()

    fun revealDelta(offset: Int, size: Int, viewportEnd: Int, endInset: Int = 0): Int =
        if (offset < 0) offset else (offset + size + endInset - viewportEnd).coerceAtLeast(0)
}
