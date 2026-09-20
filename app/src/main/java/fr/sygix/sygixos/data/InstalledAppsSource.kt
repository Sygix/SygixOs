package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import fr.sygix.sygixos.model.TvApp

class InstalledAppsSource(private val context: Context) {

    fun load(): List<TvApp> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER)
        return pm.queryIntentActivities(intent, 0)
            .mapNotNull { info ->
                val pkg = info.activityInfo.packageName
                if (pkg == context.packageName) return@mapNotNull null
                TvApp(
                    packageName = pkg,
                    label = info.loadLabel(pm)?.toString().orEmpty().ifEmpty { pkg },
                )
            }
            .distinctBy { it.packageName }
    }
}
