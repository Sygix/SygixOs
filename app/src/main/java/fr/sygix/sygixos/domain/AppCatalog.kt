package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp

object AppCatalog {

    fun order(apps: List<TvApp>, pinned: List<String>): List<TvApp> {
        val rank = pinned.withIndex().associate { (i, pkg) -> pkg to i }
        return apps.sortedWith(
            compareBy(
                { app -> rank[app.packageName] ?: Int.MAX_VALUE },
                { app -> app.label.lowercase() },
            ),
        )
    }

    fun togglePinned(current: List<String>, packageName: String): List<String> =
        if (packageName in current) current - packageName else current + packageName
}
