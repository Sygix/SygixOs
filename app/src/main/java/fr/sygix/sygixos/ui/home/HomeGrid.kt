package fr.sygix.sygixos.ui.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Grille d'apps en rangées 16:9. La rangée active reste à hauteur fixe sous le panneau
 * Top Shelf, surface fixe dessinée PAR-DESSUS la liste ; la place du panneau est une
 * marge haute de la rangée active, animée d'une rangée à l'autre. Aucun item n'est
 * réordonné (LazyColumn ré-ancrerait son défilement), les rangées glissent derrière le
 * panneau. La liste déborde d'une rangée au-dessus et au-dessous de l'écran pour que le
 * focus n'entraîne jamais son propre défilement.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HomeGrid(
    catalog: Catalog,
    alpha: Float,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    shelfPrograms: List<HeroItem>,
    validatedVisuals: Set<String>,
    onTileFocus: (rowIndex: Int) -> Unit,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
    initialShelfApp: String? = null,
    movingApp: String? = null,
) {
    var focusedApp by remember { mutableStateOf(initialShelfApp) }
    val rows = remember(catalog.grid) { catalog.grid.chunked(Dimens.GridColumns) }
    val activeRow = remember(rows, focusedApp) { rows.indexOfFirst { row -> row.any { it.packageName == focusedApp } } }
    // Traversée rapide de la grille : l'app du panneau ne change qu'une fois le focus posé.
    var settledApp by remember { mutableStateOf(initialShelfApp) }
    LaunchedEffect(focusedApp) {
        if (settledApp != null) delay(Motion.SHELF_SETTLE_MS)
        settledApp = focusedApp
    }
    val shelfUris = remember(settledApp, shelfPrograms, validatedVisuals) {
        settledApp?.let { ShelfPosters.forPackage(shelfPrograms, validatedVisuals, it) } ?: emptyList()
    }

    val listState = rememberLazyListState()
    val tileRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(packageName: String) = tileRequesters.getOrPut(packageName) { FocusRequester() }

    // Après un menu, la mémoire du focusRestorer peut être perdue : on revient sur la
    // dernière tuile focusée si sa rangée est composée, sinon la première rangée visible.
    fun restoreTarget(): FocusRequester {
        val visibleRows = listState.layoutInfo.visibleItemsInfo
            .mapNotNull { (it.key as? String)?.removePrefix("row-")?.toIntOrNull() }
        val row = activeRow.takeIf { it in visibleRows } ?: visibleRows.minOrNull() ?: 0
        val app = rows.getOrNull(row)?.firstOrNull { it.packageName == focusedApp } ?: rows.getOrNull(row)?.firstOrNull()
        return app?.let { requesterFor(it.packageName) } ?: FocusRequester.Default
    }

    if (catalog.grid.isEmpty()) {
        Box(modifier.fillMaxSize().alpha(alpha), contentAlignment = Alignment.Center) {
            Text("Aucune app TV détectée", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.6f))
        }
        return
    }

    BoxWithConstraints(modifier.fillMaxSize().alpha(alpha)) {
        val contentWidth = maxWidth - Dimens.ScreenMarginH * 2
        val tileHeight = (contentWidth - Dimens.GridSpacing * (Dimens.GridColumns - 1)) / Dimens.GridColumns * 9f / 16f
        val stride = tileHeight + Dimens.GridRowSpacing
        val overscan = stride
        val panelHeight = contentWidth / Dimens.ShelfAspectRatio + 8.dp
        // Marge haute de la rangée active : la place du panneau plus un espacement.
        val gap = panelHeight + Dimens.GridRowSpacing
        fun panelTop(row: Int): Dp = Dimens.GridTopMargin + if (row == 0) 0.dp else Dimens.ShelfPeek + Dimens.GridRowSpacing
        val density = LocalDensity.current
        val scrollSpec = tween<Float>(Motion.SHELF_SCROLL_MS, easing = AppleEasing)

        // Transition de rangée : une seule progression pilote la marge qui se dégonfle
        // sur l'ancienne rangée, celle qui se gonfle sur la nouvelle (somme constante) et
        // le défilement d'une rangée. Aucun item n'est réordonné : la liste ne saute jamais.
        var previousRow by remember { mutableIntStateOf(-1) }
        var fromRow by remember { mutableIntStateOf(-1) }
        val progress = remember { Animatable(1f) }

        LaunchedEffect(focusEnabled) {
            if (!focusEnabled || activeRow < 0) return@LaunchedEffect
            listState.scrollToItem(activeRow, with(density) { (Dimens.GridTopMargin - panelTop(activeRow)).roundToPx() })
        }
        LaunchedEffect(activeRow) {
            if (activeRow < 0) return@LaunchedEffect
            val from = previousRow
            previousRow = activeRow
            if (from < 0 || from == activeRow) {
                fromRow = -1
                progress.snapTo(1f)
                listState.scrollToItem(activeRow, with(density) { (Dimens.GridTopMargin - panelTop(activeRow)).roundToPx() })
                return@LaunchedEffect
            }
            fromRow = from
            progress.snapTo(0f)
            val delta = with(density) { (stride * (activeRow - from) + panelTop(from) - panelTop(activeRow)).toPx() }
            coroutineScope {
                launch { progress.animateTo(1f, scrollSpec) }
                launch { listState.animateScrollBy(delta, scrollSpec) }
            }
            fromRow = -1
        }
        // En déplacement, la tuile change de rangée : son nœud est recréé, on lui redonne le focus.
        LaunchedEffect(movingApp, rows) {
            if (movingApp == null) return@LaunchedEffect
            withFrameNanos { }
            requesterFor(movingApp).tryRequestFocus()
        }
        val panelTopAnimated by animateDpAsState(
            targetValue = panelTop(maxOf(activeRow, 0)),
            animationSpec = tween(Motion.SHELF_SCROLL_MS, easing = AppleEasing),
            label = "shelfTop",
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .requiredHeight(maxHeight + overscan * 2)
                .offset(y = -overscan)
                .focusRequester(focusRequester)
                .focusRestorer { restoreTarget() }
                .focusGroup(),
            contentPadding = PaddingValues(
                start = Dimens.ScreenMarginH,
                end = Dimens.ScreenMarginH,
                top = overscan + Dimens.GridTopMargin,
                bottom = overscan + Dimens.GridTopMargin,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.GridRowSpacing),
        ) {
            rows.forEachIndexed { rowIndex, rowApps ->
                item(key = "row-$rowIndex") {
                    // La progression peut dépasser 1 de quelques 1e-7 en fin d'animation :
                    // sans borne, la marge devient infinitésimalement négative et Compose lève.
                    val p = progress.value.coerceIn(0f, 1f)
                    val extraTop = when (rowIndex) {
                        activeRow -> gap * (if (fromRow >= 0) p else 1f)
                        fromRow -> gap * (1f - p)
                        else -> 0.dp
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = extraTop),
                    ) {
                        rowApps.forEach { app ->
                            AppTile(
                                app = app,
                                focusEnabled = focusEnabled,
                                onClick = { onTileClick(app) },
                                onLongClick = { onTileLongClick(app) },
                                onFocusChanged = { focused ->
                                    if (focused) {
                                        onTileFocus(rowIndex)
                                        focusedApp = app.packageName
                                    }
                                },
                                focusRequester = requesterFor(app.packageName),
                                lifted = app.packageName == movingApp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        repeat(Dimens.GridColumns - rowApps.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        }
        if (activeRow >= 0) {
            ShelfPanel(
                uris = if (movingApp == null) shelfUris else emptyList(),
                modifier = Modifier
                    .padding(start = Dimens.ScreenMarginH, end = Dimens.ScreenMarginH, top = panelTopAnimated.coerceAtLeast(0.dp))
                    .zIndex(1f),
            )
        }
    }
}
