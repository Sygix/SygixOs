/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.Context
import android.util.Log
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
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
import coil.size.Scale
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.domain.PixelSize
import kotlinx.coroutines.delay

internal const val ShelfPosterZoom = 1.06f
private const val PanStart = -12f
private const val PanEnd = 12f

internal fun posterRequest(context: Context, uri: String, size: PixelSize): ImageRequest =
    ImageRequest.Builder(context).data(uri).size(size.width, size.height).scale(Scale.FILL).build()

@Composable
internal fun ShelfPanel(uris: List<String>, posterSize: PixelSize, modifier: Modifier = Modifier) {
    var failed by remember { mutableStateOf(emptySet<String>()) }
    val loadable = uris.filter { it !in failed }
    var current by remember { mutableStateOf(loadable.firstOrNull()) }
    LaunchedEffect(loadable) {
        if (current !in loadable) current = loadable.firstOrNull()
        while (loadable.size > 1) {
            delay(6000)
            current = loadable[(loadable.indexOf(current) + 1) % loadable.size]
        }
    }
    val shape = RoundedCornerShape(Dimens.ShelfCorner)
    Box(
        modifier
            .testTag("shelf-panel")
            .fillMaxWidth()
            .aspectRatio(Dimens.ShelfAspectRatio)
            .padding(vertical = 4.dp)
            .clip(shape),
    ) {
        Crossfade(
            targetState = current,
            animationSpec = tween(Motion.SHELF_FADE_MS, easing = AppleEasing),
            label = "shelfCrossfade",
        ) { uri ->
            if (uri != null) {
                LoadedPoster(uri, posterSize, onError = {
                    Log.w("ShelfPanel", "poster illisible: $uri")
                    failed = failed + uri
                })
            }
        }
    }
}

@Composable
private fun LoadedPoster(uri: String, size: PixelSize, onError: () -> Unit) {
    val painter = rememberAsyncImagePainter(
        model = posterRequest(LocalContext.current, uri, size),
        onState = { if (it is AsyncImagePainter.State.Error) onError() },
    )
    val ready = painter.state is AsyncImagePainter.State.Success
    val pan = remember(uri) { Animatable(PanStart) }
    LaunchedEffect(ready) {
        if (ready) pan.animateTo(PanEnd, tween(Motion.SHELF_KEN_BURNS_MS, easing = AppleEasing))
    }
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
                    translationX = pan.value
                    scaleX = ShelfPosterZoom
                    scaleY = ShelfPosterZoom
                },
        )
    }
}
