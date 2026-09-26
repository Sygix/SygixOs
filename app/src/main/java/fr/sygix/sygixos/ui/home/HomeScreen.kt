/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.LocalHazeState
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.hero.AmbientGradient
import fr.sygix.sygixos.ui.hero.HeroStage
import fr.sygix.sygixos.ui.settings.LocalAppIcons
import fr.sygix.sygixos.ui.settings.SettingsScreen
import fr.sygix.sygixos.ui.settings.SettingsState
import fr.sygix.sygixos.ui.settings.SettingsViewModel
import fr.sygix.sygixos.ui.settings.rememberSettingsViewModel

@Composable
fun HomeScreen(viewModel: HomeViewModel, glassBlur: Boolean = true) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val settingsViewModel = rememberSettingsViewModel()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAppArtwork provides viewModel.artwork,
        LocalAppIcons provides settingsViewModel.icons,
    ) {
        when (val s = state) {
            HomeState.Loading -> AmbientGradient(Modifier.fillMaxSize(), animated = true)
            is HomeState.Ready -> LauncherHome(
                catalog = s.catalog,
                hero = s.hero,
                onTogglePin = viewModel::togglePin,
                onOpenApp = { AppLauncher.open(context, it) },
                onOpenHero = { AppLauncher.open(context, it) },
                onAppFocused = viewModel::prepareShelf,
                onMoveInGrid = viewModel::moveInGrid,
                onRestoreOrder = viewModel::setGridOrder,
                onHideApp = viewModel::hideApp,
                settings = settingsState,
                counts = settingsViewModel.counts,
                onToggleSource = settingsViewModel::toggleSource,
                onUnhide = settingsViewModel::unhide,
                onUnhideAll = settingsViewModel::unhideAll,
                glassBlur = glassBlur,
            )
        }
    }
}

internal enum class Zone { HERO, DOCK, GRID }

@Composable
private fun SettingsGear(
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onFocusedChange: (Boolean) -> Unit = {},
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GearIcon(
        modifier
            .testTag("settings-gear")
            .semantics { contentDescription = "Réglages" }
            .tvFocus(focusRequester = focusRequester, enabled = focusEnabled, onFocused = onFocusedChange)
            .tvClickable(onClick = onOpen)
            .size(Dimens.GearSize),
    )
}

@Composable
private fun GearIcon(modifier: Modifier = Modifier, color: Color = Color.White) {
    Canvas(modifier) {
        drawPath(gearPath(size), color)
    }
}

private fun gearPath(size: Size): Path {
    val center = size.center
    val outer = size.minDimension / 2f
    val body = outer * 0.72f
    val hole = outer * 0.27f
    val toothWidth = outer * 0.36f
    val tooth = Path().apply {
        addRoundRect(
            RoundRect(
                Rect(Offset(center.x - toothWidth / 2f, center.y - outer), Size(toothWidth, outer - body * 0.55f)),
                CornerRadius(toothWidth * 0.42f),
            ),
        )
    }
    return Path().apply {
        addOval(Rect(center, body))
        for (i in 0 until GearTeeth) {
            addPath(
                Path().apply {
                    addPath(tooth)
                    transform(
                        Matrix().apply {
                            translate(center.x, center.y)
                            rotateZ(i * 360f / GearTeeth)
                            translate(-center.x, -center.y)
                        },
                    )
                },
            )
        }
        addOval(Rect(center, hole), Path.Direction.Clockwise)
    }
}

private const val GearTeeth = 8

@Composable
internal fun LauncherHome(
    catalog: Catalog,
    hero: HeroState,
    onTogglePin: (TvApp) -> Unit,
    onOpenApp: (TvApp) -> Unit = {},
    onOpenHero: (HeroItem) -> Unit = {},
    onAppFocused: (String) -> Unit = {},
    onMoveInGrid: (TvApp, Int) -> Unit = { _, _ -> },
    onRestoreOrder: (List<String>) -> Unit = {},
    onHideApp: (TvApp) -> Unit = {},
    settings: SettingsState? = null,
    counts: Flow<Map<String, Int>> = flowOf(emptyMap()),
    onToggleSource: (String) -> Unit = {},
    onUnhide: (String) -> Unit = {},
    onUnhideAll: () -> Unit = {},
    initialZone: Zone = Zone.HERO,
    glassBlur: Boolean = true,
) {
    var zone by rememberSaveable { mutableStateOf(initialZone) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var movingApp by remember { mutableStateOf<String?>(null) }
    var orderBeforeMove by remember { mutableStateOf<List<String>>(emptyList()) }
    val haze = rememberHazeState()
    var gridRow by remember { mutableIntStateOf(0) }
    var menuApp by remember { mutableStateOf<TvApp?>(null) }
    val heroFocus = remember { FocusRequester() }
    val dockFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val gearFocus = remember { FocusRequester() }
    var gearFocused by remember { mutableStateOf(false) }
    val dockAvailable = catalog.dock.isNotEmpty()
    val menuOpen = menuApp != null

    LaunchedEffect(dockAvailable) {
        if (zone == Zone.DOCK && !dockAvailable) zone = Zone.HERO
    }
    LaunchedEffect(zone, menuOpen, settingsOpen) {
        if (menuOpen || settingsOpen) return@LaunchedEffect
        val target = when (zone) {
            Zone.HERO -> heroFocus
            Zone.DOCK -> dockFocus
            Zone.GRID -> gridFocus
        }
        withFrameNanos { }
        if (!target.tryRequestFocus()) {
            withFrameNanos { }
            target.tryRequestFocus()
        }
    }

    val heroVisible = zone != Zone.GRID
    val gridAlpha by animateFloatAsState(if (heroVisible) 0f else 1f, tween(Motion.LAYER_FADE_MS, easing = AppleEasing), label = "gridAlpha")
    val heroAlpha by animateFloatAsState(if (heroVisible) 1f else 0f, tween(Motion.LAYER_FADE_MS, easing = AppleEasing), label = "heroAlpha")

    CompositionLocalProvider(LocalHazeState provides haze.takeIf { glassBlur }) {
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (settingsOpen) {
                    // SettingsScreen gère ses touches (Retour inclus) : on laisse tout passer.
                    return@onPreviewKeyEvent false
                }
                if (menuOpen) {
                    return@onPreviewKeyEvent if (e.key == Key.Back) { menuApp = null; true } else false
                }
                movingApp?.let { moving ->
                    val app = catalog.grid.firstOrNull { it.packageName == moving }
                    return@onPreviewKeyEvent when (e.key) {
                        Key.DirectionLeft -> { app?.let { onMoveInGrid(it, -1) }; true }
                        Key.DirectionRight -> { app?.let { onMoveInGrid(it, 1) }; true }
                        Key.DirectionUp -> { app?.let { onMoveInGrid(it, -Dimens.GridColumns) }; true }
                        Key.DirectionDown -> { app?.let { onMoveInGrid(it, Dimens.GridColumns) }; true }
                        Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { movingApp = null; true }
                        Key.Back -> { onRestoreOrder(orderBeforeMove); movingApp = null; true }
                        else -> false
                    }
                }
                when (e.key) {
                    Key.DirectionDown -> when {
                        zone == Zone.HERO && gearFocused -> { heroFocus.tryRequestFocus(); true }
                        zone == Zone.HERO -> { zone = if (dockAvailable) Zone.DOCK else Zone.GRID; true }
                        zone == Zone.DOCK -> { zone = Zone.GRID; true }
                        else -> false
                    }
                    Key.DirectionUp -> when {
                        zone == Zone.HERO && !gearFocused -> { gearFocus.tryRequestFocus(); true }
                        zone == Zone.HERO -> true
                        zone == Zone.DOCK -> { zone = Zone.HERO; true }
                        gridRow == 0 -> { zone = if (dockAvailable) Zone.DOCK else Zone.HERO; true }
                        else -> false
                    }
                    Key.Back -> { zone = Zone.HERO; true }
                    else -> false
                }
            },
    ) {
        HeroStage(
            items = hero.items,
            validatedVisuals = hero.validated,
            active = zone == Zone.HERO && !menuOpen && !settingsOpen,
            visible = heroVisible && !settingsOpen,
            focusRequester = heroFocus,
            claimFocus = !gearFocused,
            onOpen = onOpenHero,
            modifier = Modifier.testTag("zone-hero").alpha(heroAlpha).hazeSource(haze, zIndex = 0f),
        )
        AmbientGradient(Modifier.fillMaxSize().alpha(gridAlpha).hazeSource(haze, zIndex = 1f))
        HomeGrid(
            catalog = catalog,
            alpha = gridAlpha,
            focusEnabled = zone == Zone.GRID && !menuOpen,
            focusRequester = gridFocus,
            shelfPrograms = if (hero.fromApps) hero.items else emptyList(),
            validatedVisuals = hero.validated,
            checkedVisuals = hero.checked,
            onAppFocused = onAppFocused,
            onTileFocus = { gridRow = it },
            onTileClick = onOpenApp,
            onTileLongClick = { menuApp = it },
            movingApp = movingApp,
            modifier = Modifier.testTag("zone-grid").hazeSource(haze, zIndex = 2f),
        )
        if (movingApp != null) {
            GlassSurface(
                Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimens.DockBottomMargin)
                    .zIndex(5f),
            ) {
                Text(
                    "Flèches : déplacer  ·  OK : valider  ·  Retour : annuler",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        Dock(
            apps = catalog.dock,
            alpha = heroAlpha,
            focusEnabled = zone == Zone.DOCK && !menuOpen,
            focusRequester = dockFocus,
            onTileClick = onOpenApp,
            onTileLongClick = { menuApp = it },
            modifier = Modifier.testTag("zone-dock").align(Alignment.BottomCenter),
        )
        if (!settingsOpen) {
            AnimatedVisibility(
                visible = heroVisible,
                enter = fadeIn(tween(Motion.LAYER_FADE_MS, easing = AppleEasing)),
                exit = fadeOut(tween(Motion.LAYER_FADE_MS, easing = AppleEasing)),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 24.dp, end = 48.dp)
                    .zIndex(6f),
            ) {
                SettingsGear(
                    focusEnabled = zone == Zone.HERO && !menuOpen,
                    focusRequester = gearFocus,
                    onFocusedChange = { gearFocused = it },
                    onOpen = { settingsOpen = true },
                )
            }
        }
        if (settingsOpen) {
            // Comptages collectés uniquement tant que les réglages sont affichés.
            val countsState = counts.collectAsStateWithLifecycle(initialValue = emptyMap())
            SettingsScreen(
                state = settings ?: SettingsState(),
                counts = countsState,
                onToggleSource = onToggleSource,
                onUnhide = onUnhide,
                onUnhideAll = onUnhideAll,
                onBack = { settingsOpen = false },
            )
        }
        menuApp?.let { app ->
            AppContextMenu(
                app = app,
                pinned = catalog.dock.any { it.packageName == app.packageName },
                onTogglePin = { onTogglePin(app); menuApp = null },
                onHide = { onHideApp(app); menuApp = null },
                onDismiss = { menuApp = null },
                onMove = if (zone == Zone.GRID) {
                    {
                        orderBeforeMove = catalog.grid.map { it.packageName }
                        movingApp = app.packageName
                        menuApp = null
                    }
                } else {
                    null
                },
                modifier = Modifier.zIndex(10f),
            )
        }
    }
    }
}
