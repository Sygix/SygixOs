/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

data class HomeKeyEvent(
    val down: Boolean,
    val repeatCount: Int = 0,
    val longPress: Boolean = false,
    val canceled: Boolean = false,
    val eventTime: Long,
)

class HomeKeyFilter(
    private val longPressMs: Long,
    private val foreground: () -> Boolean,
    private val openHome: (wasForeground: Boolean) -> Unit,
) {
    private var downAt: Long? = null
    private var held = false
    private var wasForeground = false

    fun filter(event: HomeKeyEvent): Boolean {
        if (event.down) {
            if (event.repeatCount == 0 && !event.longPress) {
                downAt = event.eventTime
                held = false
                wasForeground = runCatching(foreground).getOrDefault(false)
            } else {
                held = true
            }
            return false
        }
        val start = downAt
        val short = start != null && !held && !event.longPress && !event.canceled && event.eventTime - start < longPressMs
        val front = wasForeground
        reset()
        if (short) runCatching { openHome(front) }
        return false
    }

    fun reset() {
        downAt = null
        held = false
        wasForeground = false
    }
}
