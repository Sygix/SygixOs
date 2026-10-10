/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.animation.core.tween
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalDensity
import androidx.core.graphics.drawable.toBitmap
import coil.compose.AsyncImagePainter
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import coil.size.Precision
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.domain.GridSections
import fr.sygix.sygixos.domain.UpNextArtworkQuality
import fr.sygix.sygixos.domain.UpNextCardText
import fr.sygix.sygixos.domain.UpNextContent
import fr.sygix.sygixos.domain.UpNextScroll
import fr.sygix.sygixos.model.*
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

private val SkeletonBarShape = RoundedCornerShape(percent = 50)
private const val RetryRadius = 9.6f
private const val RetryStroke = 1.5f
private const val RetryStart = 40f
private const val RetrySweep = 280f
private const val RetryArrowAngle = 0.6
private const val RetryArrowLength = 4.5f

@Composable
internal fun UpNextRow(
    content: UpNextContent,
    width: Dp,
    focus: TileFocus,
    entry: String?,
    entryRequester: FocusRequester,
    focusEnabled: Boolean,
    onFocused: (String, Int) -> Unit,
    onOpen: (UpNextItem) -> Unit,
    onMenu: (UpNextItem) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
) {
    val scope = rememberCoroutineScope()
    val cards = (content as? UpNextContent.Items)?.items.orEmpty()
    val enabled by rememberUpdatedState(focusEnabled)
    val targetIndex = cards.indexOfFirst { it.key == entry }
    suspend fun show(index: Int) {
        val layout = state.layoutInfo
        val anchor = layout.visibleItemsInfo.firstOrNull { it.index == index } ?: layout.visibleItemsInfo.firstOrNull()
        if (anchor == null) {
            state.scrollToItem(index)
            return
        }
        val offset = UpNextScroll.offsetOf(index, anchor.index, anchor.offset, anchor.size, layout.mainAxisItemSpacing)
        val overflow = UpNextScroll.focusOverflow(anchor.size, Dimens.TileFocusScale)
        val delta = UpNextScroll.revealDelta(offset, anchor.size, layout.viewportEndOffset, overflow)
        if (delta != 0) state.animateScrollBy(delta.toFloat(), tween(Motion.FOCUS_MS, easing = AppleEasing))
    }
    LaunchedEffect(cards.map { it.key }, focusEnabled, content == UpNextContent.Error) {
        if (!focusEnabled) return@LaunchedEffect
        if (targetIndex >= 0) {
            show(targetIndex)
            withFrameNanos { }
            focus.requesterFor(cards[targetIndex].key).tryRequestFocus()
        } else if (content == UpNextContent.Error && entry == GridSections.ERROR_KEY) {
            withFrameNanos { }
            focus.requesterFor(GridSections.ERROR_KEY).tryRequestFocus()
        }
    }
    LazyRow(
        state = state,
        modifier = modifier.fillMaxWidth().testTag("zone-upnext"),
        contentPadding = PaddingValues(start = Dimens.ScreenMarginH, end = width * ((Dimens.TileFocusScale - 1f) / 2f)),
        horizontalArrangement = Arrangement.spacedBy(Dimens.GridSpacing),
    ) {
        when (content) {
            is UpNextContent.Items -> itemsIndexed(content.items, key = { _, item -> item.key }) { index, item ->
                UpNextCard(
                    item = item,
                    modifier = Modifier.width(width).then(if (item.key == entry) Modifier.focusRequester(entryRequester) else Modifier)
                        .onPreviewKeyEvent { event ->
                            val step = when (event.key) { Key.DirectionLeft -> -1; Key.DirectionRight -> 1; else -> return@onPreviewKeyEvent false }
                            if (event.type == KeyEventType.KeyDown) scope.launch {
                                val next = UpNextScroll.neighbor(index, step, cards.size)
                                val requester = focus.requesterFor(cards[next].key)
                                if (requester.tryRequestFocus()) return@launch
                                val reveal = launch { show(next) }
                                while (!requester.tryRequestFocus()) {
                                    withFrameNanos { }
                                    if (!reveal.isActive) {
                                        withFrameNanos { }
                                        requester.tryRequestFocus()
                                        break
                                    }
                                }
                            }
                            true
                        },
                    focusRequester = focus.requesterFor(item.key),
                    focusEnabled = focusEnabled,
                    onFocused = {
                        if (it && enabled) {
                            onFocused(item.key, index)
                            scope.launch { show(index) }
                        }
                    },
                    onOpen = { onOpen(item) },
                    onMenu = { onMenu(item) },
                )
            }
            UpNextContent.Error -> item(key = GridSections.ERROR_KEY) {
                Box(
                    Modifier.width(width).aspectRatio(16f / 9f)
                        .testTag("upnext-error")
                        .onPreviewKeyEvent { it.key == Key.DirectionLeft || it.key == Key.DirectionRight }
                        .then(if (entry == GridSections.ERROR_KEY) Modifier.focusRequester(entryRequester) else Modifier)
                        .tvFocus(enabled = focusEnabled, focusRequester = focus.requesterFor(GridSections.ERROR_KEY), onFocused = { if (it && enabled) onFocused(GridSections.ERROR_KEY, 0) })
                        .tvClickable(onClick = onRetry)
                        .clip(TileShape).background(SygixColors.TileBackground),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(Dimens.UpNextTextAboveProgress)) {
                        UpNextGlyph(true, SygixColors.OnDark, Modifier.size(Dimens.UpNextErrorIcon))
                        Text(stringResource(R.string.upnext_load_error), style = TextStyles.Row, color = SygixColors.OnDark)
                        Text(stringResource(R.string.upnext_retry), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                    }
                }
            }
            UpNextContent.Skeleton -> items(5, key = { "skeleton-$it" }) {
                Box(Modifier.width(width).aspectRatio(16f / 9f).clip(TileShape).background(SygixColors.SkeletonBase).testTag("upnext-skeleton")) {
                    Column(Modifier.align(Alignment.BottomStart).padding(start = Dimens.UpNextTextInset, bottom = Dimens.UpNextTextBottom), verticalArrangement = Arrangement.spacedBy(Dimens.UpNextTextAboveProgress)) {
                        Box(Modifier.size(Dimens.UpNextSkeletonTitle).clip(SkeletonBarShape).background(SygixColors.SkeletonBar))
                        Box(Modifier.size(Dimens.UpNextSkeletonSubtitle).clip(SkeletonBarShape).background(SygixColors.SkeletonBar))
                    }
                }
            }
            UpNextContent.Pending -> Unit
        }
    }
}

@Composable
internal fun UpNextCard(
    item: UpNextItem,
    modifier: Modifier = Modifier,
    focusEnabled: Boolean = true,
    focusRequester: FocusRequester? = null,
    onFocused: (Boolean) -> Unit = {},
    onOpen: () -> Unit = {},
    onMenu: () -> Unit = {},
) {
    Box(
        modifier.aspectRatio(16f / 9f).testTag("upnext-card-${item.key}")
            .onPreviewKeyEvent {
                if (it.key != Key.Menu) false else { if (it.type == KeyEventType.KeyDown) onMenu(); true }
            }
            .tvFocus(enabled = focusEnabled, focusRequester = focusRequester, onFocused = onFocused)
            .tvClickable(onClick = onOpen, onLongClick = onMenu)
            .clip(TileShape),
    ) {
        UpNextArtwork(item, Modifier.fillMaxSize().testTag("upnext-card-art-${item.key}"))
        Box(Modifier.fillMaxSize().drawWithCache {
            val scrim = Brush.verticalGradient(0f to Color.Transparent, Dimens.CardScrimStart to Color.Transparent, 1f to SygixColors.CardScrimEnd)
            onDrawBehind { drawRect(scrim) }
        })
        item.source.icon?.let { icon ->
            val bitmap = remember(icon) { icon.toBitmap().asImageBitmap() }
            Image(bitmap, null, Modifier.align(Alignment.TopEnd).padding(Dimens.UpNextBadgeInset).size(Dimens.HeroSourceIcon).clip(RoundedCornerShape(Dimens.UpNextBadgeCorner)).testTag("upnext-card-badge-${item.key}"))
        }
        val progress = UpNextCardText.progress(item)
        Column(
            Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(horizontal = Dimens.UpNextTextInset).padding(bottom = Dimens.UpNextTextBottom),
        ) {
            Text(item.displayTitle, style = TextStyles.Row, color = SygixColors.OnDark, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val number = UpNextCardText.episodeNumbers(item)?.let { (season, episode) -> stringResource(R.string.upnext_episode_number, season, episode) }
            UpNextCardText.episodeLine(item, number, stringResource(R.string.hero_details_separator))?.let {
                Text(it, style = TextStyles.RowSecondary, color = SygixColors.OnDarkDetails, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (progress != null) {
                Spacer(Modifier.height(Dimens.UpNextTextAboveProgress))
                Box(Modifier.fillMaxWidth().height(Dimens.ProgressHeight).clip(RoundedCornerShape(Dimens.ProgressHeight / 2)).background(SygixColors.ProgressTrack).testTag("upnext-card-progress-${item.key}")) {
                    Box(Modifier.fillMaxWidth(progress).fillMaxHeight().background(SygixColors.OnDark))
                }
            }
        }
    }
}

@Composable
internal fun UpNextArtwork(item: UpNextItem, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.background(SygixColors.TileBackground), contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val context = LocalContext.current
        val width = with(density) { maxWidth.roundToPx() }
        val height = with(density) { maxHeight.roundToPx() }
        val icon = UpNextArtworkQuality.placeholderIcon(Dimens.UpNextPlaceholderIcon.value, maxHeight.value).dp
        val request = remember(context, item.imageUrl, width, height) {
            ImageRequest.Builder(context).data(item.imageUrl)
                .size((width * 2).coerceAtLeast(1), (height * 2).coerceAtLeast(1))
                .precision(Precision.INEXACT).build()
        }
        val painter = rememberAsyncImagePainter(request, contentScale = ContentScale.Crop)
        val decoded = (painter.state as? AsyncImagePainter.State.Success)?.result?.drawable
        val usable = UpNextArtworkQuality.usable(decoded?.intrinsicWidth ?: 0, width)
        if (usable) Image(painter, contentDescription = null,
            contentScale = if (UpNextArtworkQuality.portrait(decoded!!.intrinsicWidth, decoded.intrinsicHeight)) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier.fillMaxSize())
        if (!usable) Box(Modifier.fillMaxWidth().fillMaxHeight(0.5f).align(Alignment.TopCenter).testTag("upnext-card-placeholder-${item.key}"), contentAlignment = Alignment.Center) {
            UpNextGlyph(false, SygixColors.PlaceholderGlyph, Modifier.size(icon).testTag("upnext-card-placeholder-icon-${item.key}"))
        }
    }
}

@Composable
private fun UpNextGlyph(retry: Boolean, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val unit = size.minDimension / 24f
        val stroke = Stroke(unit * 2)
        if (retry) {
            val radius = unit * RetryRadius
            val center = Offset(unit * 12, unit * 12)
            val width = unit * RetryStroke
            drawArc(color, RetryStart, RetrySweep, false, center - Offset(radius, radius), Size(radius * 2, radius * 2), style = Stroke(width, cap = StrokeCap.Round))
            val end = Math.toRadians((RetryStart + RetrySweep).toDouble())
            val tip = center + Offset((cos(end) * radius).toFloat(), (sin(end) * radius).toFloat())
            val heading = atan2(cos(end), -sin(end))
            for (side in listOf(-1.0, 1.0)) {
                val angle = heading + PI + side * RetryArrowAngle
                drawLine(color, tip, tip + Offset((cos(angle) * unit * RetryArrowLength).toFloat(), (sin(angle) * unit * RetryArrowLength).toFloat()), width, StrokeCap.Round)
            }
        } else {
            drawRect(color, Offset(unit * 3, unit * 3), Size(unit * 18, unit * 18), style = stroke)
            drawCircle(color, unit * 1.5f, Offset(unit * 8, unit * 8))
            val hills = Path().apply { moveTo(unit * 4, unit * 18); lineTo(unit * 10, unit * 12); lineTo(unit * 14, unit * 16); lineTo(unit * 17, unit * 13); lineTo(unit * 20, unit * 17) }
            drawPath(hills, color, style = stroke)
        }
    }
}
