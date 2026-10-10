/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

import kotlin.math.max
import kotlin.math.roundToInt

data class PixelSize(val width: Int, val height: Int)

object ImageBounds {
    const val MAX_SCREEN_WIDTH = 1920
    const val MAX_SCREEN_HEIGHT = 1080
    const val MAX_ARTWORK_WIDTH = 480
    const val MAX_ARTWORK_HEIGHT = 270

    fun fit(width: Int, height: Int, maxWidth: Int, maxHeight: Int): PixelSize {
        if (width <= 0 || height <= 0) return PixelSize(max(width, 1), max(height, 1))
        val ratio = minOf(maxWidth.toFloat() / width, maxHeight.toFloat() / height, 1f)
        return PixelSize(max((width * ratio).roundToInt(), 1), max((height * ratio).roundToInt(), 1))
    }

    fun screen(width: Int, height: Int): PixelSize = fit(width, height, MAX_SCREEN_WIDTH, MAX_SCREEN_HEIGHT)

    fun artwork(width: Int, height: Int): PixelSize = fit(width, height, MAX_ARTWORK_WIDTH, MAX_ARTWORK_HEIGHT)

    fun sampleSize(sourceWidth: Int, targetWidth: Int): Int {
        var sample = 1
        while (targetWidth > 0 && sourceWidth / (sample * 2) >= targetWidth) sample *= 2
        return sample
    }
}
