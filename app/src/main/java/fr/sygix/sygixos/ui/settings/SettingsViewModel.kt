/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.domain.SettingsOrdering
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourceRow(val app: TvApp, val enabled: Boolean)

data class HiddenRow(val app: TvApp, val hidden: Boolean, val hiddenAt: Long?)

data class SettingsState(
    val sources: List<SourceRow> = emptyList(),
    val hiddenRows: List<HiddenRow> = emptyList(),
    val version: String = "",
) {
    fun canEnter(category: SettingsCategory): Boolean = when (category) {
        SettingsCategory.SOURCES -> sources.isNotEmpty()
        SettingsCategory.HIDDEN -> hiddenRows.isNotEmpty()
        SettingsCategory.ABOUT -> true
    }
}

class SettingsViewModel(
    private val apps: AppCatalogRepository,
    programCounts: Flow<Map<String, Int>>,
    private val pm: PackageManager,
    private val selfPackage: String,
) : ViewModel() {

    val icons: AppIconCache = AppIconCache.forPackageManager(pm)

    private val version: String = runCatching {
        pm.getPackageInfo(selfPackage, 0).versionName
    }.getOrNull().orEmpty()

    private val library: StateFlow<Library?> = combine(apps.allApps, apps.hiddenWithDates) { installed, dates ->
        Library(installed.distinctBy { it.packageName }, dates)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val sourceCounts = MutableStateFlow<Map<String, Int>?>(null)

    val counts: StateFlow<Map<String, Int>?> = programCounts
        .onEach { latest -> sourceCounts.update { it ?: latest } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    private val hiddenOrder = MutableStateFlow<List<String>?>(null)

    val state: StateFlow<SettingsState> = combine(
        apps.disabledSources,
        library,
        sourceCounts,
        hiddenOrder,
    ) { disabled, current, frozenCounts, order ->
        val installed = current?.apps.orEmpty()
        val dates = current?.hiddenDates.orEmpty()
        val byPackage = installed.associateBy { it.packageName }
        SettingsState(
            sources = SettingsOrdering.sources(installed, frozenCounts.orEmpty()).map { app ->
                SourceRow(app = app, enabled = app.packageName !in disabled)
            },
            hiddenRows = order.orEmpty().mapNotNull { pkg ->
                byPackage[pkg]?.let { HiddenRow(app = it, hidden = pkg in dates, hiddenAt = dates[pkg]) }
            },
            version = version,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    init {
        viewModelScope.launch {
            val loaded = library.filterNotNull().first()
            hiddenOrder.update { it ?: hiddenSnapshot(loaded) }
        }
    }

    fun enterCategory(category: SettingsCategory) {
        when (category) {
            SettingsCategory.SOURCES -> sourceCounts.value = counts.value
            SettingsCategory.HIDDEN -> refreshHiddenRows()
            SettingsCategory.ABOUT -> Unit
        }
    }

    fun refreshHiddenRows() {
        hiddenOrder.value = library.value?.let(::hiddenSnapshot)
    }

    private fun hiddenSnapshot(library: Library): List<String> =
        SettingsOrdering.hidden(library.apps.filter { it.packageName in library.hiddenDates }, library.hiddenDates)
            .map { it.packageName }

    fun toggleSource(packageName: String) {
        viewModelScope.launch { apps.toggleSource(packageName) }
    }

    fun toggleHidden(packageName: String) {
        viewModelScope.launch { apps.toggleHidden(packageName) }
    }

    fun unhideAll() {
        val listed = hiddenOrder.value.orEmpty()
        viewModelScope.launch { apps.unhideApps(listed) }
    }

    private data class Library(val apps: List<TvApp>, val hiddenDates: Map<String, Long?>)

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            return SettingsViewModel(
                apps = app.appCatalogRepository,
                programCounts = app.tvProviderHeroSource.programCountsFlow(),
                pm = app.packageManager,
                selfPackage = app.packageName,
            ) as T
        }
    }
}

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val context = LocalContext.current
    return viewModel(factory = SettingsViewModel.Factory(context))
}
