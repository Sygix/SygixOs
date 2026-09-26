/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import kotlin.math.abs
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.domain.GridScroll
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.domain.VisualQuality
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun HomeGrid(
    catalog: Catalog,
    alpha: Float,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    shelfPrograms: List<HeroItem>,
    validatedVisuals: Set<String>,
    checkedVisuals: Set<String>,
    onAppFocused: (String) -> Unit,
    onTileFocus: (rowIndex: Int) -> Unit,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
    initialShelfApp: String? = null,
    movingApp: String? = null,
) {
    val rows = remember(catalog.grid) { catalog.grid.chunked(Dimens.GridColumns) }
    val packages = remember(catalog.grid) { catalog.grid.map { it.packageName } }
    val focus = rememberTileFocus(packages, focusEnabled, initialShelfApp)
    val focusedApp = focus.focusedApp
    val entryApp = focus.entry(packages)
    var openApp by remember { mutableStateOf<String?>(null) }

    val visuals by rememberUpdatedState(validatedVisuals)
    val settled by rememberUpdatedState(checkedVisuals)
    val programs by rememberUpdatedState(shelfPrograms)
    fun postersOf(packageName: String?): List<String> =
        packageName?.let { ShelfPosters.forPackage(programs, visuals, it) }.orEmpty()

    fun pendingFor(packageName: String?): Boolean = packageName
        ?.let { ShelfPosters.candidates(programs, it, VisualQuality.SHELF_VALIDATED_PER_APP) }
        ?.any { it !in settled } ?: false

    LaunchedEffect(focusedApp, movingApp) {
        val target = focus.focusedApp
        if (movingApp != null) {
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
    val shelfUris = postersOf(openApp)

    val scrollState = rememberScrollState()
    val density = LocalDensity.current
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    val contentWidth = screenWidth - Dimens.ScreenMarginH * 2
    val rowHeight = (contentWidth - Dimens.GridSpacing * (Dimens.GridColumns - 1)) / Dimens.GridColumns * 9f / 16f
    val panelBlock = contentWidth / Dimens.ShelfAspectRatio + Dimens.GridRowSpacing
    val geometry = remember(density, screenWidth) {
        with(density) {
            GridScroll(
                topMargin = Dimens.GridTopMargin.toPx(),
                rowHeight = rowHeight.toPx(),
                rowSpacing = Dimens.GridRowSpacing.toPx(),
                panelHeight = panelBlock.toPx(),
                margin = Dimens.GridRowSpacing.toPx(),
            )
        }
    }
    var anchor by remember { mutableStateOf<GridScroll.Anchor?>(null) }

    fun focusedRow(): Int = rows.indexOfFirst { row -> row.any { it.packageName == focus.focusedApp } }

    LaunchedEffect(openRow, focusedApp) {
        val row = focusedRow()
        if (row < 0) return@LaunchedEffect
        withFrameNanos { }
        val viewport = scrollState.viewportSize.toFloat()
        if (viewport <= 0f) return@LaunchedEffect
        val next = geometry.next(anchor, row, openRow, viewport)
        anchor = next
        val delta = next.scroll - scrollState.value
        if (abs(delta) >= 1f) {
            scrollState.animateScrollBy(delta, tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing))
        }
    }
    LaunchedEffect(movingApp, rows) {
        if (movingApp == null) return@LaunchedEffect
        withFrameNanos { }
        focus.requesterFor(movingApp).tryRequestFocus()
    }
    LaunchedEffect(focusEnabled) {
        if (!focusEnabled) openApp = null
    }

    if (catalog.grid.isEmpty()) {
        Box(modifier.fillMaxSize().alpha(alpha), contentAlignment = Alignment.Center) {
            Text("Aucune app TV détectée", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.6f))
        }
        return
    }

    CompositionLocalProvider(LocalBringIntoViewSpec provides NoAutoScroll) {
        Column(
            modifier
                .fillMaxSize()
                .alpha(alpha)
                .verticalScroll(scrollState)
                .padding(horizontal = Dimens.ScreenMarginH, vertical = Dimens.GridTopMargin)
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(Dimens.GridRowSpacing),
        ) {
            rows.forEachIndexed { rowIndex, rowApps ->
                Column {
                    AnimatedVisibility(
                        visible = rowIndex == openRow && shelfUris.isNotEmpty(),
                        enter = expandVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                            fadeIn(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
                        exit = shrinkVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                            fadeOut(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
                    ) {
                        ShelfPanel(shelfUris, Modifier.padding(bottom = Dimens.GridRowSpacing))
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        rowApps.forEachIndexed { column, app ->
                            key(app.packageName) {
                                AppTile(
                                    app = app,
                                    focusEnabled = focusEnabled,
                                    onClick = { onTileClick(app) },
                                    onLongClick = { onTileLongClick(app) },
                                    onFocusChanged = { focused ->
                                        if (focused) {
                                            onTileFocus(rowIndex)
                                            focus.onFocused(app.packageName, rowIndex * Dimens.GridColumns + column)
                                            onAppFocused(app.packageName)
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
                        repeat(Dimens.GridColumns - rowApps.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

private val NoAutoScroll = object : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = 0f
}
