package fr.sygix.sygixos.ui.home

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.model.TvApp

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(LocalContext.current))) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is HomeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chargement…", style = MaterialTheme.typography.titleMedium)
        }
        is HomeState.Ready -> LauncherHome(s.catalog, viewModel::togglePin)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LauncherHome(catalog: fr.sygix.sygixos.data.Catalog, onTogglePin: (TvApp) -> Unit) {
    val context = LocalContext.current
    var menuApp by remember { mutableStateOf<TvApp?>(null) }
    var dockFocused by remember { mutableStateOf(true) }
    val dockAlpha by animateFloatAsState(
        targetValue = if (dockFocused) 1f else 0.9f,
        animationSpec = tween(300),
        label = "dockAlpha",
    )
    val gridRevealed by remember {
        derivedStateOf { !dockFocused }
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier
                .fillMaxSize()
                .alpha(if (gridRevealed) 1f else 0.35f)
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(catalog.grid, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    onFocusChanged = { if (it) dockFocused = false },
                    onClick = {
                        context.startActivity(
                            context.packageManager.getLaunchIntentForPackage(app.packageName)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    },
                    onLongClick = { menuApp = app },
                )
            }
        }

        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp)
                .alpha(dockAlpha),
            content = {
                Row(
                    modifier = Modifier.padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (catalog.dock.isEmpty()) {
                        Text(
                            "Épinglez des apps depuis la grille (appui long)",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 24.dp),
                        )
                    }
                    catalog.dock.forEach { app ->
                        DockTile(
                            app = app,
                            onFocusChanged = { if (it) dockFocused = true },
                            onClick = {
                                context.startActivity(
                                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                            onLongClick = { menuApp = app },
                        )
                    }
                }
            },
        )
    }

    menuApp?.let { app ->
        AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(app.label) },
            text = { Text(app.packageName, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { onTogglePin(app); menuApp = null }) {
                    Text("Épingler / Désépingler")
                }
            },
            dismissButton = { TextButton(onClick = { menuApp = null }) { Text("Fermer") } },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(
    app: TvApp,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val icon = appIcon(app)
    Column(
        modifier = Modifier
            .tvFocus(onFocused = onFocusChanged)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TileBox(icon, app.label)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DockTile(
    app: TvApp,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val icon = appIcon(app)
    Box(
        modifier = Modifier
            .width(96.dp)
            .tvFocus(onFocused = onFocusChanged)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TileBox(icon, app.label)
        }
    }
}

@Composable
private fun appIcon(app: TvApp): ImageBitmap? {
    val pm = LocalContext.current.packageManager
    return remember(app.packageName) {
        runCatching { pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }.getOrNull()
    }
}

@Composable
private fun TileBox(icon: androidx.compose.ui.graphics.ImageBitmap?, label: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141418)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(icon, contentDescription = label, contentScale = ContentScale.Crop)
        } else {
            Text(label.take(1).uppercase())
        }
    }
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.85f),
        maxLines = 1,
        modifier = Modifier.padding(top = 8.dp),
    )
}
