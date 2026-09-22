/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object AccentColor {

    private const val NEUTRAL = 0xFFB4B4BE.toInt()

    fun compute(pixels: IntArray): Int {
        var r = 0.0
        var g = 0.0
        var b = 0.0
        var total = 0.0
        for (p in pixels) {
            if ((p ushr 24) and 0xFF < 128) continue
            val pr = (p shr 16) and 0xFF
            val pg = (p shr 8) and 0xFF
            val pb = p and 0xFF
            val max = maxOf(pr, pg, pb)
            val min = minOf(pr, pg, pb)
            val saturation = if (max == 0) 0.0 else (max - min).toDouble() / max
            val weight = saturation * saturation * (max / 255.0) + 0.01
            r += pr * weight
            g += pg * weight
            b += pb * weight
            total += weight
        }
        if (total == 0.0) return NEUTRAL
        return argb((r / total).toInt(), (g / total).toInt(), (b / total).toInt())
    }

    fun vivid(color: Int, minSaturation: Float = 0.55f, minValue: Float = 0.8f): Int {
        val r = ((color shr 16) and 0xFF) / 255f
        val g = ((color shr 8) and 0xFF) / 255f
        val b = (color and 0xFF) / 255f
        val max = maxOf(r, g, b)
        val min = minOf(r, g, b)
        val delta = max - min
        val hue = when {
            delta == 0f -> 0f
            max == r -> 60f * (((g - b) / delta) % 6f)
            max == g -> 60f * (((b - r) / delta) + 2f)
            else -> 60f * (((r - g) / delta) + 4f)
        }.let { if (it < 0f) it + 360f else it }
        val saturation = if (max == 0f) 0f else delta / max
        if (saturation < 0.12f) return NEUTRAL
        return hsv(hue, maxOf(saturation, minSaturation), maxOf(max, minValue))
    }

    private fun hsv(h: Float, s: Float, v: Float): Int {
        val c = v * s
        val x = c * (1f - kotlin.math.abs((h / 60f) % 2f - 1f))
        val m = v - c
        val (r1, g1, b1) = when {
            h < 60f -> Triple(c, x, 0f)
            h < 120f -> Triple(x, c, 0f)
            h < 180f -> Triple(0f, c, x)
            h < 240f -> Triple(0f, x, c)
            h < 300f -> Triple(x, 0f, c)
            else -> Triple(c, 0f, x)
        }
        return argb(((r1 + m) * 255).toInt(), ((g1 + m) * 255).toInt(), ((b1 + m) * 255).toInt())
    }

    private fun argb(r: Int, g: Int, b: Int): Int =
        (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or b.coerceIn(0, 255)
}
