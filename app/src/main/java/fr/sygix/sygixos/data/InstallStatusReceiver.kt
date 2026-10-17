/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller

fun interface InstallStatusClaim {
    fun claim(status: InstallStatus): Boolean
}

class InstallStatusRouter(private val claims: List<InstallStatusClaim>, private val fallback: (InstallStatus) -> Unit) {

    fun route(status: InstallStatus) {
        if (claims.none { it.claim(status) }) fallback(status)
    }
}

interface InstallStatusRouting {
    val installStatusRouter: InstallStatusRouter
}

class InstallStatusReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val status = InstallStatus.of(
            status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE),
            sessionId = intent.getIntExtra(PackageInstaller.EXTRA_SESSION_ID, -1),
            intent = intent.getParcelableExtra(Intent.EXTRA_INTENT, Intent::class.java),
        )
        (context.applicationContext as InstallStatusRouting).installStatusRouter.route(status)
    }
}
