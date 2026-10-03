/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent

interface SystemScreenLauncher {
    fun launch(intent: Intent): Boolean
}

class ContextSystemScreenLauncher(private val context: Context) : SystemScreenLauncher {

    override fun launch(intent: Intent): Boolean = try {
        context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (e: ActivityNotFoundException) {
        false
    } catch (e: SecurityException) {
        false
    }
}
