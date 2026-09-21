package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.data.TvProviderHeroSource
import fr.sygix.sygixos.model.TvApp

private const val GRID_COLUMNS = 5

/** Grille d'apps en rangées, avec panneau Top Shelf inséré sous la rangée focusée. */
@Composable
internal fun HomeGrid(
    catalog: Catalog,
    alpha: Float,
    onTileFocus: () -> Unit,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    firstTileFocusRequester: androidx.compose.ui.focus.FocusRequester,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    var focusedShelfApp by remember { mutableStateOf<String?>(null) }
    var shelfUris by remember { mutableStateOf<List<String>>(emptyList()) }
    val shelfSource = remember { TvProviderHeroSource(context.applicationContext) }
    LaunchedEffect(focusedShelfApp) {
        shelfUris = focusedShelfApp?.let { pkg ->
            runCatching { shelfSource.posterUrisFor(pkg) }.getOrDefault(emptyList())
        } ?: emptyList()
    }

    val gridRows = catalog.grid.chunked(GRID_COLUMNS)
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        gridRows.forEachIndexed { rowIndex, rowApps ->
            item(key = "row-$rowIndex") {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                    rowApps.forEachIndexed { colIndex, app ->
                        Box(Modifier.weight(1f)) {
                            AppTile(
                                app = app,
                                focusRequester = if (rowIndex == 0 && colIndex == 0) firstTileFocusRequester else null,
                                onFocusChanged = {
                                    if (it) {
                                        onTileFocus()
                                        focusedShelfApp = app.packageName
                                    }
                                },
                                onClick = { onTileClick(app) },
                                onLongClick = { onTileLongClick(app) },
                                onMenuKey = { onTileLongClick(app) },
                            )
                        }
                    }
                    repeat(GRID_COLUMNS - rowApps.size) {
                        Box(Modifier.weight(1f))
                    }
                }
            }
            if (rowApps.any { it.packageName == focusedShelfApp } && shelfUris.isNotEmpty()) {
                item(key = "shelf-$rowIndex") {
                    ShelfPanel(shelfUris)
                }
            }
        }
    }
}

/** Preview pour le screenshot Roborazzi : grille avec panneau shelf affiché. */
@Composable
internal fun GridWithShelfPreview(catalog: Catalog) {
    val gridRows = catalog.grid.chunked(GRID_COLUMNS)
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        gridRows.forEachIndexed { rowIndex, rowApps ->
            item(key = "row-$rowIndex") {
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                    rowApps.forEach { app ->
                        Box(Modifier.weight(1f)) {
                            AppTile(app, onFocusChanged = {}, onClick = {}, onLongClick = {})
                        }
                    }
                }
            }
            if (rowIndex == 0) {
                item(key = "shelf-$rowIndex") {
                    ShelfPanel(
                        listOf(
                            "https://example.com/poster-a.jpg",
                            "https://example.com/poster-b.jpg",
                        ),
                    )
                }
            }
        }
    }
}
