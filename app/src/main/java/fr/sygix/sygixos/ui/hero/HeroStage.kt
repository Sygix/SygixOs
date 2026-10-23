/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import android.util.Log
import android.view.TextureView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.glassRim
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
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
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.model.HeroItem
import androidx.compose.foundation.Image
import androidx.compose.runtime.produceState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import fr.sygix.sygixos.data.AppIconCache
import fr.sygix.sygixos.domain.HeroCaption
import fr.sygix.sygixos.domain.HeroDetails
import fr.sygix.sygixos.domain.HeroHeader
import fr.sygix.sygixos.model.ProgramKind
import fr.sygix.sygixos.ui.LocalBackgroundDispatcher
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.async
import kotlinx.coroutines.Deferred
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle

import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.runtime.key
import fr.sygix.sygixos.core.designsystem.GlassBackdrop
import fr.sygix.sygixos.core.designsystem.glassBackdropOrigin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val TAG = "HeroStage"

internal val LocalSourceIcons = staticCompositionLocalOf<AppIconCache?> { null }

internal val LocalHeroVideoFactory = staticCompositionLocalOf { SystemHeroVideo }

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
    onVisualReady: () -> Unit = {},
    motion: Boolean = true,
    backdrop: GlassBackdrop = remember { GlassBackdrop() },
    stillActive: () -> Boolean = { true },
    keepVideo: Boolean = false,
    onDisplayed: (String?) -> Unit = {},
    generations: Map<String, Long> = emptyMap(),
) {
    var currentId by remember { mutableStateOf<String?>(null) }
    var failedVideos by remember { mutableStateOf(emptySet<String>()) }
    var natureIndex by remember { mutableStateOf(0) }
    var slowVisuals by remember { mutableStateOf(emptySet<String>()) }
    val lastIndex = remember { IntArray(1) }
    val index = items.indexOfFirst { it.id == currentId }.takeIf { it >= 0 }
        ?: lastIndex[0].coerceAtMost(items.lastIndex).coerceAtLeast(0)
    lastIndex[0] = index
    val current = items.getOrNull(index)
    val generation = current?.id?.let { generations[it] } ?: 0L
    val imageEvent = fr.sygix.sygixos.ui.home.artworkEvent(current?.artwork)
    val imageVersion = (imageEvent as? fr.sygix.sygixos.data.ArtworkEvent.ImageReady)?.version.orEmpty()
    var failedImages by remember(current?.artwork?.identity, imageVersion) { mutableStateOf(emptySet<String>()) }
    val programVideo = current?.videoUrl != null && current.id !in failedVideos
    val imageCandidate = current?.artwork?.candidates?.firstOrNull()
    val fallback = current != null && !programVideo && (current.artworkFailed || imageCandidate == null || imageCandidate in failedImages)
    val natureUrl = fr.sygix.sygixos.data.NatureFallbackProvider.CLIPS.getOrNull(natureIndex)
    val mediaId = if (fallback) "nature-$natureIndex" else current?.id
    val mediaUrl = if (fallback) natureUrl else current?.videoUrl
    val showVideo = programVideo || fallback && natureUrl != null

    fun viable(item: HeroItem): Boolean =
        item.id !in slowVisuals

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
        val uri = (if (fallback) natureUrl else current?.videoUrl)?.let(android.net.Uri::parse)
        Log.w(TAG, "app=${current?.sourcePackage ?: "nature"} scheme=${uri?.scheme} authority=${uri?.let { fr.sygix.sygixos.data.diagnosticAuthority(it) }} cause=video-unavailable")
        if (fallback && id.startsWith("nature-")) natureIndex += 1 else failedVideos = failedVideos + id
    }
    val onVideoErrorState = rememberUpdatedState(onVideoError)
    val onVideoEndedState = rememberUpdatedState<() -> Unit>({
        if (fallback) natureIndex = (natureIndex + 1) % fr.sygix.sygixos.data.NatureFallbackProvider.CLIPS.size else stepState.value(1)
    })
    val onImageError: (String) -> Unit = { url ->
        failedImages = failedImages + url
    }

    LaunchedEffect(current?.id, validatedVisuals, failedImages, failedVideos, items) {
        if (current != null && !viable(current)) {
            items.firstOrNull(::viable)?.let { currentId = it.id }
        }
    }

    val context = LocalContext.current
    val providedIcons = LocalSourceIcons.current
    val icons = remember(providedIcons) { providedIcons ?: AppIconCache.squareFor(context.packageManager) }
    val videoFactory = LocalHeroVideoFactory.current
    val player = remember(videoFactory) {
        videoFactory.create(
            context.applicationContext,
            { onVideoEndedState.value() },
            { onVideoErrorState.value(it) },
        )
    }
    LifecycleStartEffect(player) { onStopOrDispose { player.release() } }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val onVisualReadyState = rememberUpdatedState(onVisualReady)
    LaunchedEffect(player) {
        snapshotFlow { player.firstFrameRendered }.filter { it }.collect { onVisualReadyState.value() }
    }
    LaunchedEffect(player) {
        snapshotFlow { player.readyItem }.collect { id -> if (id != null) slowVisuals = slowVisuals - id }
    }
    LaunchedEffect(player) {
        var loaded: String? = null
        snapshotFlow { player.loadedItem }.collect { id ->
            loaded?.let { slowVisuals = slowVisuals - it }
            loaded = id
        }
    }

    LifecycleStartEffect(mediaId, showVideo, visible, motion, items.size, keepVideo) {
        when {
            visible && mediaId != null && mediaUrl != null && showVideo -> player.show(mediaId, mediaUrl, play = motion, loop = !fallback && items.size == 1)
            visible && player.loadedItem in slowVisuals -> player.park()
            keepVideo && showVideo && player.loadedItem == mediaId -> player.park()
            else -> player.clear()
        }
        onStopOrDispose { }
    }
    LaunchedEffect(mediaId, showVideo, visible) {
        if (current == null) return@LaunchedEffect
        if (!showVideo || !visible) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delay(Motion.HERO_VIDEO_START_TIMEOUT_MS)
            if (!player.firstFrameRendered) mediaId?.let { onVideoErrorState.value(it) }
        }
    }
    LaunchedEffect(current?.id, showVideo, fallback, visible, items.size) {
        if (current == null || showVideo && !fallback || !visible || items.size < 2) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delay(Motion.HERO_DWELL_MS)
            stepState.value(1)
        }
    }

    val posterUrl = imageCandidate?.takeIf { !showVideo && it !in failedImages }
    var shownId by remember { mutableStateOf<String?>(null) }
    val targetId = current?.artwork?.identity
    val currentPoster by rememberUpdatedState(targetId to posterUrl)
    LaunchedEffect(targetId, mediaId, showVideo, posterUrl, fallback) {
        when {
            targetId == null -> shownId = null
            fallback -> shownId = targetId
            showVideo -> {
                snapshotFlow { player.renderedItem }.first { it == mediaId }
                shownId = targetId
            }
            posterUrl == null -> shownId = targetId
        }
    }
    LaunchedEffect(targetId, posterUrl, showVideo, visible) {
        if (!visible) return@LaunchedEffect
        val id = current?.id ?: return@LaunchedEffect
        if (shownId == targetId) return@LaunchedEffect
        if (!programVideo || !(showVideo && shownId != null)) return@LaunchedEffect
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            delay(Motion.HERO_VISUAL_TIMEOUT_MS)
            if (shownId != targetId) {
                slowVisuals = slowVisuals + id
                stepState.value(1)
            }
        }
    }
    val shown = items.firstOrNull { it.artwork.identity == shownId }
    LaunchedEffect(shown?.id) { onDisplayed(shown?.id) }
    var buttonFocused by remember { mutableStateOf(false) }
    val launchable = shown != null && shown.title.isNotEmpty() && (shown.launchUri != null || shown.sourcePackage != null || shown.tracking != null)
    val hasVisual = showVideo && player.firstFrameRendered || posterUrl != null
    LaunchedEffect(launchable, active, claimFocus) {
        if (!active || !claimFocus) return@LaunchedEffect
        withFrameNanos { }
        if (stillActive()) focusRequester.tryRequestFocus()
    }

    Box(
        modifier
            .fillMaxSize()
            .glassBackdropOrigin(backdrop)
            .then(if (launchable) Modifier else Modifier.focusRequester(focusRequester))
            .focusProperties { canFocus = active && !(launchable && buttonFocused) }
            .onKeyEvent { e ->
                if (e.type != KeyEventType.KeyDown) return@onKeyEvent false
                when (e.key) {
                    Key.DirectionLeft -> { step(-1); true }
                    Key.DirectionRight -> { step(1); true }
                    Key.DirectionCenter, Key.Enter, Key.NumPadEnter -> if (!launchable) { shown?.let(onOpen); true } else false
                    else -> false
                }
            }
            .focusable(),
    ) {
        AmbientGradient(
            animated = visible && !hasVisual && !showVideo && motion,
            deferred = !motion,
            covered = { backdrop.covered },
        )
        if (fallback) Spacer(Modifier.fillMaxSize().testTag(if (showVideo && player.firstFrameRendered) "hero-fallback-nature" else "hero-fallback-neutral"))
        if (showVideo) {
            HeroVideoLayer(player, visible = player.firstFrameRendered)
        }
        val density = LocalDensity.current
        val veils = remember(density) { with(density) { HeroVeils(CornerVeilWidth.toPx(), CornerVeilHeight.toPx()) } }
        DisposableEffect(backdrop, veils) {
            backdrop.overlay = { area -> drawIntoCanvas { veils.drawCorner(it.nativeCanvas, area.width, area.height) } }
            onDispose { backdrop.overlay = null }
        }
        HeroVeilLayer(veils, covered = { backdrop.covered })
        HeroPosterLayers(
            backdrop = backdrop,
            url = posterUrl,
            artwork = current?.artwork,
            version = imageVersion,
            generation = generation,
            running = visible && motion,
            animated = motion,
            onReady = { url ->
                onVisualReadyState.value()
                val (id, poster) = currentPoster
                if (url == poster) shownId = id
            },
            onError = onImageError,
            onLoaded = { url -> slowVisuals = slowVisuals - items.filter { it.imageUrl == url }.map { it.id }.toSet() },
        )
        Spacer(
            Modifier
                .fillMaxSize()
                .drawBehind { drawIntoCanvas { veils.drawCorner(it.nativeCanvas, size.width, size.height) } },
        )
        HeroOverlay(
            current = shown?.takeIf { it.title.isNotEmpty() },
            icons = icons,
            launchable = launchable,
            buttonFocusRequester = focusRequester,
            focusEnabled = active && launchable,
            onOpen = { shown?.let(onOpen) },
            onButtonFocused = { buttonFocused = it },
        )
    }
}

@Composable
private fun HeroVideoLayer(player: HeroVideo, visible: Boolean) {
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(Motion.HERO_VIDEO_FADE_MS, easing = AppleEasing),
        label = "heroVideoAlpha",
    )
    AndroidView(
        factory = { ctx -> TextureView(ctx).apply { isOpaque = false } },
        update = { player.attach(it) },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                val containerAspect = size.width / size.height
                val videoAspect = player.videoAspect
                val cover = if (videoAspect > containerAspect) videoAspect / containerAspect else containerAspect / videoAspect
                this.alpha = alpha
                scaleX = if (videoAspect > containerAspect) cover else 1f
                scaleY = if (videoAspect > containerAspect) 1f else cover
            },
    )
}

@Composable
private fun HeroVeilLayer(veils: HeroVeils, covered: () -> Boolean) {
    Spacer(
        Modifier
            .fillMaxSize()
            .drawBehind {
                if (!covered()) drawIntoCanvas { veils.draw(it.nativeCanvas, size.width, size.height) }
            },
    )
}

@Composable
private fun HeroPosterLayers(
    backdrop: GlassBackdrop,
    url: String?,
    running: Boolean,
    onReady: (String) -> Unit,
    onError: (String) -> Unit,
    animated: Boolean = true,
    onLoaded: (String) -> Unit = {},
    artwork: fr.sygix.sygixos.model.ProgramArtwork? = null,
    version: String = "",
    generation: Long = 0,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val density = LocalDensity.current
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnError by rememberUpdatedState(onError)
    val fades by rememberUpdatedState(animated)
    val currentOnLoaded by rememberUpdatedState(onLoaded)
    val loadScope = rememberCoroutineScope()
    val loads = remember { LinkedHashMap<String, Deferred<HeroPosterImages?>>() }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val spec = remember(constraints.maxWidth, constraints.maxHeight, density) {
            with(density) { PosterSpec.of(constraints.maxWidth, constraints.maxHeight, CornerVeilWidth.toPx(), CornerVeilHeight.toPx()) }
        }
        LaunchedEffect(url, spec, artwork?.identity, version) {
            lifecycle.currentStateFlow.first { it.isAtLeast(Lifecycle.State.STARTED) }
            if (url == null) {
                coroutineScope {
                    backdrop.layers.toList().forEach { layer ->
                        launch { layer.fade.animateTo(0f, tween(Motion.HERO_VIDEO_FADE_MS, easing = AppleEasing)) }
                    }
                }
                backdrop.clear()
                return@LaunchedEffect
            }
            val loadKey = url + ":" + (artwork?.let { fr.sygix.sygixos.data.artworkHash(it.packageName + it.identity) } ?: "") + ":" + version
            val first = backdrop.layers.none { it.image != null && it.fade.value > 0f && it.key != loadKey }
            val layer = backdrop.layer(loadKey)
            if (layer.image == null) {
                val pending = loads.getOrPut(loadKey) {
                    loadScope.async { HeroPosterLoader.load(context, url, spec, artwork) }.also { load ->
                        load.invokeOnCompletion { cause ->
                            if (loads[loadKey] === load) loads.remove(loadKey)
                            if (cause == null) currentOnLoaded(url)
                        }
                    }
                }
                while (loads.size > MaxPendingPosters) {
                    val oldest = loads.keys.first()
                    loads.remove(oldest)?.cancel()
                }
                val images = pending.await()
                if (images == null) {
                    backdrop.layers.remove(layer)
                    currentOnError(url)
                    return@LaunchedEffect
                }
                layer.image = images.poster
                layer.blurred = images.backdrop
            }
            currentOnReady(url)
            val duration = if (first) Motion.HERO_VIDEO_FADE_MS else Motion.HERO_CROSSFADE_MS
            if (!fades) {
                layer.fade.snapTo(1f)
                backdrop.above(layer).forEach { it.fade.snapTo(0f) }
            } else coroutineScope {
                launch {
                    layer.fade.animateTo(1f, tween((duration * (1f - layer.fade.value)).toInt(), easing = AppleEasing))
                }
                backdrop.above(layer).forEach { upper ->
                    launch { upper.fade.animateTo(0f, tween((duration * upper.fade.value).toInt(), easing = AppleEasing)) }
                }
            }
            backdrop.keepOnly(layer)
        }
        backdrop.layers.forEach { layer ->
            key(layer.key) {
                val ready = layer.image != null
                LaunchedEffect(ready, running) {
                    if (!ready || !running) return@LaunchedEffect
                    val remaining = (KenBurnsScale - layer.zoom.value) / (KenBurnsScale - 1f)
                    layer.zoom.animateTo(KenBurnsScale, tween((Motion.HERO_KEN_BURNS_MS * remaining).toInt(), easing = LinearEasing))
                }
            }
        }
        if (backdrop.layers.any { it.image != null }) {
            Spacer(
                Modifier
                    .testTag("hero-poster")
                    .fillMaxSize()
                    .drawBehind {
                        val target = IntSize(size.width.roundToInt(), size.height.roundToInt())
                        backdrop.layers.forEach { layer ->
                            val image = layer.image ?: return@forEach
                            val alpha = layer.fade.value
                            if (alpha <= 0f) return@forEach
                            val zoom = layer.zoom.value
                            scale(zoom, zoom, pivot = center) {
                                drawImage(image, dstSize = target, alpha = alpha)
                            }
                        }
                    },
            )
        }
    }
}

@Composable
private fun HeroOverlay(
    current: HeroItem?,
    icons: AppIconCache,
    launchable: Boolean,
    buttonFocusRequester: FocusRequester,
    focusEnabled: Boolean,
    onOpen: () -> Unit,
    onButtonFocused: (Boolean) -> Unit,
) {
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(start = Dimens.ScreenMarginH, bottom = Dimens.HeroTextBottom)
                .widthIn(max = Dimens.HeroTextWidth),
            verticalArrangement = Arrangement.spacedBy(Dimens.HeroTextSpacing),
        ) {
            SequentialFade(target = current, durationMillis = Motion.HERO_CROSSFADE_MS, identity = { it.artwork.identity }) { item ->
                HeroMetadata(item, icons)
            }
            if (launchable && current != null) {
                HeroOpenButton(
                    label = if (current.progress != null) "Reprendre" else "Ouvrir",
                    focusRequester = buttonFocusRequester,
                    enabled = focusEnabled,
                    onClick = onOpen,
                    onFocused = onButtonFocused,
                    modifier = Modifier.padding(top = ButtonExtraGap),
                )
            }
        }
    }
}

@Composable
private fun HeroMetadata(item: HeroItem, icons: AppIconCache) {
    Column(
        Modifier.testTag("hero-metadata"),
        verticalArrangement = Arrangement.spacedBy(Dimens.HeroTextSpacing),
    ) {
        HeroHeaderRow(item, icons)
        Text(
            item.title,
            style = TextStyles.HeroTitle.copy(shadow = TitleShadow),
            color = SygixColors.OnDark,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        HeroCaption.details(item)?.let { details ->
            Text(
                detailsText(details),
                style = TextStyles.HeroDetails.copy(shadow = SmallTextShadow),
                color = SygixColors.OnDarkDetails,
                maxLines = 1,
                modifier = Modifier.testTag("hero-details"),
            )
        }
        item.progress?.let { progress ->
            Row(
                Modifier.testTag("hero-progress"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(ProgressGap),
            ) {
                Box(
                    Modifier
                        .width(ProgressWidth)
                        .height(Dimens.ProgressHeight)
                        .clip(RoundedCornerShape(Dimens.ProgressHeight / 2))
                        .background(SygixColors.ProgressTrack)
                        .testTag("hero-progress-bar"),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .fillMaxHeight()
                            .background(SygixColors.OnDark),
                    )
                }
                HeroCaption.remainingMinutes(item)?.let { minutes ->
                    Text(
                        stringResource(R.string.hero_remaining, durationText(minutes)),
                        style = TextStyles.HeroRemaining.copy(shadow = SmallTextShadow),
                        color = SourceColor,
                        maxLines = 1,
                        modifier = Modifier.testTag("hero-remaining"),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHeaderRow(item: HeroItem, icons: AppIconCache) {
    val header = HeroCaption.header(item)
    val pkg = item.sourcePackage
    val background = LocalBackgroundDispatcher.current
    val icon by produceState(initialValue = pkg?.let(icons::cached), pkg, icons) {
        if (value == null && pkg != null) value = withContext(background) { icons.get(pkg) }
    }
    val label = header?.let { headerText(it) }
    val bitmap = icon
    if (label == null && bitmap == null) return
    Row(
        Modifier
            .testTag("hero-header")
            .heightIn(min = Dimens.HeroSourceIcon),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HeaderGap),
    ) {
        if (bitmap != null) {
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(
                bitmap = image,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(Dimens.HeroSourceIcon)
                    .clip(SourceIconShape)
                    .testTag("hero-source-icon"),
            )
        }
        if (label != null) {
            Text(
                label,
                style = TextStyles.HeroSource.copy(shadow = SmallTextShadow),
                color = SourceColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.testTag("hero-header-label"),
            )
        }
    }
}

@Composable
private fun headerText(header: HeroHeader): String? {
    val app = header.app
    return when (header.kind) {
        ProgramKind.CONTINUE -> if (app != null) stringResource(R.string.hero_header_continue, app) else stringResource(R.string.hero_kind_continue)
        ProgramKind.NEXT -> if (app != null) stringResource(R.string.hero_header_next, app) else stringResource(R.string.hero_kind_next)
        ProgramKind.NEW -> if (app != null) stringResource(R.string.hero_header_new, app) else stringResource(R.string.hero_kind_new)
        ProgramKind.WATCHLIST -> if (app != null) stringResource(R.string.hero_header_watchlist, app) else stringResource(R.string.hero_kind_watchlist)
        ProgramKind.FEATURED, ProgramKind.WATCH_NEXT -> app
    }
}

@Composable
private fun detailsText(details: HeroDetails): String {
    val parts = listOfNotNull(
        details.season?.let { stringResource(R.string.hero_season, it) },
        details.episode?.let { stringResource(R.string.hero_episode, it) },
        details.minutes?.let { durationText(it) },
    )
    return parts.joinToString(stringResource(R.string.hero_details_separator))
}

@Composable
private fun durationText(minutes: Int): String {
    val split = HeroCaption.split(minutes)
    return when {
        split.hours == 0 -> stringResource(R.string.hero_duration_minutes, split.minutes)
        split.minutes == 0 -> stringResource(R.string.hero_duration_hours, split.hours)
        else -> stringResource(R.string.hero_duration_hours_minutes, split.hours, split.minutes)
    }
}

@Composable
private fun HeroOpenButton(
    label: String,
    focusRequester: FocusRequester,
    enabled: Boolean,
    onClick: () -> Unit,
    onFocused: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val pill = RoundedCornerShape(percent = 50)
    val progress = animateFloatAsState(
        if (focused) 1f else 0f,
        tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "heroButtonFocus",
    )
    val content by animateColorAsState(
        if (focused) SygixColors.OnPill else SygixColors.OnDark,
        tween(Motion.FOCUS_MS, easing = AppleEasing),
        label = "heroButtonContent",
    )
    Box(
        modifier
            .testTag("hero-open")
            .graphicsLayer {
                val p = progress.value
                val scale = 1f + (Dimens.ButtonFocusScale - 1f) * p
                scaleX = scale
                scaleY = scale
                shadowElevation = Dimens.ButtonFocusElevation.toPx() * p
                shape = pill
                clip = false
            }
            .focusRequester(focusRequester)
            .focusProperties { canFocus = enabled }
            .onFocusChanged {
                focused = it.isFocused
                onFocused(it.isFocused)
            }
            .tvClickable(onClick = onClick)
            .glassRim(pill) { progress.value }
            .height(Dimens.ButtonHeight)
            .padding(start = ButtonPaddingStart, end = ButtonPaddingEnd),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            PlayGlyph(content)
            Spacer(Modifier.width(ButtonGap))
            Text(label, style = TextStyles.Button, color = content, maxLines = 1)
        }
    }
}

@Composable
private fun PlayGlyph(color: Color) {
    Canvas(Modifier.size(PlayGlyphSize)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, color)
    }
}

private const val KenBurnsScale = 1.08f
private const val MaxPendingPosters = 4
private val CornerVeilWidth = 320.dp
private val CornerVeilHeight = 150.dp
private val ProgressWidth = 160.dp
private val ProgressGap = 8.dp
private val HeaderGap = 6.dp
private val SourceIconShape = RoundedCornerShape(Dimens.HeroSourceIconCorner)
private val SourceColor = Color.White.copy(alpha = 0.86f)
private val ButtonPaddingStart = 16.dp
private val ButtonPaddingEnd = 20.dp
private val ButtonGap = 7.dp
private val PlayGlyphSize = 13.dp
private val ButtonExtraGap = 3.dp
private val TitleShadow = Shadow(Color.Black.copy(alpha = 0.45f), Offset(0f, 2f), 12f)
private val SmallTextShadow = Shadow(Color.Black.copy(alpha = 0.5f), Offset(0f, 1f), 3f)
