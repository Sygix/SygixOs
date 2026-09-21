package fr.sygix.sygixos.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.LocalHazeState
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.hero.AmbientGradient
import fr.sygix.sygixos.ui.hero.HeroStage

@Composable
fun HomeScreen(viewModel: HomeViewModel, glassBlur: Boolean = true) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    CompositionLocalProvider(LocalAppArtwork provides viewModel.artwork) {
        when (val s = state) {
            HomeState.Loading -> AmbientGradient(Modifier.fillMaxSize(), animated = true)
            is HomeState.Ready -> LauncherHome(
                catalog = s.catalog,
                hero = s.hero,
                onTogglePin = viewModel::togglePin,
                onOpenApp = { AppLauncher.open(context, it) },
                onOpenHero = { AppLauncher.open(context, it) },
                onMoveInGrid = viewModel::moveInGrid,
                onRestoreOrder = viewModel::setGridOrder,
                glassBlur = glassBlur,
            )
        }
    }
}

internal enum class Zone { HERO, DOCK, GRID }

/**
 * Trois paliers : héro (plein écran) → dock (overlay) → grille (masque le héro).
 * `zone` est l'unique source de vérité : seule la couche active est focusable,
 * les transitions haut/bas sont explicites, le focus est demandé après composition.
 */
@Composable
internal fun LauncherHome(
    catalog: Catalog,
    hero: HeroState,
    onTogglePin: (TvApp) -> Unit,
    onOpenApp: (TvApp) -> Unit = {},
    onOpenHero: (HeroItem) -> Unit = {},
    onMoveInGrid: (TvApp, Int) -> Unit = { _, _ -> },
    onRestoreOrder: (List<String>) -> Unit = {},
    initialZone: Zone = Zone.HERO,
    glassBlur: Boolean = true,
) {
    var zone by rememberSaveable { mutableStateOf(initialZone) }
    var movingApp by remember { mutableStateOf<String?>(null) }
    var orderBeforeMove by remember { mutableStateOf<List<String>>(emptyList()) }
    val haze = remember { HazeState() }
    var gridRow by remember { mutableIntStateOf(0) }
    var menuApp by remember { mutableStateOf<TvApp?>(null) }
    val heroFocus = remember { FocusRequester() }
    val dockFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val dockAvailable = catalog.dock.isNotEmpty()
    val menuOpen = menuApp != null

    LaunchedEffect(dockAvailable) {
        if (zone == Zone.DOCK && !dockAvailable) zone = Zone.HERO
    }
    LaunchedEffect(zone, menuOpen) {
        if (menuOpen) return@LaunchedEffect
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
                    Key.DirectionDown -> when (zone) {
                        Zone.HERO -> { zone = if (dockAvailable) Zone.DOCK else Zone.GRID; true }
                        Zone.DOCK -> { zone = Zone.GRID; true }
                        Zone.GRID -> false
                    }
                    Key.DirectionUp -> when (zone) {
                        Zone.HERO -> true
                        Zone.DOCK -> { zone = Zone.HERO; true }
                        Zone.GRID -> if (gridRow == 0) { zone = if (dockAvailable) Zone.DOCK else Zone.HERO; true } else false
                    }
                    Key.Back -> { zone = Zone.HERO; true }
                    else -> false
                }
            },
    ) {
        HeroStage(
            items = hero.items,
            validatedVisuals = hero.validated,
            active = zone == Zone.HERO && !menuOpen,
            visible = heroVisible,
            focusRequester = heroFocus,
            onOpen = onOpenHero,
            modifier = Modifier.alpha(heroAlpha).hazeSource(haze, zIndex = 0f),
        )
        AmbientGradient(Modifier.fillMaxSize().alpha(gridAlpha).hazeSource(haze, zIndex = 1f))
        HomeGrid(
            catalog = catalog,
            alpha = gridAlpha,
            focusEnabled = zone == Zone.GRID && !menuOpen,
            focusRequester = gridFocus,
            shelfPrograms = if (hero.fromApps) hero.items else emptyList(),
            validatedVisuals = hero.validated,
            onTileFocus = { gridRow = it },
            onTileClick = onOpenApp,
            onTileLongClick = { menuApp = it },
            movingApp = movingApp,
            modifier = Modifier.hazeSource(haze, zIndex = 2f),
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
            modifier = Modifier.align(Alignment.BottomCenter),
        )
        menuApp?.let { app ->
            AppContextMenu(
                app = app,
                pinned = catalog.dock.any { it.packageName == app.packageName },
                onTogglePin = { onTogglePin(app); menuApp = null },
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
