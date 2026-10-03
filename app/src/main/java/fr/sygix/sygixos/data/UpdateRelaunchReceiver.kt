/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class UpdateRelaunchReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        val app = context.applicationContext as SygixOsApp
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                relaunchIfRequested(app, app.updatePrefs, installedVersionCode(app))
            } finally {
                pending.finish()
            }
        }
    }

    companion object {
        fun installedVersionCode(context: Context): Long? =
            runCatching { context.packageManager.getPackageInfo(context.packageName, 0).longVersionCode }.getOrNull()

        suspend fun relaunchIfRequested(context: Context, store: UpdateStore, installedVersionCode: Long?) {
            val target = store.takeRelaunch()
            if (target != null && target == installedVersionCode) {
                context.startActivity(Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            }
        }
    }
}
