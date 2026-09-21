package fr.sygix.sygixos.ui.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import fr.sygix.sygixos.data.AppCatalogRepository
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.data.InstalledAppsSource
import fr.sygix.sygixos.data.PinnedAppsStore
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeState {
    data object Loading : HomeState
    data class Ready(val catalog: Catalog) : HomeState
}

class HomeViewModel(private val repository: AppCatalogRepository) : ViewModel() {

    val state: StateFlow<HomeState> = repository.catalog
        .map { HomeState.Ready(it) as HomeState }
        .stateIn(viewModelScope, SharingStarted.Eagerly, HomeState.Loading)

    fun togglePin(app: TvApp) {
        val current = (state.value as? HomeState.Ready)?.catalog?.grid?.map { it.packageName }
            ?.plus((state.value as? HomeState.Ready)?.catalog?.dock?.map { it.packageName } ?: emptyList())
            ?: return
        viewModelScope.launch {
            repository.togglePin(current, app.packageName)
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val repository = AppCatalogRepository(
                InstalledAppsSource(context.applicationContext),
                PinnedAppsStore(context.applicationContext),
            )
            return HomeViewModel(repository) as T
        }
    }
}
