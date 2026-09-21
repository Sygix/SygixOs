package fr.sygix.sygixos.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
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
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.domain.ShelfPosters
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.delay

/**
 * Grille d'apps en rangées 16:9, entièrement visible à l'arrivée. Le panneau d'aperçu
 * ne s'ouvre qu'après une pause du focus sur une app qui a des visuels : il s'insère
 * au-dessus de la rangée focusée et pousse la grille vers le bas. Colonne défilante
 * simple : toutes les tuiles sont composées, donc le défilement ne se ré-ancre jamais
 * (source des sauts) et la tuile focusée est ramenée dans la vue à chaque changement.
 */
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
internal fun HomeGrid(
    catalog: Catalog,
    alpha: Float,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    shelfPrograms: List<HeroItem>,
    validatedVisuals: Set<String>,
    onAppFocused: (String) -> Unit,
    onTileFocus: (rowIndex: Int) -> Unit,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
    initialShelfApp: String? = null,
    movingApp: String? = null,
) {
    val rows = remember(catalog.grid) { catalog.grid.chunked(Dimens.GridColumns) }
    var focusedApp by remember { mutableStateOf(initialShelfApp) }
    var openApp by remember { mutableStateOf<String?>(null) }

    // Les visuels sont validés en flux : on les lit comme un état pour ne pas relancer le minuteur à chaque arrivée.
    val visuals by rememberUpdatedState(validatedVisuals)
    val programs by rememberUpdatedState(shelfPrograms)
    fun postersOf(packageName: String?): List<String> =
        packageName?.let { ShelfPosters.forPackage(programs, visuals, it) }.orEmpty()

    // Pause avant la première ouverture ; bascule immédiate tant qu'un panneau est ouvert ;
    // fermeture dès que l'app focusée n'a aucun visuel.
    LaunchedEffect(focusedApp, movingApp) {
        val target = focusedApp
        if (movingApp != null) {
            openApp = null
            return@LaunchedEffect
        }
        if (postersOf(target).isEmpty()) openApp = null
        if (openApp == null) delay(Motion.SHELF_OPEN_DELAY_MS)
        snapshotFlow { postersOf(target) }.collect { posters ->
            openApp = target.takeIf { posters.isNotEmpty() }
        }
    }

    val openRow = remember(rows, openApp) { rows.indexOfFirst { row -> row.any { it.packageName == openApp } } }
    val shelfUris = postersOf(openApp)

    val scrollState = rememberScrollState()
    val tileRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val rowViewRequesters = remember { mutableMapOf<Int, BringIntoViewRequester>() }
    fun requesterFor(packageName: String) = tileRequesters.getOrPut(packageName) { FocusRequester() }
    fun viewRequesterFor(row: Int) = rowViewRequesters.getOrPut(row) { BringIntoViewRequester() }

    fun focusedRow(): Int = rows.indexOfFirst { row -> row.any { it.packageName == focusedApp } }

    fun restoreTarget(): FocusRequester {
        val app = catalog.grid.firstOrNull { it.packageName == focusedApp } ?: catalog.grid.firstOrNull()
        return app?.let { requesterFor(it.packageName) } ?: FocusRequester.Default
    }

    // Le panneau pousse les rangées : on ramène la tuile focusée dans la vue une fois l'animation finie.
    LaunchedEffect(openRow, focusedApp) {
        val row = focusedRow()
        if (row < 0) return@LaunchedEffect
        withFrameNanos { }
        delay(Motion.SHELF_EXPAND_MS.toLong())
        runCatching { viewRequesterFor(row).bringIntoView() }
    }
    // En déplacement, la tuile change de rangée : son nœud est recréé, on lui redonne le focus.
    LaunchedEffect(movingApp, rows) {
        if (movingApp == null) return@LaunchedEffect
        withFrameNanos { }
        requesterFor(movingApp).tryRequestFocus()
    }
    // À la sortie de la grille, l'aperçu se referme et la grille revient en haut.
    LaunchedEffect(focusEnabled) {
        if (!focusEnabled) {
            openApp = null
            scrollState.scrollTo(0)
        }
    }

    if (catalog.grid.isEmpty()) {
        Box(modifier.fillMaxSize().alpha(alpha), contentAlignment = Alignment.Center) {
            Text("Aucune app TV détectée", style = MaterialTheme.typography.titleMedium, color = Color.White.copy(alpha = 0.6f))
        }
        return
    }

    Column(
        modifier
            .fillMaxSize()
            .alpha(alpha)
            .verticalScroll(scrollState)
            .padding(horizontal = Dimens.ScreenMarginH, vertical = Dimens.GridTopMargin)
            .focusRequester(focusRequester)
            .focusRestorer { restoreTarget() }
            .focusGroup(),
        verticalArrangement = Arrangement.spacedBy(Dimens.GridRowSpacing),
    ) {
        rows.forEachIndexed { rowIndex, rowApps ->
            // Aperçu et rangée forment un bloc : c'est lui qu'on amène dans la vue,
            // sinon seule une bande du panneau resterait visible au-dessus de la rangée.
            Column(Modifier.bringIntoViewRequester(viewRequesterFor(rowIndex))) {
            AnimatedVisibility(
                visible = rowIndex == openRow && shelfUris.isNotEmpty(),
                enter = expandVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                    fadeIn(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)),
                exit = shrinkVertically(tween(Motion.SHELF_EXPAND_MS, easing = AppleEasing)) +
                    fadeOut(tween(Motion.SHELF_FADE_MS, easing = AppleEasing)),
            ) {
                ShelfPanel(shelfUris, Modifier.padding(bottom = Dimens.GridRowSpacing))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
                modifier = Modifier.fillMaxWidth(),
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
                                onAppFocused(app.packageName)
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
