/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.SygixTypography
import fr.sygix.sygixos.domain.UpNextState
import fr.sygix.sygixos.model.UpNextItem
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.domain.FocusFallback
import fr.sygix.sygixos.domain.GridScroll
import fr.sygix.sygixos.domain.GridSections
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.domain.PixelSize
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import coil.imageLoader
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
internal fun HomeGrid(
    catalog: Catalog,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    shelfPrograms: () -> List<HeroItem>,
    validatedVisuals: () -> Set<String>,
    checkedVisuals: () -> Set<String>,
    onAppFocused: (String) -> Unit,
    onExitUp: () -> Unit,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    origin: Float,
    viewport: Float,
    anchor: () -> GridScroll.Anchor?,
    onAnchor: (GridScroll.Anchor) -> Unit,
    scroll: () -> Int,
    modifier: Modifier = Modifier,
    movingApp: String? = null,
    upNext: UpNextState? = null,
    onOpenUpNext: (UpNextItem) -> Unit = {},
    onUpNextMenu: (UpNextItem) -> Unit = {},
    onRetryUpNext: () -> Unit = {},
    onUpNextReturnHandled: (Long) -> Unit = {},
    trackingStatus: @Composable () -> Unit = {},
) {
    val rows = remember(catalog.grid) { catalog.grid.chunked(Dimens.GridColumns) }
    val appPackages = remember(catalog.grid) { catalog.grid.map { it.packageName } }
    val sections = remember(upNext, appPackages) { GridSections.of(upNext, appPackages) }
    val firstAppRow = sections.firstAppRow
    val upNextRow = upNext?.takeIf { sections.upNextShown }
    val upNextScroll = rememberLazyListState()
    var titleHeight by remember { mutableStateOf(0f) }
    val focus = remember { TileFocus() }
    val returnFocus = upNext?.returnFocus
    val returnIndex = returnFocus?.let { sections.upNextKeys.indexOf(it.key) } ?: -1
    val returnHandled by rememberUpdatedState(onUpNextReturnHandled)
    LaunchedEffect(returnFocus) {
        val target = returnFocus ?: return@LaunchedEffect
        if (returnIndex >= 0) {
            focus.onFocused(target.key, sections.cardOrderIndex(returnIndex), FocusFallback.CardVisit(returnIndex, afterApps = !sections.upNextFirst))
        }
        returnHandled(target.id)
    }
    val enabled by rememberUpdatedState(focusEnabled)
    val focusedApp = focus.focusedApp
    val entryApp = returnFocus?.key?.takeIf { returnIndex >= 0 } ?: focus.gridEntry(sections)
    LaunchedEffect(sections.upNextKeys) { focus.track(sections) }
    var shelf by remember { mutableStateOf<Shelf?>(null) }

    val programs by rememberUpdatedState(shelfPrograms)
    val visuals by rememberUpdatedState(validatedVisuals)
    val settled by rememberUpdatedState(checkedVisuals)
    fun postersOf(packageName: String?): List<String> =
        packageName?.let { ShelfPosters.forPackage(programs(), visuals(), it) }.orEmpty()

    fun pendingFor(packageName: String?): Boolean = packageName
        ?.let { ShelfPosters.candidates(programs(), it, VisualQuality.SHELF_VALIDATED_PER_APP) }
        ?.any { it !in settled() } ?: false

    val context = LocalContext.current
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth = screenWidth - Dimens.ScreenMarginH * 2
    val rowHeight = (contentWidth - Dimens.GridSpacing * (Dimens.GridColumns - 1)) / Dimens.GridColumns * 9f / 16f
    val upWidth = (contentWidth - Dimens.GridSpacing * 3) / 4
    val upHeight = upWidth * 9f / 16f
    val panelBlock = contentWidth / Dimens.ShelfAspectRatio + Dimens.GridRowSpacing
    val posterSize = remember(density, screenWidth) {
        with(density) {
            val zoomed = contentWidth * ShelfPosterZoom
            ImageBounds.screen(zoomed.roundToPx(), (zoomed / Dimens.ShelfAspectRatio).roundToPx())
        }
    }
    val sectionRows = with(density) { sections.rows(rows.size, upHeight.toPx(), rowHeight.toPx(), titleHeight) }
    val geometry = remember(density, screenWidth, sectionRows) {
        with(density) {
            GridScroll(
                topMargin = Dimens.GridTopMargin.toPx(),
                rowHeight = rowHeight.toPx(),
                rowSpacing = Dimens.GridRowSpacing.toPx(),
                panelHeight = panelBlock.toPx(),
                margin = Dimens.GridRowSpacing.toPx(),
                rows = sectionRows,
            )
        }
    }
    LaunchedEffect(focusedApp, movingApp, appPackages) {
        val target = focus.focusedApp
        if (movingApp != null || target == null || target !in appPackages) {
            shelf = null
            return@LaunchedEffect
        }
        if (postersOf(target).isEmpty() && !pendingFor(target)) shelf = null
        if (shelf == null) {
            launch {
                val first = snapshotFlow { postersOf(target).firstOrNull() }.filterNotNull().first()
                context.imageLoader.execute(posterRequest(context, first, posterSize, programs().firstOrNull { it.imageUrl == first }?.artwork))
            }
            delay(Motion.SHELF_OPEN_DELAY_MS)
        }
        snapshotFlow { postersOf(target) to pendingFor(target) }.collect { (posters, pending) ->
            when {
                posters.isEmpty() -> shelf = null
                shelf?.app != target || !pending -> shelf = Shelf(target, posters)
                else -> shelf = shelf?.let { it.copy(uris = it.uris.intersect(posters.toSet()).toList()) }?.takeIf { it.uris.isNotEmpty() }
            }
        }
    }
    val openRow = remember(rows, shelf?.app) { rows.indexOfFirst { row -> row.any { it.packageName == shelf?.app } } }
    val sectionOpenRow = if (openRow < 0) -1 else openRow + firstAppRow
    val window by remember(geometry, sectionOpenRow, viewport, origin) {
        derivedStateOf { geometry.window(scroll().toFloat(), anchor(), sectionOpenRow, viewport, origin) }
    }
    suspend fun reveal(key: String): Boolean = focus.reveal(key) {
        val row = sections.rowOf(key, Dimens.GridColumns)
        if (row >= 0 && viewport > 0f) onAnchor(geometry.next(anchor(), row, sectionOpenRow, viewport, origin))
        withFrameNanos { }
    }
    LaunchedEffect(openRow, focusedApp, origin, viewport, geometry) {
        val row = sections.rowOf(focus.focusedApp, Dimens.GridColumns)
        if (row < 0 || viewport <= 0f) return@LaunchedEffect
        onAnchor(geometry.next(anchor(), row, sectionOpenRow, viewport, origin))
    }
    LaunchedEffect(sections.focusOrder, focusEnabled) {
        if (focusEnabled && entryApp != null && entryApp !in sections.upNextKeys) reveal(entryApp)
    }
    LaunchedEffect(movingApp, rows) {
        if (movingApp != null) reveal(movingApp)
    }
    LaunchedEffect(focusEnabled) {
        if (!focusEnabled) shelf = null
    }
    val appFocused by rememberUpdatedState(onAppFocused)
    LaunchedEffect(focusedApp, focusEnabled) {
        val target = focusedApp ?: return@LaunchedEffect
        if (!focusEnabled) return@LaunchedEffect
        if (target !in appPackages) return@LaunchedEffect
        delay(Motion.SHELF_PREPARE_DELAY_MS)
        appFocused(target)
    }

    val exitUp by rememberUpdatedState(onExitUp)
    val scope = rememberCoroutineScope()
    suspend fun focusCard(key: String) {
        if (focus.focus(key)) return
        val index = sections.upNextKeys.indexOf(key)
        if (index >= 0) focus.reveal(key) { upNextScroll.scrollToItem(index); withFrameNanos { } }
    }
    Column(
        modifier.fillMaxWidth().padding(vertical = Dimens.GridTopMargin).focusGroup()
            .onPreviewKeyEvent { event ->
                if (event.key != Key.DirectionUp) return@onPreviewKeyEvent false
                when (val up = sections.upFrom(focus.focusedApp, Dimens.GridColumns, focus.lastCard)) {
                    GridSections.Up.Default -> false
                    GridSections.Up.Exit -> { if (event.type == KeyEventType.KeyDown) exitUp(); true }
                    is GridSections.Up.Card -> { if (event.type == KeyEventType.KeyDown) scope.launch { focusCard(up.key) }; true }
                }
            },
        verticalArrangement = Arrangement.spacedBy(Dimens.GridRowSpacing),
    ) {
        @Composable fun UpNextSection() {
            if (upNextRow == null) return
            Column {
                Box(Modifier.fillMaxWidth().padding(end = Dimens.ScreenMarginH)) {
                    SectionTitle(R.string.upnext_title, "section-title-upnext", padded = true, onHeight = { titleHeight = it })
                    Box(Modifier.align(Alignment.CenterEnd).padding(bottom = Dimens.SectionTitleGap)) { trackingStatus() }
                }
                UpNextRow(upNextRow.content, upWidth, focus, entryApp, focusRequester, focusEnabled,
                    onFocused = { itemKey, index ->
                        if (enabled) {
                            focus.onFocused(itemKey, sections.cardOrderIndex(index), FocusFallback.CardVisit(index, afterApps = !sections.upNextFirst))
                        }
                    },
                    onOpen = onOpenUpNext, onMenu = onUpNextMenu, onRetry = onRetryUpNext, state = upNextScroll)
            }
        }
        if (sections.upNextFirst) UpNextSection()
        rows.forEachIndexed { rowIndex, rowApps ->
            if (rowIndex + firstAppRow !in window) {
                Spacer(Modifier.height(rowHeight + if (rowIndex == 0) with(density) { titleHeight.toDp() } else 0.dp))
                return@forEachIndexed
            }
            Column(Modifier.padding(horizontal = Dimens.ScreenMarginH)) {
                if (rowIndex == 0) SectionTitle(R.string.applications_title, "section-title-apps", onHeight = { titleHeight = it })
                GridRow(
                    rowIndex = rowIndex,
                    apps = rowApps,
                    focus = focus,
                    focusEnabled = focusEnabled,
                    focusRequester = focusRequester,
                    entryApp = entryApp?.takeIf { entry -> rowApps.any { it.packageName == entry } },
                    movingApp = movingApp?.takeIf { moving -> rowApps.any { it.packageName == moving } },
                    shelfUris = if (rowIndex == openRow) shelf?.uris.orEmpty() else emptyList(),
                    posterSize = posterSize,
                    shelfSources = programs().mapNotNull { item -> item.imageUrl?.let { it to item.artwork } }.toMap(),
                    onTileClick = onTileClick,
                    onTileLongClick = onTileLongClick,
                    indexOffset = sections.appIndexOffset,
                    isFocusEnabled = { enabled },
                )
            }
        }
        if (!sections.upNextFirst) UpNextSection()
        if (catalog.grid.isEmpty()) {
            val height = with(density) { sections.emptyAppsHeight(viewport, Dimens.GridTopMargin.toPx(), upHeight.toPx(), titleHeight, Dimens.GridRowSpacing.toPx()).toDp() }
            Box(Modifier.fillMaxWidth().height(height).testTag("no-tv-apps"), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.no_tv_apps), style = MaterialTheme.typography.titleMedium, color = SygixColors.OnDarkSecondary)
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes resource: Int, tag: String, padded: Boolean = false, onHeight: (Float) -> Unit) {
    Text(stringResource(resource), style = SygixTypography.titleLarge, color = SygixColors.OnDarkSecondary,
        modifier = Modifier.onSizeChanged { onHeight(it.height.toFloat()) }.testTag(tag)
            .padding(start = if (padded) Dimens.ScreenMarginH else 0.dp, bottom = Dimens.SectionTitleGap))
}

private data class Shelf(val app: String, val uris: List<String>)

@Composable
private fun GridRow(
    rowIndex: Int,
    apps: List<TvApp>,
    focus: TileFocus,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    entryApp: String?,
    movingApp: String?,
    shelfUris: List<String>,
    posterSize: PixelSize,
    shelfSources: Map<String, fr.sygix.sygixos.model.ProgramArtwork>,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    indexOffset: Int = 0,
    isFocusEnabled: () -> Boolean = { focusEnabled },
) {
    Column {
        val shown = remember { MutableTransitionState(false) }
        shown.targetState = shelfUris.isNotEmpty()
        if (shown.currentState || shown.targetState) AnimatedVisibility(
            visibleState = shown,
            enter = expandVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                fadeIn(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
            exit = shrinkVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                fadeOut(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
        ) {
            ShelfPanel(shelfUris, posterSize, Modifier.padding(bottom = Dimens.GridRowSpacing), shelfSources)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
            modifier = Modifier.fillMaxWidth(),
        ) {
            apps.forEachIndexed { column, app ->
                key(app.packageName) {
                    AppTile(
                        app = app,
                        focusEnabled = focusEnabled,
                        onClick = { onTileClick(app) },
                        onLongClick = { onTileLongClick(app) },
                        onFocusChanged = { focused ->
                            if (focused && isFocusEnabled()) {
                                focus.onFocused(app.packageName, indexOffset + rowIndex * Dimens.GridColumns + column)
                            }
                        },
                        focusRequester = focus.bind(app.packageName, focusRequester.takeIf { app.packageName == entryApp }),
                        lifted = app.packageName == movingApp,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
            repeat(Dimens.GridColumns - apps.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
