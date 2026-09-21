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
import fr.sygix.sygixos.data.VisualValidator
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
    /** URIs d'images validées (chargées, assez grandes) : seules celles-ci sont affichées. */
    val validated: Set<String> = emptySet(),
    /** URIs déjà vérifiées, validées ou non : ailleurs, la vérification est encore en cours. */
    val checked: Set<String> = emptySet(),
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
    private val validator: VisualValidator,
) : ViewModel() {

    private val heroState = MutableStateFlow(HeroState.Initial)
    private val validated = MutableStateFlow<Set<String>>(emptySet())
    private val checked = MutableStateFlow<Set<String>>(emptySet())
    private var heroValidationJob: Job? = null
    private var shelfValidationJob: Job? = null
    private val shelfRequested = mutableSetOf<String>()

    val state: StateFlow<HomeState> = combine(
        apps.catalog.onEach { preloadArtwork(it) },
        heroState,
        validated,
        checked,
    ) { catalog, h, v, c ->
        HomeState.Ready(catalog, h.copy(validated = v, checked = c)) as HomeState
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
            shelfRequested.clear()
            android.util.Log.w("HomeVM", "programmes par app: " + feed.items.groupingBy { it.sourcePackage ?: "?" }.eachCount().entries.sortedByDescending { it.value }.joinToString { "${it.key}=${it.value}" })
            // Le TV Provider peut publier des centaines de programmes : seuls les premiers
            // visuels du héro sont validés au chargement, le reste l'est à la demande.
            val heroUris = feed.items.asSequence()
                .mapNotNull { it.imageUrl }
                .distinct()
                .take(VisualQuality.HERO_VALIDATED)
                .toList()
            heroValidationJob?.cancel()
            heroValidationJob = launchValidation(heroUris, VisualQuality.HERO_IN_MEMORY)
        }
    }

    /** Focus posé sur une app de la grille : on valide quelques-unes de ses affiches. */
    fun prepareShelf(packageName: String) {
        if (!shelfRequested.add(packageName)) return
        val uris = ShelfPosters.candidates(heroState.value.items, packageName, VisualQuality.SHELF_VALIDATED_PER_APP)
        android.util.Log.w("HomeVM", "prepareShelf $packageName candidats=${uris.size}")
        shelfValidationJob?.cancel()
        shelfValidationJob = launchValidation(uris, keepInMemory = 0)
    }

    private fun launchValidation(uris: List<String>, keepInMemory: Int): Job? {
        val pending = uris.filter { it !in validated.value }
        if (pending.isEmpty()) return null
        return viewModelScope.launch {
            validator.validate(pending, keepInMemory).collect { check ->
                checked.update { it + check.uri }
                if (check.usable) validated.update { it + check.uri }
            }
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
                validator = VisualValidator(app),
            ) as T
        }
    }
}
