/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Intent

class RoleRequestLauncher {
    @Volatile private var launcher: ((Intent) -> Unit)? = null

    fun bind(launcher: (Intent) -> Unit) {
        this.launcher = launcher
    }

    fun unbind(launcher: (Intent) -> Unit) {
        if (this.launcher === launcher) this.launcher = null
    }

    fun launch(intent: Intent): Boolean {
        val current = launcher ?: return false
        return runCatching { current(intent) }.isSuccess
    }
}
