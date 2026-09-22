package fr.sygix.sygixos.ui.home

import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import kotlinx.coroutines.delay

@Composable
internal fun ShelfPanel(uris: List<String>, modifier: Modifier = Modifier) {
    var failed by remember(uris) { mutableStateOf(emptySet<String>()) }
    val loadable = remember(uris, failed) { uris.filter { it !in failed } }
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
    val panX by rememberInfiniteTransition(label = "kenBurns").animateFloat(
        initialValue = -12f,
        targetValue = 12f,
        animationSpec = infiniteRepeatable(tween(16000, easing = AppleEasing), RepeatMode.Reverse),
        label = "kenBurnsX",
    )
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier
            .testTag("shelf-panel")
            .fillMaxWidth()
            .aspectRatio(Dimens.ShelfAspectRatio)
            .padding(vertical = 4.dp)
            .clip(shape),
    ) {
        Crossfade(
            targetState = loadable.getOrNull(index % maxOf(loadable.size, 1)),
            animationSpec = tween(Motion.SHELF_FADE_MS, easing = AppleEasing),
            label = "shelfCrossfade",
        ) { uri ->
            if (uri != null) {
                LoadedPoster(uri, panX, onError = {
                    Log.w("ShelfPanel", "poster illisible: $uri")
                    failed = failed + uri
                })
            }
        }
    }
}

@Composable
private fun LoadedPoster(uri: String, panX: Float, onError: () -> Unit) {
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current).data(uri).size(1920, 1080).build(),
        onState = { if (it is AsyncImagePainter.State.Error) onError() },
    )
    val ready = painter.state is AsyncImagePainter.State.Success
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(Motion.SHELF_FADE_MS, easing = AppleEasing), label = "posterAlpha")
    if (ready) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    translationX = panX
                    scaleX = 1.06f
                    scaleY = 1.06f
                },
        )
    }
}
