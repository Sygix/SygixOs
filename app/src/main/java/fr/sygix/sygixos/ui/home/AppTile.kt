package fr.sygix.sygixos.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.data.AppArtwork
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalAppArtwork = staticCompositionLocalOf<AppArtworkSource?> { null }

private val TileBackground = Color(0xFF141418)

/** Tuile d'app 16:9 (grille et dock) : bannière Android TV, sinon icône entière. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun AppTile(
    app: TvApp,
    focusEnabled: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocusChanged: (Boolean) -> Unit = {},
    focusRequester: FocusRequester? = null,
    lifted: Boolean = false,
) {
    val artwork by rememberAppArtwork(app)
    Box(
        modifier
            .onPreviewKeyEvent { e ->
                if (e.key == Key.Menu && e.type == KeyEventType.KeyDown) {
                    onLongClick()
                    true
                } else {
                    false
                }
            }
            .tvFocus(onFocused = onFocusChanged, focusRequester = focusRequester, enabled = focusEnabled)
            .combinedClickable(
                interactionSource = null,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        TileBox(artwork, app.label, lifted)
    }
}

@Composable
private fun rememberAppArtwork(app: TvApp): State<AppArtwork?> {
    val context = LocalContext.current
    val provided = LocalAppArtwork.current
    val source = remember(provided) { provided ?: AppArtworkSource(context.packageManager) }
    return produceState<AppArtwork?>(initialValue = null, app.packageName, source) {
        value = withContext(Dispatchers.IO) { runCatching { source.load(app) }.getOrNull() }
    }
}

@Composable
internal fun TileBox(artwork: AppArtwork?, label: String, lifted: Boolean = false) {
    val shape = RoundedCornerShape(Dimens.TileCorner)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(TileBackground)
            .then(if (lifted) Modifier.border(3.dp, Color.White, shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(targetState = artwork, animationSpec = tween(250), label = "tileArtwork") { art ->
            when {
                art == null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(label.take(1).uppercase(), style = MaterialTheme.typography.titleLarge, color = Color.White.copy(alpha = 0.85f))
                }
                art.isBanner -> Image(
                    bitmap = art.bitmap.asImageBitmap(),
                    contentDescription = label,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Image(
                        bitmap = art.bitmap.asImageBitmap(),
                        contentDescription = label,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxHeight(0.56f).aspectRatio(1f),
                    )
                }
            }
        }
    }
}
