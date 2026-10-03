/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

class StartupSession {
    private var claimed = false

    @Synchronized
    fun claimColdStart(): Boolean {
        if (claimed) return false
        claimed = true
        return true
    }
}
