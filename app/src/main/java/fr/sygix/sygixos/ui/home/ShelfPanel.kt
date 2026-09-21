package fr.sygix.sygixos.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Image
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import android.util.Log
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Quality
import kotlinx.coroutines.delay

/** Panneau Top Shelf au-dessus de la rangée focusée (posters de l'app focus). Disparaît si aucun poster n'est lisible. */
@Composable
internal fun ShelfPanel(uris: List<String>, modifier: Modifier = Modifier) {
    var failed by remember(uris) { mutableStateOf(emptySet<String>()) }
    val loadable = remember(uris, failed) { uris.filter { it !in failed } }
    if (loadable.isEmpty()) return
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(loadable) {
        index = 0
        if (loadable.size > 1) {
            while (true) {
                delay(6000)
                index = (index + 1) % loadable.size
            }
        }
    }
    val pan = rememberInfiniteTransition(label = "kenBurns")
    val panX by pan.animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(16000, easing = AppleEasing), RepeatMode.Reverse),
        label = "kenBurnsX",
    )
    val shape = RoundedCornerShape(16.dp)
    Crossfade(
        targetState = index,
        animationSpec = tween(350, easing = AppleEasing),
        label = "shelfCrossfade",
        modifier = modifier,
    ) { i ->
        val uri = loadable[i % loadable.size]
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(Dimens.ShelfAspectRatio)
                .padding(vertical = 4.dp)
                .shadow(14.dp, shape)
                .clip(shape)
                .background(Color(0xFF14141A)),
        ) {
            val painter = rememberAsyncImagePainter(
                model = ImageRequest.Builder(LocalContext.current).data(uri).size(1920, 1080).build(),
                onState = { state ->
                    val tooSmall = state is AsyncImagePainter.State.Success &&
                        state.result.drawable.intrinsicWidth < Quality.MIN_VISUAL_WIDTH_PX
                    if (state is AsyncImagePainter.State.Error || tooSmall) {
                        Log.w("ShelfPanel", "poster illisible ou trop petit: $uri")
                        failed = failed + uri
                    }
                },
            )
            val state = painter.state
            if (state is AsyncImagePainter.State.Success && state.result.drawable.intrinsicWidth >= Quality.MIN_VISUAL_WIDTH_PX) {
                Image(
                    painter = painter,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = panX
                            scaleX = 1.06f
                            scaleY = 1.06f
                        },
                )
            }
        }
    }
}
