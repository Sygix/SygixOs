/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import fr.sygix.sygixos.core.designsystem.GlassLook
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.tvFocusable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
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
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.emptyFlow
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import dev.chrisbanes.haze.rememberHazeState
import dev.chrisbanes.haze.hazeSource
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Badge
import fr.sygix.sygixos.core.designsystem.BadgePlacement
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.GlassBackdrop
import fr.sygix.sygixos.core.designsystem.LocalGlassBackdrop
import fr.sygix.sygixos.core.designsystem.LocalHazeState
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.domain.AppCatalog
import fr.sygix.sygixos.domain.GridScroll
import fr.sygix.sygixos.domain.GridSections
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.domain.HomePage
import fr.sygix.sygixos.domain.StartupPhase
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.model.UpNextSourceEntry
import fr.sygix.sygixos.ui.hero.AmbientGradient
import fr.sygix.sygixos.ui.hero.HeroStage
import fr.sygix.sygixos.ui.settings.HomeScreenActions
import fr.sygix.sygixos.ui.settings.LocalAppIcons
import fr.sygix.sygixos.ui.settings.SettingsCategory
import fr.sygix.sygixos.ui.settings.SettingsScreen
import fr.sygix.sygixos.ui.settings.SettingsState
import fr.sygix.sygixos.ui.settings.SettingsViewModel
import fr.sygix.sygixos.ui.settings.UpdateActions
import fr.sygix.sygixos.ui.settings.rememberSettingsViewModel
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import fr.sygix.sygixos.ui.settings.LauncherSystemControls

@Composable
fun HomeScreen(viewModel: HomeViewModel, glassBlur: Boolean = true, launcher: LauncherSystemViewModel? = null) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val startup by viewModel.startup.collectAsStateWithLifecycle()
    val launcherState = launcher?.state?.collectAsStateWithLifecycle()?.value ?: LauncherSystemState()
    LaunchedEffect(startup) { if (startup == StartupPhase.Done) launcher?.onStartupFinished() }
    val context = LocalContext.current
    val settingsViewModel = rememberSettingsViewModel()
    val settingsState by settingsViewModel.state.collectAsStateWithLifecycle()
    CompositionLocalProvider(
        LocalAppArtwork provides viewModel.artwork,
        LocalAppIcons provides settingsViewModel.icons,
    ) {
        StartupHost(
            state = state,
            phase = startup,
            animated = viewModel.startupAnimated,
            mascot = viewModel.mascot,
            onSplashShown = viewModel::onSplashShown,
            onFadeFinished = viewModel::onSplashFadeFinished,
            onMascotShown = viewModel::onMascotShown,
            onMascotUnavailable = viewModel::onMascotUnavailable,
            windowFocused = LocalWindowInfo.current.isWindowFocused,
        ) { s, interactive, homeHandlesBack ->
            LaunchedEffect(Unit) { viewModel.onHomeShown() }
            LauncherHome(
                catalog = s.catalog,
                hero = s.hero,
                upNext = s.upNext,
                onOpenUpNext = { AppLauncher.open(context, it) },
                onOpenUpNextSource = { item, source -> AppLauncher.openWith(context, item, source) },
                onRetryUpNext = viewModel::refreshUpNext,
                upNextUpdates = viewModel.upNextUpdates,
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
                onToggleHidden = settingsViewModel::toggleHidden,
                onUnhideAll = settingsViewModel::unhideAll,
                onSettingsCategory = settingsViewModel::enterCategory,
                glassBlur = glassBlur,
                clock = viewModel.clock,
                updateBadge = viewModel.updateBadge,
                update = settingsViewModel.updateActions,
                homeScreenActions = settingsViewModel.homeScreenActions,
                interactive = interactive,
                homeHandlesBack = homeHandlesBack,
                onHeroVisualReady = viewModel::onHeroVisualReady,
                gridReady = startup == StartupPhase.Done,
                launcherState = launcherState,
                launcherActions = launcher?.actions ?: LauncherSystemActions(),
            )
        }
    }
}

internal enum class Zone { HERO, DOCK, GRID }

@Composable
private fun HeroCapsule(
    clock: StateFlow<String>,
    updateBadge: StateFlow<Boolean>,
    glassActive: Boolean,
    gearFocusEnabled: Boolean,
    gearFocus: FocusRequester,
    onGearFocused: (Boolean) -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier.testTag("hero-capsule"),
        shape = CapsuleShape,
        active = glassActive,
        look = GlassLook.Capsule,
    ) {
        Row(
            Modifier
                .height(Dimens.CapsuleHeight)
                .padding(
                start = Dimens.CapsulePaddingStart,
                top = Dimens.CapsulePadding,
                end = Dimens.CapsulePadding,
                bottom = Dimens.CapsulePadding,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.CapsuleSpacing),
        ) {
            ClockText(clock)
            SettingsGear(
                updateBadge = updateBadge,
                focusEnabled = gearFocusEnabled,
                focusRequester = gearFocus,
                onFocusedChange = onGearFocused,
                onOpen = onOpenSettings,
            )
        }
    }
}

@Composable
private fun ClockText(clock: StateFlow<String>) {
    val time by clock.collectAsStateWithLifecycle()
    Text(time, style = TextStyles.Clock, color = SygixColors.OnDark, maxLines = 1, modifier = Modifier.testTag("hero-clock"))
}

@Composable
private fun SettingsGear(
    updateBadge: StateFlow<Boolean>,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onFocusedChange: (Boolean) -> Unit,
    onOpen: () -> Unit,
) {
    val label = stringResource(R.string.settings_title)
    val badge by updateBadge.collectAsStateWithLifecycle()
    var focused by remember { mutableStateOf(false) }
    val progress = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "gearFocus",
    )
    Box(
        Modifier
            .testTag("settings-gear")
            .semantics { contentDescription = label }
            .tvFocusable(
                onFocused = {
                    focused = it
                    onFocusedChange(it)
                },
                focusRequester = focusRequester,
                enabled = focusEnabled,
            )
            .tvClickable(onClick = onOpen)
            .graphicsLayer {
                val p = progress.value
                val scale = 1f + (Dimens.GearFocusScale - 1f) * p
                scaleX = scale
                scaleY = scale
                shadowElevation = Dimens.GearFocusElevation.toPx() * p
                shape = CircleShape
                clip = false
            }
            .drawBehind { drawCircle(lerp(SygixColors.GearRest, SygixColors.OnDark, progress.value)) }
            .size(Dimens.GearButton),
        contentAlignment = Alignment.Center,
    ) {
        GearIcon(Modifier.size(Dimens.GearIcon)) { lerp(SygixColors.OnDark, SygixColors.OnPill, progress.value) }
        if (badge) Badge(BadgePlacement.CORNER, tag = "update-badge-gear")
    }
}

@Composable
private fun GearIcon(modifier: Modifier = Modifier, color: () -> Color) {
    Spacer(
        modifier.drawWithCache {
            val stroke = Stroke(
                width = size.minDimension * GearStroke,
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            )
            val outline = gearOutline(size, stroke.width)
            val hub = size.minDimension / 2f * GearHub
            onDrawBehind {
                val tint = color()
                drawPath(outline, tint, style = stroke)
                drawCircle(tint, radius = hub, style = stroke)
            }
        },
    )
}

private fun gearOutline(size: Size, strokeWidth: Float): Path {
    val center = size.center
    val outer = size.minDimension / 2f - strokeWidth / 2f
    val body = outer * 0.74f
    val toothWidth = outer * 0.38f
    var outline = Path().apply { addOval(Rect(center, body)) }
    for (i in 0 until GearTeeth) {
        val tooth = Path().apply {
            addRoundRect(
                RoundRect(
                    Rect(Offset(center.x - toothWidth / 2f, center.y - outer), Size(toothWidth, outer - body * 0.6f)),
                    CornerRadius(toothWidth * 0.3f),
                ),
            )
            transform(
                Matrix().apply {
                    translate(center.x, center.y)
                    rotateZ(i * 360f / GearTeeth)
                    translate(-center.x, -center.y)
                },
            )
        }
        outline = Path.combine(PathOperation.Union, outline, tooth)
    }
    return outline
}

private const val GearStroke = 1.8f / 24f
private const val GearHub = 0.25f
private const val GearTeeth = 8

private val CapsuleShape = RoundedCornerShape(percent = 50)

private val NoAutoScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}

private val NoCounts: StateFlow<Map<String, Int>?> = MutableStateFlow(emptyMap())

private val NoClock: StateFlow<String> = MutableStateFlow("")

private val NoBadge: StateFlow<Boolean> = MutableStateFlow(false)

@OptIn(ExperimentalFoundationApi::class)
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
    counts: StateFlow<Map<String, Int>?> = NoCounts,
    onToggleSource: (String) -> Unit = {},
    onToggleHidden: (String) -> Unit = {},
    onUnhideAll: () -> Unit = {},
    onSettingsCategory: (SettingsCategory) -> Unit = {},
    initialZone: Zone = Zone.HERO,
    glassBlur: Boolean = true,
    clock: StateFlow<String> = NoClock,
    updateBadge: StateFlow<Boolean> = NoBadge,
    update: UpdateActions = UpdateActions(),
    homeScreenActions: HomeScreenActions = HomeScreenActions(),
    interactive: Boolean = true,
    homeHandlesBack: Boolean = true,
    onHeroVisualReady: () -> Unit = {},
    gridReady: Boolean = true,
    upNext: UpNextState? = null,
    onOpenUpNext: (UpNextItem) -> Unit = {},
    onOpenUpNextSource: (UpNextItem, UpNextSourceEntry) -> Unit = { _, _ -> },
    onRetryUpNext: () -> Unit = {},
    upNextUpdates: Flow<Unit> = emptyFlow(),
    launcherState: LauncherSystemState = LauncherSystemState(),
    launcherActions: LauncherSystemActions = LauncherSystemActions(),
) {
    var zone by rememberSaveable { mutableStateOf(initialZone) }
    var settingsOpen by rememberSaveable { mutableStateOf(false) }
    var movingApp by remember { mutableStateOf<String?>(null) }
    var orderBeforeMove by remember { mutableStateOf<List<String>>(emptyList()) }
    val haze = rememberHazeState()
    val backdrop = remember { GlassBackdrop() }
    var menuApp by remember { mutableStateOf<TvApp?>(null) }
    var menuItem by remember { mutableStateOf<UpNextItem?>(null) }
    val heroFocus = remember { FocusRequester() }
    val dockFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }
    val gearFocus = remember { FocusRequester() }
    var gearFocused by remember { mutableStateOf(false) }
    val dockAvailable = catalog.dock.isNotEmpty()
    val gridAvailable = GridSections.hasFocusTarget(catalog.grid.size, upNext)
    val menuOpen = menuApp != null || menuItem != null
    val lifecycle = LocalLifecycleOwner.current.lifecycle

    LaunchedEffect(upNextUpdates, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { upNextUpdates.collect() }
    }
    val launcherOverlay = launcherState.overlayShown
    var handledHome by remember { mutableStateOf(launcherState.homeRequest) }
    LaunchedEffect(launcherState.homeRequest) {
        if (handledHome == launcherState.homeRequest) return@LaunchedEffect
        handledHome = launcherState.homeRequest
        settingsOpen = false
        menuApp = null
        menuItem = null
        if (movingApp != null) onRestoreOrder(orderBeforeMove)
        movingApp = null
        gearFocused = false
        zone = Zone.HERO
        withFrameNanos { }
        heroFocus.tryRequestFocus()
    }

    LaunchedEffect(dockAvailable) {
        if (zone == Zone.DOCK && !dockAvailable) zone = Zone.HERO
    }
    LaunchedEffect(gridAvailable) {
        if (zone == Zone.GRID && !gridAvailable) zone = Zone.HERO
    }
    LaunchedEffect(zone, menuOpen, settingsOpen, interactive, launcherOverlay) {
        if (!interactive || menuOpen || settingsOpen || launcherOverlay) return@LaunchedEffect
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
    val heroNow = rememberUpdatedState(hero)
    var hiddenHeroVisuals by remember { mutableStateOf(hero.validated) }
    SideEffect { if (heroVisible) hiddenHeroVisuals = hero.validated }
    val heroVisuals = if (heroVisible) hero.validated else hiddenHeroVisuals
    val pageScroll = rememberScrollState()
    var gridAnchor by remember { mutableStateOf<GridScroll.Anchor?>(null) }
    val density = LocalDensity.current
    val countsState = counts.collectAsStateWithLifecycle()

    CompositionLocalProvider(LocalHazeState provides haze.takeIf { glassBlur }) {
    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .onPreviewKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                if (launcherOverlay) return@onPreviewKeyEvent false
                if (!homeHandlesBack && e.key == Key.Back) return@onPreviewKeyEvent false
                if (settingsOpen) return@onPreviewKeyEvent false
                if (menuOpen) {
                    return@onPreviewKeyEvent if (e.key == Key.Back) { menuApp = null; menuItem = null; true } else false
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
                        else -> false
                    }
                    Key.Back -> {
                        if (zone == Zone.HERO && gearFocused) heroFocus.tryRequestFocus() else zone = Zone.HERO
                        true
                    }
                    else -> false
                }
            },
    ) {
        val viewportHeight = maxHeight
        val viewport = with(density) { viewportHeight.toPx() }
        val heroOnScreen by remember(viewport) { derivedStateOf { HomePage.heroOnScreen(pageScroll.value, viewport) } }
        LaunchedEffect(viewport) {
            var previous: Boolean? = null
            snapshotFlow {
                val gridActive = zone == Zone.GRID
                gridActive to HomePage.target(gridActive, gridAnchor, viewport)
            }.collectLatest { (gridActive, target) ->
                val transition = HomePage.transition(previous, gridActive)
                previous = gridActive
                val position = target.roundToInt()
                when (transition) {
                    HomePage.Transition.SNAP -> pageScroll.scrollTo(position)
                    HomePage.Transition.ZONE -> pageScroll.animateScrollTo(position, tween(Motion.PAGE_SCROLL_MS, easing = AppleEasing))
                    HomePage.Transition.SAME_ZONE -> pageScroll.animateScrollTo(position, tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing))
                }
            }
        }
        CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
            Column(Modifier.fillMaxSize().verticalScroll(pageScroll, enabled = false)) {
                Box(Modifier.fillMaxWidth().height(viewportHeight)) {
                    HeroStage(
                        items = hero.items,
                        validatedVisuals = heroVisuals,
                        active = interactive && zone == Zone.HERO && !menuOpen && !settingsOpen && !launcherOverlay,
                        stillActive = { zone == Zone.HERO && !menuOpen && !settingsOpen && !launcherOverlay },
                        visible = heroVisible && !settingsOpen,
                        focusRequester = heroFocus,
                        claimFocus = !gearFocused && !launcherOverlay,
                        onOpen = onOpenHero,
                        onVisualReady = onHeroVisualReady,
                        motion = interactive,
                        backdrop = backdrop,
                        modifier = Modifier.testTag("zone-hero").hazeSource(haze, zIndex = 0f),
                    )
                    CompositionLocalProvider(LocalGlassBackdrop provides backdrop.takeIf { glassBlur }) {
                        Dock(
                            apps = catalog.dock,
                            active = heroOnScreen,
                            focusEnabled = interactive && zone == Zone.DOCK && !menuOpen && !launcherOverlay,
                            focusRequester = dockFocus,
                            onTileClick = onOpenApp,
                            onTileLongClick = { menuApp = it },
                            modifier = Modifier.testTag("zone-dock").align(Alignment.BottomCenter),
                        )
                        if (!settingsOpen) {
                            HeroCapsule(
                                clock = clock,
                                updateBadge = updateBadge,
                                glassActive = heroOnScreen,
                                gearFocusEnabled = interactive && zone == Zone.HERO && !menuOpen && !launcherOverlay,
                                gearFocus = gearFocus,
                                onGearFocused = { gearFocused = it },
                                onOpenSettings = {
                                    onSettingsCategory(SettingsCategory.Initial)
                                    settingsOpen = true
                                },
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = Dimens.CapsuleTop, end = Dimens.CapsuleEnd),
                            )
                        }
                    }
                }
                Box(
                    Modifier
                        .testTag("zone-grid")
                        .fillMaxWidth()
                        .heightIn(min = viewportHeight)
                        .hazeSource(haze, zIndex = 1f),
                ) {
                    AmbientGradient(Modifier.matchParentSize())
                    if (gridReady) HomeGrid(
                        catalog = catalog,
                        focusEnabled = interactive && zone == Zone.GRID && !menuOpen && !launcherOverlay,
                        focusRequester = gridFocus,
                        shelfPrograms = { heroNow.value.let { if (it.fromApps) it.items else emptyList() } },
                        validatedVisuals = { heroNow.value.validated },
                        checkedVisuals = { heroNow.value.checked },
                        onAppFocused = onAppFocused,
                        onExitUp = { zone = if (dockAvailable) Zone.DOCK else Zone.HERO },
                        onTileClick = onOpenApp,
                        onTileLongClick = { menuApp = it },
                        origin = viewport,
                        viewport = viewport,
                        anchor = { gridAnchor },
                        onAnchor = { gridAnchor = it },
                        movingApp = movingApp,
                        upNext = upNext,
                        onOpenUpNext = onOpenUpNext,
                        onUpNextMenu = { menuItem = it },
                        onRetryUpNext = onRetryUpNext,
                        modifier = Modifier.testTag("home-grid"),
                    )
                }
            }
        }
        if (movingApp != null) {
            GlassSurface(
                Modifier
                    .testTag("move-banner")
                    .align(Alignment.BottomCenter)
                    .padding(bottom = Dimens.DockBottomMargin)
                    .zIndex(5f),
                shape = CapsuleShape,
            ) {
                Text(
                    "Flèches : déplacer  ·  OK : valider  ·  Retour : annuler",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                )
            }
        }
        if (settingsOpen) {
            SettingsScreen(
                state = settings ?: SettingsState(),
                counts = countsState,
                onToggleSource = onToggleSource,
                onToggleHidden = onToggleHidden,
                onUnhideAll = onUnhideAll,
                onCategoryEntered = onSettingsCategory,
                update = update,
                homeScreenActions = homeScreenActions,
                onBack = { settingsOpen = false },
                homeHandlesBack = homeHandlesBack,
                homeScreenRows = { enabled -> LauncherSystemControls(launcherState, launcherActions, enabled && !launcherOverlay) },
            )
        }
        menuItem?.let { item ->
            UpNextContextMenu(item, onOpen = { source -> onOpenUpNextSource(item, source); menuItem = null }, modifier = Modifier.zIndex(10f))
        }
        menuApp?.let { app ->
            AppContextMenu(
                app = app,
                pinState = AppCatalog.pinState(catalog.dock, app.packageName),
                onTogglePin = { onTogglePin(app); menuApp = null },
                onHide = { onHideApp(app); menuApp = null },
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
        if (launcherState.showOnboarding) LauncherOnboarding(launcherState, launcherActions)
        if (launcherState.showOverlayConfirmation) OverlayPermissionConfirmation(launcherActions)
    }
    }
}
