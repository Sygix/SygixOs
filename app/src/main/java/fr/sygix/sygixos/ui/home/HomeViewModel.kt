package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.data.HeroRepository
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.NatureFallbackProvider
import fr.sygix.sygixos.data.LauncherPrefs
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HeroState(
    val items: List<HeroItem>,
    /** true : programmes des apps installées ; false : fond vidéo de secours. */
    val fromApps: Boolean,
    val loading: Boolean,
) {
    companion object {
        val Initial = HeroState(emptyList(), fromApps = false, loading = true)
    }
}

sealed interface HomeState {
    data object Loading : HomeState
    data class Ready(val catalog: Catalog, val hero: HeroState) : HomeState
}

class HomeViewModel(
    private val apps: AppCatalogRepository,
    private val hero: HeroRepository,
    val artwork: AppArtworkSource,
) : ViewModel() {

    private val heroState = MutableStateFlow(HeroState.Initial)

    val state: StateFlow<HomeState> = combine(
        apps.catalog.onEach { preloadArtwork(it) },
        heroState,
    ) { catalog, h ->
        HomeState.Ready(catalog, h) as HomeState
    }.stateIn(viewModelScope, SharingStarted.Eagerly, HomeState.Loading)

    private fun preloadArtwork(catalog: Catalog) {
        viewModelScope.launch(Dispatchers.IO) { artwork.preload(catalog.dock + catalog.grid) }
    }

    /** Au retour au launcher : apps installées et progression des programmes ont pu changer. */
    fun refresh() {
        refreshApps()
        refreshHero()
    }

    fun refreshApps() {
        viewModelScope.launch { apps.refreshApps() }
    }

    fun refreshHero() {
        viewModelScope.launch {
            heroState.update { it.copy(loading = true) }
            val feed = hero.load()
            heroState.value = HeroState(feed.items, feed.fromApps, loading = false)
        }
    }

    fun togglePin(app: TvApp) {
        viewModelScope.launch { apps.togglePin(app.packageName) }
    }

    fun moveInGrid(app: TvApp, delta: Int) {
        viewModelScope.launch { apps.moveInGrid(app.packageName, delta) }
    }

    fun setGridOrder(order: List<String>) {
        viewModelScope.launch { apps.setGridOrder(order) }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext
            return HomeViewModel(
                apps = AppCatalogRepository(InstalledAppsSource(app), LauncherPrefs(app)),
                hero = HeroRepository(TvProviderHeroSource(app), NatureFallbackProvider()),
                artwork = AppArtworkSource(app.packageManager),
            ) as T
        }
    }
}
