package fr.sygix.sygixos.ui.home

import android.content.Context
import android.content.Intent
import android.widget.Toast
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp

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

    private fun start(context: Context, intent: Intent): Boolean = runCatching {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    }.getOrDefault(false)
}
