/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SourceRow(val app: TvApp, val enabled: Boolean)

data class SettingsState(
    val sources: List<SourceRow> = emptyList(),
    val hiddenApps: List<TvApp> = emptyList(),
    val version: String = "",
)

class SettingsViewModel(
    private val apps: AppCatalogRepository,
    private val tvProvider: TvProviderHeroSource,
    private val pm: PackageManager,
    private val selfPackage: String,
) : ViewModel() {

    val icons: AppIconCache = AppIconCache.forPackageManager(pm)

    private val version: String = runCatching {
        pm.getPackageInfo(selfPackage, 0).versionName
    }.getOrNull().orEmpty()

    // Comptages exposés séparément : seules les lignes dont le compte change se recomposent.
    // WhileSubscribed : aucun scan du TV Provider tant que les réglages ne sont pas affichés.
    val counts: StateFlow<Map<String, Int>> = tvProvider.programCountsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyMap())

    val state: StateFlow<SettingsState> = combine(
        apps.disabledSources,
        apps.allApps,
        apps.hidden,
    ) { disabled, allApps, hidden ->
        val sourceApps = allApps.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
        SettingsState(
            sources = sourceApps.map { app ->
                SourceRow(
                    app = app,
                    enabled = app.packageName !in disabled,
                )
            },
            hiddenApps = sourceApps.filter { it.packageName in hidden },
            version = version,
        )
    }.stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    fun toggleSource(packageName: String) {
        // Bascule atomique : lecture et écriture dans la même transaction DataStore.
        viewModelScope.launch {
            apps.toggleSource(packageName)
        }
    }

    fun unhide(packageName: String) {
        viewModelScope.launch { apps.unhideApp(packageName) }
    }

    fun unhideAll() {
        viewModelScope.launch { apps.unhideAll() }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as SygixOsApp
            return SettingsViewModel(
                apps = app.appCatalogRepository,
                tvProvider = app.tvProviderHeroSource,
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
