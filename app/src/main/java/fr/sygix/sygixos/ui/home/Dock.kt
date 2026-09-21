package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.model.TvApp

private val DockInnerPadding = 24.dp
private val DockTileSpacing = 28.dp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun Dock(
    apps: List<TvApp>,
    alpha: Float,
    onTileFocus: () -> Unit,
    onTileClick: (TvApp) -> Unit,
    onTileLongClick: (TvApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier
            .padding(bottom = 28.dp)
            .fillMaxWidth(0.82f)
            .alpha(alpha),
        content = {
            if (apps.isEmpty()) {
                Text(
                    "Épinglez des apps depuis la grille (appui long)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = DockInnerPadding + 12.dp, vertical = 48.dp),
                )
            } else {
                Row(
                    modifier = Modifier
                        .padding(horizontal = DockInnerPadding, vertical = DockInnerPadding)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(DockTileSpacing, Alignment.CenterHorizontally),
                ) {
                    apps.forEach { app ->
                        Box(modifier = Modifier.weight(1f)) {
                            DockTile(
                                app = app,
                                onFocusChanged = { if (it) onTileFocus() },
                                onClick = { onTileClick(app) },
                                onLongClick = { onTileLongClick(app) },
                            )
                        }
                    }
                }
            }
        },
    )
}
