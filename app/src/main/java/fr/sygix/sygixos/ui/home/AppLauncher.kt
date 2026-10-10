/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import fr.sygix.sygixos.R

object AppLauncher {

    fun open(context: Context, app: TvApp) {
        val pm = context.packageManager
        val intent = pm.getLeanbackLaunchIntentForPackage(app.packageName)
            ?: pm.getLaunchIntentForPackage(app.packageName)
        if (intent == null || !start(context, intent)) {
            Toast.makeText(context, "Impossible d'ouvrir ${app.label}", Toast.LENGTH_SHORT).show()
        }
    }

    fun open(context: Context, item: HeroItem) {
        val published = item.launchUri?.let { uri ->
            runCatching { Intent.parseUri(uri, Intent.URI_INTENT_SCHEME) }.getOrNull()
        }
        if (published != null && start(context, published)) return
        item.sourcePackage?.let { open(context, TvApp(it, item.sourceLabel ?: it)) }
    }

    fun open(context: Context, item: UpNextItem) = open(context, item, item.sources.first(), fromMenu = false)

    fun openWith(context: Context, item: UpNextItem, source: UpNextSourceEntry) = open(context, item, source, fromMenu = true)

    private fun open(context: Context, item: UpNextItem, source: UpNextSourceEntry, fromMenu: Boolean) {
        val published = source.intentUri?.let { uri ->
            runCatching { Intent.parseUri(uri, Intent.URI_INTENT_SCHEME).setPackage(source.packageName) }.getOrNull()
        }
        if (published != null && start(context, published)) return
        val pm = context.packageManager
        val fallback = pm.getLeanbackLaunchIntentForPackage(source.packageName)
            ?: pm.getLaunchIntentForPackage(source.packageName)
        if (fallback != null && start(context, fallback)) {
            if (fromMenu && source.intentUri != null) {
                Toast.makeText(context, context.getString(R.string.upnext_open_fallback, source.label), Toast.LENGTH_SHORT).show()
            }
            return
        }
        Toast.makeText(context, context.getString(R.string.upnext_open_error, item.displayTitle), Toast.LENGTH_SHORT).show()
    }

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
