/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.ui.settings.HomeScreenActions
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import fr.sygix.sygixos.ui.settings.SettingsState
import fr.sygix.sygixos.ui.settings.UpdateActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal fun app(pkg: String, label: String) = TvApp(packageName = pkg, label = label)

internal fun heroItem(
    id: String,
    title: String,
    imageUrl: String? = null,
    sourcePackage: String? = null,
    progress: Float? = null,
) = HeroItem(
    id = id,
    title = title,
    imageUrl = imageUrl,
    sourcePackage = sourcePackage,
    durationMillis = progress?.let { PROGRESS_SCALE },
    positionMillis = progress?.let { (it * PROGRESS_SCALE).toLong() },
)

private const val PROGRESS_SCALE = 1_000_000L

internal fun catalogOf(dock: List<TvApp>, grid: List<TvApp>) = Catalog(dock = dock, grid = grid)

internal fun heroStateOf(
    items: List<HeroItem>,
    validated: Set<String> = emptySet(),
    checked: Set<String> = emptySet(),
    fromApps: Boolean = true,
) = HeroState(items = items, fromApps = fromApps, validated = validated, checked = checked)

@Composable
internal fun TestHome(
    catalog: Catalog,
    hero: HeroState,
    initialZone: Zone = Zone.HERO,
    onTogglePin: (TvApp) -> Unit = {},
    onHide: (TvApp) -> Unit = {},
    settings: SettingsState? = null,
    glassBlur: Boolean = false,
    counts: StateFlow<Map<String, Int>?> = MutableStateFlow(emptyMap()),
    clock: StateFlow<String> = MutableStateFlow("21:47"),
    updateBadge: StateFlow<Boolean> = MutableStateFlow(false),
    update: UpdateActions = UpdateActions(),
    onHeroVisualReady: () -> Unit = {},
    interactive: Boolean = true,
    onAppFocused: (String) -> Unit = {},
    onOpenHero: (HeroItem) -> Unit = {},
    artworkSource: AppArtworkSource? = null,
    launcherState: LauncherSystemState = LauncherSystemState(),
    launcherActions: LauncherSystemActions = LauncherSystemActions(),
    onRestoreOrder: (List<String>) -> Unit = {},
    upNext: UpNextState? = null,
    homeScreenActions: HomeScreenActions = HomeScreenActions(),
) {
    val packageManager = LocalContext.current.packageManager
    val artwork = remember(packageManager, artworkSource) { artworkSource ?: AppArtworkSource(packageManager, Dispatchers.Unconfined) }
    CompositionLocalProvider(LocalAppArtwork provides artwork) {
        MaterialTheme {
            LauncherHome(
                catalog = catalog,
                hero = hero,
                onTogglePin = onTogglePin,
                initialZone = initialZone,
                glassBlur = glassBlur,
                onHideApp = onHide,
                settings = settings,
                counts = counts,
                clock = clock,
                updateBadge = updateBadge,
                update = update,
                onHeroVisualReady = onHeroVisualReady,
                interactive = interactive,
                onAppFocused = onAppFocused,
                onOpenHero = onOpenHero,
                launcherState = launcherState,
                launcherActions = launcherActions,
                onRestoreOrder = onRestoreOrder,
                upNext = upNext,
                homeScreenActions = homeScreenActions,
            )
        }
    }
}

internal data class Span(val top: Float, val bottom: Float)

internal fun ComposeContentTestRule.span(tag: String): Span {
    val node = onNodeWithTag(tag).fetchSemanticsNode()
    val top = node.positionInRoot.y
    return Span(top, top + node.size.height)
}

internal fun ComposeContentTestRule.screenHeight(): Float = onRoot().fetchSemanticsNode().size.height.toFloat()
