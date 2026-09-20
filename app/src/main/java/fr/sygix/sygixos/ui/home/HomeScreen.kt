package fr.sygix.sygixos.ui.home

import android.content.Intent
import androidx.core.graphics.drawable.toBitmap
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.model.TvApp

@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(LocalContext.current))) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    when (val s = state) {
        is HomeState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Chargement…", style = MaterialTheme.typography.titleMedium)
        }
        is HomeState.Ready -> AppGrid(s.apps, viewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppGrid(apps: List<TvApp>, viewModel: HomeViewModel) {
    val context = LocalContext.current
    var menuApp by remember { mutableStateOf<TvApp?>(null) }

    LazyVerticalGrid(
        columns = GridCells.Fixed(7),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 32.dp),
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        items(apps, key = { it.packageName }) { app ->
            AppTile(
                app = app,
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

    menuApp?.let { app ->
        AlertDialog(
            onDismissRequest = { menuApp = null },
            title = { Text(app.label) },
            text = { Text(app.packageName, style = MaterialTheme.typography.bodyMedium) },
            confirmButton = {
                TextButton(onClick = { viewModel.togglePin(app); menuApp = null }) {
                    Text("Épingler / Désépingler")
                }
            },
            dismissButton = {
                TextButton(onClick = { menuApp = null }) { Text("Fermer") }
            },
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppTile(app: TvApp, onClick: () -> Unit, onLongClick: () -> Unit) {
    val context = LocalContext.current
    val icon = remember(app.packageName) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName).toBitmap().asImageBitmap()
        }.getOrNull()
    }
    Column(
        modifier = Modifier
            .tvFocus()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(22.dp))
                .background(Color(0xFF141418)),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) {
                Image(icon, contentDescription = app.label, contentScale = ContentScale.Crop)
            } else {
                Text(app.label.take(1).uppercase())
            }
        }
        Text(
            app.label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.85f),
            maxLines = 1,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}
