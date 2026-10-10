/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import fr.sygix.sygixos.ui.MainActivity
import java.util.UUID

object LauncherSystemIntents {
    private const val TOKEN = "fr.sygix.sygixos.LAUNCHER_TOKEN"
    private const val SOURCE = "fr.sygix.sygixos.LAUNCHER_SOURCE"
    private const val FOREGROUND = "fr.sygix.sygixos.LAUNCHER_FOREGROUND"
    private val token = UUID.randomUUID().toString()
    private const val BOOT = "boot"
    private const val ACCESSIBILITY = "accessibility"

    fun boot(context: Context): Intent = base(context).putExtra(SOURCE, BOOT)
    fun home(context: Context, wasForeground: Boolean): Intent =
        base(context).putExtra(SOURCE, ACCESSIBILITY).putExtra(FOREGROUND, wasForeground)

    fun isBoot(intent: Intent): Boolean = trusted(intent) && intent.getStringExtra(SOURCE) == BOOT
    fun isForegroundHome(intent: Intent): Boolean =
        trusted(intent) && intent.getStringExtra(SOURCE) == ACCESSIBILITY && intent.getBooleanExtra(FOREGROUND, false)

    private fun trusted(intent: Intent): Boolean = intent.getStringExtra(TOKEN) == token
    private fun base(context: Context): Intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        .putExtra(TOKEN, token)
}
