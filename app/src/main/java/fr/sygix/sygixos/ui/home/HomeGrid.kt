package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp

/**
 * Grille d'apps en rangées 16:9. La rangée active reste à hauteur fixe : la rangée
 * précédente dépasse en haut, le panneau Top Shelf de l'app focus s'insère juste
 * au-dessus de la rangée active, les rangées suivantes occupent le reste.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun HomeGrid(
    catalog: Catalog,
    alpha: Float,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    shelfPrograms: List<HeroItem>,
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
    val shelfUris = remember(focusedApp, shelfPrograms) {
        focusedApp?.let { ShelfPosters.forPackage(shelfPrograms, it) } ?: emptyList()
    }
    val shelfShown = activeRow >= 0 && shelfUris.isNotEmpty() && movingApp == null
    val listState = rememberLazyListState()
    val tileRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(packageName: String) = tileRequesters.getOrPut(packageName) { FocusRequester() }

    // Après un dialogue (fenêtre séparée), la mémoire du focusRestorer est perdue :
    // on revient sur la dernière tuile focusée si sa rangée est composée, sinon la première rangée visible.
    fun restoreTarget(): FocusRequester {
        val visibleRows = listState.layoutInfo.visibleItemsInfo
            .mapNotNull { (it.key as? String)?.removePrefix("row-")?.toIntOrNull() }
        val row = activeRow.takeIf { it in visibleRows } ?: visibleRows.minOrNull() ?: 0
        val app = rows.getOrNull(row)?.firstOrNull { it.packageName == focusedApp } ?: rows.getOrNull(row)?.firstOrNull()
        return app?.let { requesterFor(it.packageName) } ?: FocusRequester.Default
    }
    val peekPx = with(LocalDensity.current) { (Dimens.ShelfPeek + Dimens.GridRowSpacing).roundToPx() }

    LaunchedEffect(activeRow, shelfShown) {
        if (activeRow < 0) return@LaunchedEffect
        // Le panneau, quand il existe, est l'item juste avant la rangée active : même index.
        listState.animateScrollToItem(activeRow, if (activeRow == 0) 0 else -peekPx)
    }
    // En déplacement, la tuile change de rangée : son nœud est recréé, on lui redonne le focus.
    LaunchedEffect(movingApp, rows) {
        if (movingApp == null) return@LaunchedEffect
        withFrameNanos { }
        requesterFor(movingApp).tryRequestFocus()
    }

    if (catalog.grid.isEmpty()) {
        Box(modifier.fillMaxSize().alpha(alpha), contentAlignment = Alignment.Center) {
            Text(
                "Aucune app TV détectée",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
        return
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .focusRequester(focusRequester)
            .focusRestorer { restoreTarget() }
            .focusGroup(),
        contentPadding = PaddingValues(horizontal = Dimens.ScreenMarginH, vertical = Dimens.GridTopMargin),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridRowSpacing),
    ) {
        rows.forEachIndexed { rowIndex, rowApps ->
            if (shelfShown && rowIndex == activeRow) {
                item(key = "shelf") {
                    ShelfPanel(shelfUris, Modifier.animateItem())
                }
            }
            item(key = "row-$rowIndex") {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
                    modifier = Modifier.fillMaxWidth().animateItem(),
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
}
