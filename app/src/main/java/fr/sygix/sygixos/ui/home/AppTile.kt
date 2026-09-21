package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.model.TvApp

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppTile(
    app: TvApp,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onMenuKey: () -> Unit = {},
) {
    val icon = appIcon(app)
    Column(
        modifier = Modifier
            .onPreviewKeyEvent { e ->
                if (e.key == Key.Menu && e.type == KeyEventType.KeyDown) {
                    onMenuKey()
                    true
                } else {
                    false
                }
            }
            .tvFocus(onFocused = onFocusChanged, focusRequester = focusRequester)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        TileBox(icon, app.label)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun DockTile(
    app: TvApp,
    onFocusChanged: (Boolean) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    focusRequester: androidx.compose.ui.focus.FocusRequester? = null,
    onMenuKey: () -> Unit = {},
) {
    val icon = appIcon(app)
    Box(
        modifier = Modifier
            .onPreviewKeyEvent { e ->
                if (e.key == Key.Menu && e.type == KeyEventType.KeyDown) {
                    onMenuKey()
                    true
                } else {
                    false
                }
            }
            .width(96.dp)
            .tvFocus(onFocused = onFocusChanged, focusRequester = focusRequester)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            TileBox(icon, app.label)
        }
    }
}

@Composable
internal fun appIcon(app: TvApp): ImageBitmap? {
    val pm = LocalContext.current.packageManager
    return remember(app.packageName) {
        runCatching { pm.getApplicationIcon(app.packageName).toBitmap().asImageBitmap() }.getOrNull()
    }
}

@Composable
internal fun TileBox(icon: ImageBitmap?, label: String) {
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
