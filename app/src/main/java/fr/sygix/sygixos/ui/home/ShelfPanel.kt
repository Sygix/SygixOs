package fr.sygix.sygixos.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import kotlinx.coroutines.delay

/** Panneau Top Shelf inséré sous la rangée focusée (posters de l'app focus). */
@Composable
internal fun ShelfPanel(uris: List<String>) {
    if (uris.isEmpty()) return
    var index by remember { mutableStateOf(0) }
    LaunchedEffect(uris) {
        if (uris.size > 1) {
            while (true) {
                delay(6000)
                index = (index + 1) % uris.size
            }
        }
    }
    val pan = rememberInfiniteTransition(label = "kenBurns")
    val panX by pan.animateFloat(
        initialValue = -24f,
        targetValue = 24f,
        animationSpec = infiniteRepeatable(tween(16000, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "kenBurnsX",
    )
    val panelHeight = (LocalConfiguration.current.screenHeightDp * 0.45f).dp
    Crossfade(
        targetState = index,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "shelfCrossfade",
    ) { i ->
        Box(
            Modifier
                .fillMaxWidth()
                .height(panelHeight)
                .padding(vertical = 4.dp)
                .shadow(18.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF14141A)),
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uris[i % uris.size])
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = panX
                        scaleX = 1.08f
                        scaleY = 1.08f
                    },
            )
        }
    }
}
