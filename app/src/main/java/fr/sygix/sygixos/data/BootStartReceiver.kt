/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.sygix.sygixos.SygixOsApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootStartReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? SygixOsApp ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                BootStartDispatcher(app.launcherPrefs) {
                    runCatching { context.startActivity(LauncherSystemIntents.boot(context)) }.isSuccess
                }.handle(intent.action)
            } finally {
                pending.finish()
            }
        }
    }
}
