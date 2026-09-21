package fr.sygix.sygixos.domain

import fr.sygix.sygixos.model.TvApp

object AppCatalog {

    fun dock(apps: List<TvApp>, pinned: List<String>): List<TvApp> =
        pinned.mapNotNull { pkg -> apps.find { it.packageName == pkg } }

    fun grid(apps: List<TvApp>, pinned: List<String>): List<TvApp> =
        apps.filter { it.packageName !in pinned }.sortedBy { it.label.lowercase() }

    fun togglePinned(current: List<String>, packageName: String): List<String> =
        if (packageName in current) current - packageName else current + packageName
}
