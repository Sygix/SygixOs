/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import fr.sygix.sygixos.data.AppCatalogRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class AndroidPlaybackApps(private val context: Context, private val catalog: AppCatalogRepository, private val knownVideoPackages: () -> Set<String> = { emptySet() }) : PlaybackAppResolver {
    override suspend fun installed(): List<PlaybackApp> = withContext(Dispatchers.IO) {
        catalog.allApps.first().filter { it.packageName != context.packageName }.map { app ->
            val category = runCatching { context.packageManager.getApplicationInfo(app.packageName, 0).category }.getOrNull()
            PlaybackApp(app.packageName, app.label, category == ApplicationInfo.CATEGORY_VIDEO || app.packageName in knownVideoPackages())
        }
    }
}

class AndroidPlaybackLauncher(private val context: Context) : PlaybackLauncher {
    override fun launch(app: PlaybackApp, request: PlaybackRequest): PlaybackPrecision? {
        val target = request.targets.firstOrNull { it.packageName == app.packageName && it.identity == request.content.identity }
        if (target?.intentUri != null) {
            val intent = runCatching {
                Intent.parseUri(target.intentUri, Intent.URI_INTENT_SCHEME).takeIf { parsed ->
                    (parsed.component == null || parsed.component?.packageName == app.packageName) && parsed.data?.scheme !in setOf("file", "javascript", "data")
                }?.apply { selector = null; setPackage(app.packageName) }
            }.getOrNull()
            if (intent != null && start(intent)) return PlaybackPrecision.CONTENT
        }
        val intent = context.packageManager.run { getLeanbackLaunchIntentForPackage(app.packageName) ?: getLaunchIntentForPackage(app.packageName) }
        return if (intent != null && start(intent)) {
            android.widget.Toast.makeText(context, context.getString(fr.sygix.sygixos.R.string.tracking_fallback_app), android.widget.Toast.LENGTH_LONG).show()
            PlaybackPrecision.APPLICATION
        } else null
    }

    private fun start(intent: Intent): Boolean = runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)); true }.getOrDefault(false)
}
