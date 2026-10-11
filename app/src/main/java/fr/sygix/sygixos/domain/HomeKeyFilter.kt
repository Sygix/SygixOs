/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

data class HomeKeyEvent(
    val home: Boolean = true,
    val down: Boolean,
    val repeatCount: Int = 0,
    val canceled: Boolean = false,
)

class HomeKeyFilter(
    private val foreground: () -> Boolean,
    private val openHome: (wasForeground: Boolean) -> Unit,
) {
    private var pressed = false
    private var wasForeground = false

    fun filter(event: HomeKeyEvent, connected: Boolean): Boolean {
        if (!event.home) return false
        if (!connected) {
            reset()
            return false
        }
        if (event.down) {
            if (event.repeatCount == 0) {
                pressed = true
                wasForeground = runCatching(foreground).getOrDefault(false)
            }
            return pressed
        }
        if (!pressed) return false
        val front = wasForeground
        reset()
        if (!event.canceled) runCatching { openHome(front) }
        return true
    }

    fun reset() {
        pressed = false
        wasForeground = false
    }
}
