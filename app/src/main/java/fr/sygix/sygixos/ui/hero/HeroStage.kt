package fr.sygix.sygixos.ui.hero

import android.util.Log
import android.view.TextureView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.Image
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.Quality
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.delay

private const val TAG = "HeroStage"

/**
 * Héro plein écran : un seul visuel à la fois (vidéo d'aperçu, sinon poster),
 * muet, avance automatique, fondu croisé et zoom lent façon Apple TV.
 * Gauche/droite : précédent/suivant. OK : ouvre le contenu.
 */
@Composable
fun HeroStage(
    items: List<HeroItem>,
    active: Boolean,
    visible: Boolean,
    focusRequester: FocusRequester,
    onOpen: (HeroItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    var currentId by remember { mutableStateOf<String?>(null) }
    var failedVideos by remember { mutableStateOf(emptySet<String>()) }
    var failedImages by remember { mutableStateOf(emptySet<String>()) }
    val index = items.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val current = items.getOrNull(index)
    val showVideo = current?.videoUrl != null && current.id !in failedVideos

    fun viable(item: HeroItem): Boolean =
        (item.videoUrl != null && item.id !in failedVideos) || (item.imageUrl != null && item.id !in failedImages)

    val step: (Int) -> Unit = { delta ->
        var i = index
        for (attempt in items.indices) {
            i = (i + delta).mod(items.size)
            if (viable(items[i])) {
                currentId = items[i].id
                break
            }
        }
    }
    val stepState = rememberUpdatedState(step)
    val onVideoError: (String) -> Unit = { id ->
        Log.w(TAG, "vidéo illisible: ${items.firstOrNull { it.id == id }?.videoUrl}")
        failedVideos = failedVideos + id
        if (current?.id == id && !viable(current)) stepState.value(1)
    }
    val onVideoErrorState = rememberUpdatedState(onVideoError)
    val onImageError: (HeroItem) -> Unit = { item ->
        Log.w(TAG, "poster illisible ou trop petit: ${item.imageUrl}")
        failedImages = failedImages + item.id
        if (current?.id == item.id && !viable(item)) stepState.value(1)
    }

    val context = LocalContext.current
    val player = remember {
        HeroPlayer(
            context = context.applicationContext,
            onEnded = { stepState.value(1) },
            onError = { onVideoErrorState.value(it) },
        )
    }
    DisposableEffect(player) { onDispose { player.release() } }

    LaunchedEffect(current?.id, showVideo, visible, items.size) {
        val item = current
        if (item?.videoUrl != null && showVideo) {
            player.show(item.id, item.videoUrl, play = visible, loop = items.size == 1)
        } else {
            player.clear()
        }
    }
    LaunchedEffect(current?.id, showVideo) {
        val item = current ?: return@LaunchedEffect
        if (!showVideo) return@LaunchedEffect
        delay(Motion.HERO_VIDEO_START_TIMEOUT_MS)
        if (!player.firstFrameRendered) onVideoErrorState.value(item.id)
    }
    LaunchedEffect(current?.id, showVideo, visible, items.size) {
        if (current == null || showVideo || !visible || items.size < 2) return@LaunchedEffect
        delay(Motion.HERO_DWELL_MS)
        stepState.value(1)
    }

    Box(
        modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusProperties { canFocus = active }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { step(-1); true }
                    Key.DirectionRight -> { step(1); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> { current?.let(onOpen); true }
                    else -> false
                }
            }
            .focusable(),
    ) {
        AmbientGradient()
        if (showVideo) {
            HeroVideoLayer(player, visible = player.firstFrameRendered)
        }
        Crossfade(
            targetState = current?.takeIf { !showVideo && it.imageUrl != null },
            animationSpec = tween(Motion.HERO_CROSSFADE_MS, easing = AppleEasing),
            label = "heroPoster",
        ) { item ->
            item?.imageUrl?.let { url ->
                KenBurnsPoster(url, onError = { onImageError(item) })
            }
        }
        Crossfade(
            targetState = current?.takeIf { it.title.isNotEmpty() },
            animationSpec = tween(Motion.HERO_CROSSFADE_MS, easing = AppleEasing),
            label = "heroMetadata",
        ) { item ->
            if (item != null) HeroMetadata(item)
        }
    }
}

@Composable
private fun HeroVideoLayer(player: HeroPlayer, visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(Motion.HERO_VIDEO_FADE_MS, easing = AppleEasing),
        label = "heroVideoAlpha",
    )
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val containerAspect = maxWidth / maxHeight
        val videoAspect = player.videoAspect
        val cover = if (videoAspect > containerAspect) videoAspect / containerAspect else containerAspect / videoAspect
        val scaleX = if (videoAspect > containerAspect) cover else 1f
        val scaleY = if (videoAspect > containerAspect) 1f else cover
        AndroidView(
            factory = { ctx -> TextureView(ctx).apply { isOpaque = false } },
            update = { player.attach(it) },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                },
        )
    }
}

/** Le poster n'est dessiné qu'une fois chargé ET assez grand : pas de flash d'image floue avant l'écartement. */
@Composable
private fun KenBurnsPoster(url: String, onError: () -> Unit) {
    // Taille explicite : sans dessin préalable, le painter n'aurait jamais de taille et ne chargerait rien.
    val painter = rememberAsyncImagePainter(
        model = ImageRequest.Builder(LocalContext.current).data(url).size(1920, 1080).crossfade(false).build(),
        onState = { state ->
            when {
                state is AsyncImagePainter.State.Error -> onError()
                state is AsyncImagePainter.State.Success &&
                    state.result.drawable.intrinsicWidth < Quality.MIN_VISUAL_WIDTH_PX -> onError()
            }
        },
    )
    val state = painter.state
    val ready = state is AsyncImagePainter.State.Success &&
        state.result.drawable.intrinsicWidth >= Quality.MIN_VISUAL_WIDTH_PX
    var zoomed by remember(url) { mutableStateOf(false) }
    LaunchedEffect(ready) { if (ready) zoomed = true }
    val scale by animateFloatAsState(
        targetValue = if (zoomed) 1.08f else 1f,
        animationSpec = tween(Motion.HERO_KEN_BURNS_MS, easing = LinearEasing),
        label = "kenBurns",
    )
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(Motion.HERO_VIDEO_FADE_MS, easing = AppleEasing), label = "posterAlpha")
    if (ready) {
        Image(
            painter = painter,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.alpha = alpha
                    scaleX = scale
                    scaleY = scale
                },
        )
    }
}

@Composable
private fun HeroMetadata(item: HeroItem) {
    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)
                .align(Alignment.BottomStart)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f)))),
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = Dimens.ScreenMarginH + 8.dp, bottom = 172.dp)
                .widthIn(max = 520.dp),
        ) {
            item.sourceLabel?.let { label ->
                Text(label.uppercase(), style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f))
                Spacer(Modifier.height(6.dp))
            }
            Text(
                item.title,
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            item.progress?.let { progress ->
                Box(
                    Modifier
                        .padding(top = 12.dp)
                        .width(220.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color.White.copy(alpha = 0.3f)),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(Color.White),
                    )
                }
            }
        }
    }
}
