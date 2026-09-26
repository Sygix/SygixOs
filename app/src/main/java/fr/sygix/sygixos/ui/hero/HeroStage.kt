/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.util.Log
import android.view.TextureView
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
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
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import coil.request.ImageRequest
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.model.HeroItem
import kotlinx.coroutines.delay

private const val TAG = "HeroStage"

@Composable
fun HeroStage(
    items: List<HeroItem>,
    validatedVisuals: Set<String>,
    active: Boolean,
    visible: Boolean,
    focusRequester: FocusRequester,
    onOpen: (HeroItem) -> Unit,
    modifier: Modifier = Modifier,
    claimFocus: Boolean = true,
) {
    var currentId by remember { mutableStateOf<String?>(null) }
    var failedVideos by remember { mutableStateOf(emptySet<String>()) }
    var failedImages by remember { mutableStateOf(emptySet<String>()) }
    val index = items.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val current = items.getOrNull(index)
    val showVideo = current?.videoUrl != null && current.id !in failedVideos

    fun viable(item: HeroItem): Boolean =
        (item.videoUrl != null && item.id !in failedVideos) ||
            (item.imageUrl != null && item.imageUrl in validatedVisuals && item.id !in failedImages)

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
        Log.w(TAG, "poster illisible: ${item.imageUrl}")
        failedImages = failedImages + item.id
        if (current?.id == item.id && !viable(item)) stepState.value(1)
    }

    LaunchedEffect(current?.id, validatedVisuals, failedImages, failedVideos, items) {
        if (current != null && !viable(current)) {
            items.firstOrNull(::viable)?.let { currentId = it.id }
        }
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
        if (visible && item?.videoUrl != null && showVideo) {
            player.show(item.id, item.videoUrl, play = true, loop = items.size == 1)
        } else {
            player.clear()
        }
    }
    LaunchedEffect(current?.id, showVideo, visible) {
        val item = current ?: return@LaunchedEffect
        if (!showVideo || !visible) return@LaunchedEffect
        delay(Motion.HERO_VIDEO_START_TIMEOUT_MS)
        if (!player.firstFrameRendered) onVideoErrorState.value(item.id)
    }
    LaunchedEffect(current?.id, showVideo, visible, items.size) {
        if (current == null || showVideo || !visible || items.size < 2) return@LaunchedEffect
        delay(Motion.HERO_DWELL_MS)
        stepState.value(1)
    }

    val launchable = current != null && current.title.isNotEmpty() && (current.launchUri != null || current.sourcePackage != null)
    val hasVisual = current != null && (showVideo || (current.imageUrl != null && current.imageUrl in validatedVisuals))
    LaunchedEffect(launchable, active, claimFocus) {
        if (!active || !claimFocus) return@LaunchedEffect
        withFrameNanos { }
        focusRequester.tryRequestFocus()
    }

    Box(
        modifier
            .fillMaxSize()
            .then(if (launchable) Modifier else Modifier.focusRequester(focusRequester))
            .focusProperties { canFocus = active && !launchable }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { step(-1); true }
                    Key.DirectionRight -> { step(1); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (!launchable) { current?.let(onOpen); true } else false
                    else -> false
                }
            }
            .focusable(),
    ) {
        AmbientGradient(animated = visible && !hasVisual)
        if (showVideo) {
            HeroVideoLayer(player, visible = player.firstFrameRendered)
        }
        Crossfade(
            targetState = current?.takeIf { !showVideo && it.imageUrl != null && it.imageUrl in validatedVisuals },
            animationSpec = tween(Motion.HERO_CROSSFADE_MS, easing = AppleEasing),
            label = "heroPoster",
        ) { item ->
            item?.imageUrl?.let { url ->
                KenBurnsPoster(url, onError = { onImageError(item) })
            }
        }
        HeroOverlay(
            current = current?.takeIf { it.title.isNotEmpty() },
            launchable = launchable,
            buttonFocusRequester = focusRequester,
            focusEnabled = active && launchable,
            onOpen = { current?.let(onOpen) },
        )
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

@Composable
private fun KenBurnsPoster(url: String, onError: () -> Unit) {
    var ready by remember(url) { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (ready) 1.08f else 1f,
        animationSpec = tween(Motion.HERO_KEN_BURNS_MS, easing = LinearEasing),
        label = "kenBurns",
    )
    val alpha by animateFloatAsState(if (ready) 1f else 0f, tween(Motion.HERO_VIDEO_FADE_MS, easing = AppleEasing), label = "posterAlpha")
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current).data(url).size(1920, 1080).crossfade(false).build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        onSuccess = { ready = true },
        onError = { onError() },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                this.alpha = alpha
                scaleX = scale
                scaleY = scale
            },
    )
}

@Composable
private fun HeroOverlay(
    current: HeroItem?,
    launchable: Boolean,
    buttonFocusRequester: FocusRequester,
    focusEnabled: Boolean,
    onOpen: () -> Unit,
) {
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
                .widthIn(max = 560.dp),
        ) {
            Crossfade(
                targetState = current,
                animationSpec = tween(Motion.HERO_CROSSFADE_MS, easing = AppleEasing),
                label = "heroMetadata",
            ) { item ->
                if (item != null) HeroMetadata(item)
            }
            if (launchable && current != null) {
                Spacer(Modifier.height(14.dp))
                HeroOpenButton(
                    label = if (current.progress != null) "Reprendre" else "Ouvrir",
                    focusRequester = buttonFocusRequester,
                    enabled = focusEnabled,
                    onClick = onOpen,
                )
            }
        }
    }
}

@Composable
private fun HeroMetadata(item: HeroItem) {
    Column {
        Text(
            item.sourceLabel?.uppercase() ?: " ",
            style = MaterialTheme.typography.labelMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            item.title,
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            minLines = 2,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Box(
            Modifier
                .padding(top = 12.dp)
                .width(220.dp)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color.White.copy(alpha = if (item.progress != null) 0.3f else 0f)),
        ) {
            item.progress?.let { progress ->
                Box(
                    Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color.White)
                        .testTag("hero-progress"),
                )
            }
        }
    }
}

@Composable
private fun HeroOpenButton(label: String, focusRequester: FocusRequester, enabled: Boolean, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(50)
    val content by animateColorAsState(
        if (focused) Color(0xFF15151A) else Color.White,
        tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "heroButtonContent",
    )
    val scale by animateFloatAsState(if (focused) 1.06f else 1f, tween(Motion.FOCUS_MS, easing = AppleEasing), label = "heroButtonScale")
    val focusModifier = Modifier
        .testTag("hero-open")
        .scale(scale)
        .focusRequester(focusRequester)
        .focusProperties { canFocus = enabled }
        .onFocusChanged { focused = it.isFocused }
        .tvClickable(onClick = onClick)
    val labelRow: @Composable () -> Unit = {
        Row(
            Modifier.padding(start = 18.dp, end = 22.dp, top = 9.dp, bottom = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
                PlayGlyph(content)
            Spacer(Modifier.width(8.dp))
            Text(label, style = MaterialTheme.typography.titleMedium, color = content)
        }
    }
    Box(focusModifier) {
        if (focused) {
            Box(Modifier.clip(pill).background(Color.White)) { labelRow() }
        } else {
            GlassSurface(shape = pill) { labelRow() }
        }
    }
}

@Composable
private fun PlayGlyph(color: Color) {
    Canvas(Modifier.size(14.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
    }
}
