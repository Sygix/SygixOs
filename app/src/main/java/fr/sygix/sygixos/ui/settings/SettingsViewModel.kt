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
import fr.sygix.sygixos.data.UpdateController
import fr.sygix.sygixos.domain.HiddenGroup
import fr.sygix.sygixos.domain.HiddenPaneLayout
import fr.sygix.sygixos.domain.SettingsOrdering
import fr.sygix.sygixos.domain.UpNextContentKey
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextPosition
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SourceRow(val app: TvApp, val enabled: Boolean)

data class HiddenRow(val app: TvApp, val hidden: Boolean, val hiddenAt: Long?)

data class HiddenContentRow(
    val key: UpNextContentKey,
    val title: String,
    val apps: List<String>,
    val imageUrl: String?,
    val removedAt: Long,
)

sealed interface HiddenEntry {
    val key: String

    data object UnhideAll : HiddenEntry {
        override val key: String = "unhide-all"
    }

    data class App(val row: HiddenRow) : HiddenEntry {
        override val key: String get() = "app:${row.app.packageName}"
    }

    data class More(val group: HiddenGroup, val count: Int) : HiddenEntry {
        override val key: String get() = "more:${group.name}"
    }

    data object ContentsTitle : HiddenEntry {
        override val key: String = "contents-title"
    }

    data class Content(val row: HiddenContentRow) : HiddenEntry {
        override val key: String get() = "content:${row.key.serialized}"
    }
}

internal fun buildHiddenEntries(apps: List<HiddenRow>, contents: List<HiddenContentRow>, expanded: Set<HiddenGroup>): List<HiddenEntry> = buildList {
    if (apps.isNotEmpty()) {
        val open = HiddenGroup.APPS in expanded
        add(HiddenEntry.UnhideAll)
        apps.take(HiddenPaneLayout.shown(apps.size, open)).forEach { add(HiddenEntry.App(it)) }
        HiddenPaneLayout.more(apps.size, open).takeIf { it > 0 }?.let { add(HiddenEntry.More(HiddenGroup.APPS, it)) }
    }
    if (contents.isNotEmpty()) {
        val open = HiddenGroup.UPNEXT in expanded
        add(HiddenEntry.ContentsTitle)
        contents.take(HiddenPaneLayout.shown(contents.size, open)).forEach { add(HiddenEntry.Content(it)) }
        HiddenPaneLayout.more(contents.size, open).takeIf { it > 0 }?.let { add(HiddenEntry.More(HiddenGroup.UPNEXT, it)) }
    }
}

data class HiddenRefocus(val key: String, val request: Long)

data class SettingsState(
    val sources: List<SourceRow> = emptyList(),
    val hiddenRows: List<HiddenRow> = emptyList(),
    val version: String = "",
    val update: AboutUpdateState = AboutUpdateState(),
    val upNextVisible: Boolean = true,
    val upNextPosition: UpNextPosition = UpNextPosition.BEFORE_APPS,
    val hiddenContents: List<HiddenContentRow> = emptyList(),
    val hiddenExpanded: Set<HiddenGroup> = emptySet(),
    val hiddenRefocus: HiddenRefocus? = null,
) {
    val hiddenEntries: List<HiddenEntry> by lazy { buildHiddenEntries(hiddenRows, hiddenContents, hiddenExpanded) }

    val hiddenFocusOrder: List<String> by lazy { hiddenEntries.filter { it != HiddenEntry.ContentsTitle }.map { it.key } }

    val hiddenInitialFocus: String? by lazy { hiddenEntries.firstOrNull { it is HiddenEntry.App || it is HiddenEntry.Content }?.key }

    fun canEnter(category: SettingsCategory): Boolean = when (category) {
        SettingsCategory.SOURCES -> sources.isNotEmpty()
        SettingsCategory.HIDDEN -> hiddenRows.isNotEmpty() || hiddenContents.isNotEmpty()
        SettingsCategory.ABOUT, SettingsCategory.HOME_SCREEN -> true
    }
}

class SettingsViewModel(
    private val apps: AppCatalogRepository,
    programCounts: Flow<Map<String, Int>>,
    private val pm: PackageManager,
    private val selfPackage: String,
    private val updates: UpdateController,
    private val labelOf: (String) -> String? = { pkg ->
        runCatching { pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString() }.getOrNull()
    },
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

    private val hiddenExpanded = MutableStateFlow<Set<HiddenGroup>>(emptySet())

    private val hiddenVisit = MutableStateFlow(0L)
    private var hiddenFocused: String? = null
    private var hiddenFocusedIndex = 0
    private var hiddenShownOrder: List<String>? = null
    private var hiddenShownVisit = -1L
    private var hiddenRefocus: HiddenRefocus? = null
    private var hiddenRefocusRequests = 0L

    private val hiddenContents: Flow<List<HiddenContentRow>> = combine(apps.upNextRemoved.catch { emit(emptyList()) }, library) { removed, current ->
        val labels = current?.apps.orEmpty().associate { it.packageName to it.label }
        removed.sortedByDescending { it.removedAt }.map { content ->
            HiddenContentRow(
                key = content.key,
                title = content.title,
                apps = content.packages.mapNotNull { labels[it] ?: labelOf(it) },
                imageUrl = content.imageUrl,
                removedAt = content.removedAt,
            )
        }
    }

    private val updateMapper = AboutUpdateMapper()

    private val aboutUpdate = combine(updates.status, updates.systemScreenReturns) { status, returns -> updateMapper.map(status, returns) }

    val updateActions = UpdateActions(
        onCheck = updates::check,
        onInstall = updates::startUpdate,
        onTogglePrereleases = updates::togglePrereleases,
    )

    val state: StateFlow<SettingsState> = combine(
        apps.disabledSources,
        library,
        sourceCounts,
        hiddenOrder,
        aboutUpdate,
    ) { disabled, current, frozenCounts, order, update ->
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
            update = update,
        )
    }.combine(combine(apps.upNextVisible, apps.upNextPosition) { visible, position -> visible to position }) { state, preferences ->
        state.copy(upNextVisible = preferences.first, upNextPosition = preferences.second)
    }.combine(combine(hiddenContents, hiddenExpanded) { contents, expanded -> contents to expanded }) { state, hidden ->
        state.copy(hiddenContents = hidden.first, hiddenExpanded = hidden.second)
    }.combine(hiddenVisit) { state, visit -> withHiddenRefocus(state, visit) }
        .stateIn(viewModelScope, SharingStarted.Eagerly, SettingsState())

    init {
        viewModelScope.launch {
            val loaded = library.filterNotNull().first()
            hiddenOrder.update { it ?: hiddenSnapshot(loaded) }
        }
    }

    fun enterCategory(category: SettingsCategory) {
        when (category) {
            SettingsCategory.SOURCES -> sourceCounts.value = counts.value
            SettingsCategory.HIDDEN -> {
                hiddenExpanded.value = emptySet()
                refreshHiddenRows()
                hiddenFocused = null
                hiddenFocusedIndex = 0
                hiddenVisit.update { it + 1 }
            }
            SettingsCategory.ABOUT, SettingsCategory.HOME_SCREEN -> Unit
        }
    }

    fun refreshHiddenRows() {
        hiddenOrder.value = library.value?.let(::hiddenSnapshot)
    }

    val homeScreenActions = HomeScreenActions(
        onToggleUpNext = { viewModelScope.launch { apps.setUpNextVisible(!state.value.upNextVisible) } },
        onUpNextPosition = { position -> viewModelScope.launch { apps.setUpNextPosition(position) } },
    )

    private fun hiddenSnapshot(library: Library): List<String> =
        SettingsOrdering.hidden(library.apps.filter { it.packageName in library.hiddenDates }, library.hiddenDates)
            .map { it.packageName }

    fun onHiddenFocused(key: String) {
        hiddenFocused = key
        hiddenFocusedIndex = hiddenShownOrder.orEmpty().indexOf(key).coerceAtLeast(0)
    }

    private fun withHiddenRefocus(state: SettingsState, visit: Long): SettingsState {
        val order = state.hiddenFocusOrder
        if (visit != hiddenShownVisit) {
            hiddenShownVisit = visit
            hiddenRefocus = null
        } else if (order != hiddenShownOrder) {
            hiddenRefocus = HiddenPaneLayout.refocus(order, hiddenFocused, hiddenFocusedIndex)?.let { HiddenRefocus(it, ++hiddenRefocusRequests) }
        }
        hiddenShownOrder = order
        return state.copy(hiddenRefocus = hiddenRefocus)
    }

    fun expandHidden(group: HiddenGroup) {
        hiddenExpanded.update { it + group }
    }

    fun restoreUpNext(key: UpNextContentKey) {
        viewModelScope.launch { apps.restoreUpNext(key) }
    }

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
                updates = app.updateRepository,
            ) as T
        }
    }
}

@Composable
fun rememberSettingsViewModel(): SettingsViewModel {
    val context = LocalContext.current
    return viewModel(factory = SettingsViewModel.Factory(context))
}
