package fr.sygix.sygixos.ui.hero

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.zIndex
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HeroCarousel(
    items: List<HeroItem>,
    onFocused: (Boolean) -> Unit,
    onItemClick: (HeroItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (items.isEmpty()) return
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var focusedIndex by remember { mutableStateOf(0) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val pageWidth = maxWidth * 0.82f
        val edgePadding = (maxWidth - pageWidth) / 2

        LazyRow(
            state = listState,
            contentPadding = PaddingValues(horizontal = edgePadding),
            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(items, key = { it.id }) { item ->
                val isFocused = focusedIndex == items.indexOf(item)
                HeroPage(
                    item = item,
                    width = pageWidth,
                    isFocused = isFocused,
                    onFocus = {
                        focusedIndex = items.indexOf(item)
                        onFocused(true)
                    },
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun HeroPage(
    item: HeroItem,
    width: androidx.compose.ui.unit.Dp,
    isFocused: Boolean,
    onFocus: () -> Unit,
    onClick: () -> Unit,
) {
    val scale by animateFloatAsState(if (isFocused) 1f else 0.96f, tween(300), label = "pageScale")
    Box(
        modifier = Modifier
            .width(width)
            .fillMaxHeight()
            .padding(horizontal = 8.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0xFF0A0A0C))
            .scale(scale)
            .onFocusChanged { if (it.isFocused) onFocus() }
            .focusable()
            .clickable(onClick = onClick),
        contentAlignment = Alignment.BottomStart,
    ) {
        when {
            item.videoUrl != null -> AerialVideo(item.videoUrl)
            item.imageUrl != null -> CoilPoster(item.imageUrl)
            else -> GradientFallback()
        }
        if (item.title.isNotEmpty() || item.progress != null) {
            androidx.compose.foundation.layout.Column(
                modifier = Modifier
                    .padding(40.dp)
                    .zIndex(1f),
            ) {
                if (item.title.isNotEmpty()) {
                    Text(
                        item.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                    )
                }
                item.progress?.let { progress ->
                    Box(
                        Modifier
                            .padding(top = 10.dp)
                            .width(width * 0.35f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(Color.White.copy(alpha = 0.25f)),
                    ) {
                        Box(
                            Modifier
                                .fillMaxWidth(progress)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color.White),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AerialVideo(url: String) {
    val context = LocalContext.current
    val player = remember(url) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(url))
            repeatMode = Player.REPEAT_MODE_ALL
            volume = 0f
            prepare()
            playWhenReady = true
        }
    }
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                this.player = player
            }
        },
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
private fun CoilPoster(url: String) {
    coil.compose.AsyncImage(
        model = url,
        contentDescription = null,
        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
        modifier = Modifier.fillMaxSize(),
    )
}

@Composable
fun GradientFallback(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF0E1A33), Color(0xFF121212), Color(0xFF1B1030)),
                ),
            ),
    )
}
