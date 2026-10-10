/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.structuralEqualityPolicy
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
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalConfiguration
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
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
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
import kotlinx.coroutines.delay
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
    modifier: Modifier = Modifier,
    initialShelfApp: String? = null,
    movingApp: String? = null,
    upNext: UpNextState? = null,
    onOpenUpNext: (UpNextItem) -> Unit = {},
    onUpNextMenu: (UpNextItem) -> Unit = {},
    onRetryUpNext: () -> Unit = {},
) {
    val rows = remember(catalog.grid) { catalog.grid.chunked(Dimens.GridColumns) }
    val appPackages = remember(catalog.grid) { catalog.grid.map { it.packageName } }
    val sections = remember(upNext, appPackages) { GridSections.of(upNext, appPackages) }
    val firstAppRow = sections.firstAppRow
    val upNextRow = upNext?.takeIf { sections.upNextShown }
    val upNextScroll = rememberLazyListState()
    var titleHeight by remember { mutableStateOf(0f) }
    val focus = remember { TileFocus(initialShelfApp) }
    val enabled by rememberUpdatedState(focusEnabled)
    val focusedApp = focus.focusedApp
    val entryApp = focus.gridEntry(sections)
    LaunchedEffect(sections.upNextKeys) { focus.track(sections) }
    LaunchedEffect(sections.focusOrder, focusEnabled) {
        if (focusEnabled && entryApp != null && entryApp !in sections.upNextKeys) {
            withFrameNanos { }
            focus.requesterFor(entryApp).tryRequestFocus()
        }
    }
    var openApp by remember { mutableStateOf<String?>(null) }

    val programs by rememberUpdatedState(shelfPrograms)
    val visuals by rememberUpdatedState(validatedVisuals)
    val settled by rememberUpdatedState(checkedVisuals)
    fun postersOf(packageName: String?): List<String> =
        packageName?.let { ShelfPosters.forPackage(programs(), visuals(), it) }.orEmpty()

    fun pendingFor(packageName: String?): Boolean = packageName
        ?.let { ShelfPosters.candidates(programs(), it, VisualQuality.SHELF_VALIDATED_PER_APP) }
        ?.any { it !in settled() } ?: false

    LaunchedEffect(focusedApp, movingApp, appPackages) {
        val target = focus.focusedApp
        if (movingApp != null || target !in appPackages) {
            openApp = null
            return@LaunchedEffect
        }
        if (postersOf(target).isEmpty() && !pendingFor(target)) openApp = null
        if (openApp == null) delay(Motion.SHELF_OPEN_DELAY_MS)
        snapshotFlow { postersOf(target) to pendingFor(target) }.collect { (posters, pending) ->
            when {
                posters.isNotEmpty() -> openApp = target
                !pending -> openApp = null
                else -> Unit
            }
        }
    }

    val openRow = remember(rows, openApp) { rows.indexOfFirst { row -> row.any { it.packageName == openApp } } }
    val shelfUris by remember(openApp) { derivedStateOf(structuralEqualityPolicy()) { postersOf(openApp) } }

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
    LaunchedEffect(openRow, focusedApp, origin, viewport, geometry) {
        val row = sections.rowOf(focus.focusedApp, Dimens.GridColumns)
        if (row < 0 || viewport <= 0f) return@LaunchedEffect
        onAnchor(geometry.next(anchor(), row, if (openRow < 0) -1 else openRow + firstAppRow, viewport, origin))
    }
    LaunchedEffect(movingApp, rows) {
        if (movingApp == null) return@LaunchedEffect
        withFrameNanos { }
        focus.requesterFor(movingApp).tryRequestFocus()
    }
    LaunchedEffect(focusEnabled) {
        if (!focusEnabled) openApp = null
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
        val requester = focus.requesterFor(key)
        if (requester.tryRequestFocus()) return
        val index = sections.upNextKeys.indexOf(key)
        if (index < 0) return
        upNextScroll.scrollToItem(index)
        withFrameNanos { }
        requester.tryRequestFocus()
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
                SectionTitle(R.string.upnext_title, "section-title-upnext", padded = true, onHeight = { titleHeight = it })
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
                    shelfUris = if (rowIndex == openRow) shelfUris else NoShelf,
                    posterSize = posterSize,
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

private val NoShelf: List<String> = emptyList()

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
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    indexOffset: Int = 0,
    isFocusEnabled: () -> Boolean = { focusEnabled },
) {
    Column {
        AnimatedVisibility(
            visible = shelfUris.isNotEmpty(),
            enter = expandVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                fadeIn(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
            exit = shrinkVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                fadeOut(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
        ) {
            ShelfPanel(shelfUris, posterSize, Modifier.padding(bottom = Dimens.GridRowSpacing))
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
                        focusRequester = focus.requesterFor(app.packageName),
                        lifted = app.packageName == movingApp,
                        modifier = Modifier
                            .weight(1f)
                            .then(if (app.packageName == entryApp) Modifier.focusRequester(focusRequester) else Modifier),
                    )
                }
            }
            repeat(Dimens.GridColumns - apps.size) {
                Spacer(Modifier.weight(1f))
            }
        }
    }
}
