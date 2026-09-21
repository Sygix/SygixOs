package fr.sygix.sygixos.ui.home

import android.content.Intent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.data.DefaultHeroProvider
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.hero.GradientFallback
import fr.sygix.sygixos.ui.hero.HeroCarousel

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(LocalContext.current))) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var heroItems by remember { mutableStateOf(emptyList<HeroItem>()) }
    LaunchedEffect(Unit) {
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

private enum class Zone { HERO, DOCK, GRID }

@Composable
internal fun LauncherHome(
    catalog: Catalog,
    heroItems: List<HeroItem>,
    onTogglePin: (TvApp) -> Unit,
) {
    val context = LocalContext.current
    var menuApp by remember { mutableStateOf<TvApp?>(null) }
    var zone by remember { mutableStateOf(Zone.HERO) }
    val dockFocus = remember { FocusRequester() }
    val gridFocus = remember { FocusRequester() }

    // Demande de focus différée en post-composition (requestFocus immédiat = course)
    var pendingFocus by remember { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(pendingFocus) {
        pendingFocus?.let { runCatching { it.requestFocus() } }
        pendingFocus = null
    }
    val gridAlpha by animateFloatAsState(if (zone == Zone.GRID) 1f else 0f, tween(300), label = "gridAlpha")
    val heroAlpha by animateFloatAsState(if (zone == Zone.GRID) 0f else 1f, tween(300), label = "heroAlpha")
    val dockAlpha by animateFloatAsState(if (zone == Zone.GRID) 0f else 1f, tween(300), label = "dockAlpha")

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .onPreviewKeyEvent { e ->
                if (e.type != androidx.compose.ui.input.key.KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (e.key) {
                    Key.DirectionDown -> when (zone) {
                        Zone.HERO -> {
                            if (catalog.dock.isNotEmpty()) {
                                zone = Zone.DOCK
                                pendingFocus = dockFocus
                            } else {
                                zone = Zone.GRID
                                pendingFocus = gridFocus
                            }
                            true
                        }
                        Zone.DOCK -> {
                            zone = Zone.GRID
                            pendingFocus = gridFocus
                            true
                        }
                        else -> false
                    }
                    Key.DirectionUp -> if (zone == Zone.DOCK) {
                        zone = Zone.HERO
                        true
                    } else {
                        false
                    }
                    else -> false
                }
            },
    ) {
        // Fond dégradé neutre type tvOS quand on est dans la grille
        if (zone == Zone.GRID) {
            GradientFallback(Modifier.fillMaxSize())
        }

        HomeGrid(
            catalog = catalog,
            alpha = gridAlpha,
            onTileFocus = { zone = Zone.GRID },
            firstTileFocusRequester = gridFocus,
            onTileClick = { app ->
                context.startActivity(
                    context.packageManager.getLaunchIntentForPackage(app.packageName)
                        ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            },
            onTileLongClick = { menuApp = it },
        )

        if (heroItems.isEmpty()) {
            GradientFallback(Modifier.alpha(heroAlpha).fillMaxSize())
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
            firstTileFocusRequester = dockFocus,
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
            onDismissRequest = {
                menuApp = null
                // Le focus a pu être perdu (app épinglée retirée de la grille) : on le restaure
                pendingFocus = when (zone) {
                    Zone.DOCK -> dockFocus
                    Zone.GRID -> gridFocus
                    else -> null
                }
            },
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
