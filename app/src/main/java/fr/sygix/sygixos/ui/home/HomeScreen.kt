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
import androidx.compose.ui.zIndex
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
import fr.sygix.sygixos.data.DefaultHeroProvider
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.hero.HeroCarousel
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(LocalContext.current))) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    val context = LocalContext.current
    var heroItems by remember { mutableStateOf(emptyList<HeroItem>()) }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        heroItems = runCatching { DefaultHeroProvider(context.applicationContext).load() }
            .getOrDefault(emptyList())
    }
    when (val s = state) {
        is HomeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chargement…", style = MaterialTheme.typography.titleMedium)
        }
        is HomeState.Ready -> LauncherHome(s.catalog, heroItems, viewModel::togglePin)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun LauncherHome(
    catalog: fr.sygix.sygixos.data.Catalog,
    heroItems: List<HeroItem>,
    onTogglePin: (TvApp) -> Unit,
) {
    val context = LocalContext.current
    var menuApp by remember { mutableStateOf<TvApp?>(null) }
    var zone by remember { mutableStateOf(Zone.HERO) }
    val gridAlpha by animateFloatAsState(if (zone == Zone.GRID) 1f else 0f, tween(300), label = "gridAlpha")
    val heroAlpha by animateFloatAsState(if (zone == Zone.GRID) 0f else 1f, tween(300), label = "heroAlpha")
    val dockAlpha by animateFloatAsState(if (zone == Zone.GRID) 0f else 1f, tween(300), label = "dockAlpha")

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(5),
            modifier = Modifier
                .fillMaxSize()
                .alpha(gridAlpha)
                .padding(horizontal = 48.dp, vertical = 32.dp),
            horizontalArrangement = Arrangement.spacedBy(20.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            items(catalog.grid, key = { it.packageName }) { app ->
                AppTile(
                    app = app,
                    onFocusChanged = { if (it) zone = Zone.GRID },
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

        if (heroItems.isEmpty()) {
            fr.sygix.sygixos.ui.hero.GradientFallback(Modifier.alpha(heroAlpha).fillMaxSize())
        }
        HeroCarousel(
            items = heroItems,
            onFocused = { if (it) zone = Zone.HERO },
            onItemClick = { item ->
                val launched = item.launchUri?.let { uri ->
                    runCatching {
                        val intent = Intent.parseUri(uri, Intent.URI_INTENT_SCHEME)
                        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                    }.isSuccess
                } ?: false
                if (!launched) {
                    item.sourcePackage?.let { pkg ->
                        context.startActivity(
                            context.packageManager.getLaunchIntentForPackage(pkg)
                                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }
                }
            },
            modifier = Modifier.alpha(heroAlpha),
        )

        Dock(
            apps = catalog.dock,
            alpha = dockAlpha,
            onTileFocus = { zone = Zone.DOCK },
            onTileClick = { app ->
                context.startActivity(
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            onTileLongClick = { menuApp = it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .zIndex(2f),
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

private enum class Zone { HERO, DOCK, GRID }

private val DockInnerPadding = 24.dp
private val DockTileSpacing = 28.dp
private const val DockTileAspect = 1.2f

@Composable
private fun Dock(
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
            .aspectRatio(1.2f)
            .clip(RoundedCornerShape(22.dp))
            .background(Color(0xFF141418)),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            Image(icon, contentDescription = label, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            Text(label.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.9f))
        }
    }
}
