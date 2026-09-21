package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.model.TvApp

/** Rail Liquid Glass des apps épinglées, en overlay bas du héro. Tuiles de la taille de la grille, alignées sur ses colonnes. */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun Dock(
    apps: List<TvApp>,
    alpha: Float,
    focusEnabled: Boolean,
    focusRequester: FocusRequester,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focusedApp by remember { mutableStateOf<String?>(null) }
    val tileRequesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(packageName: String) = tileRequesters.getOrPut(packageName) { FocusRequester() }
    fun restoreTarget(): FocusRequester {
        val app = apps.firstOrNull { it.packageName == focusedApp } ?: apps.firstOrNull()
        return app?.let { requesterFor(it.packageName) } ?: FocusRequester.Default
    }
    GlassSurface(
        modifier = modifier
            .padding(start = Dimens.DockOuterMargin, end = Dimens.DockOuterMargin, bottom = Dimens.DockBottomMargin)
            .fillMaxWidth()
            .alpha(alpha)
            .focusRequester(focusRequester)
            .focusRestorer { restoreTarget() }
            .focusGroup(),
    ) {
        if (apps.isEmpty()) {
            Text(
                "Épinglez des apps depuis la grille (appui long ou touche Menu)",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 28.dp, vertical = 36.dp),
            )
        } else {
            BoxWithConstraints(Modifier.padding(Dimens.DockPadding).fillMaxWidth()) {
                val columns = maxOf(apps.size, Dimens.GridColumns)
                val tileWidth = (maxWidth - Dimens.GridSpacing * (columns - 1)) / columns
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing, Alignment.CenterHorizontally),
                ) {
                    apps.forEach { app ->
                        AppTile(
                            app = app,
                            focusEnabled = focusEnabled,
                            onClick = { onTileClick(app) },
                            onLongClick = { onTileLongClick(app) },
                            onFocusChanged = { if (it) focusedApp = app.packageName },
                            focusRequester = requesterFor(app.packageName),
                            modifier = Modifier.width(tileWidth),
                        )
                    }
                }
            }
        }
    }
}
