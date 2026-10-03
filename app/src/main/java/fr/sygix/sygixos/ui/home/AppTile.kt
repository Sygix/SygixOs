/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocus
import fr.sygix.sygixos.data.AppArtwork
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.model.TvApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal val LocalAppArtwork = staticCompositionLocalOf<AppArtworkSource?> { null }

private val TileBackground = Color(0xFF141418)

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
            .testTag("app-tile-" + app.packageName)
            .tvFocus(
                onFocused = onFocusChanged,
                focusRequester = focusRequester,
                enabled = focusEnabled,
            )
            .tvClickable(onClick = onClick, onLongClick = onLongClick),
    ) {
        TileBox(artwork, app.label, lifted = lifted, modifier = Modifier.testTag("app-tile-art-" + app.packageName))
    }
}

@Composable
internal fun rememberAppArtwork(app: TvApp): State<AppArtwork?> {
    val context = LocalContext.current
    val provided = LocalAppArtwork.current
    val source = remember(provided) { provided ?: AppArtworkSource(context.packageManager) }
    return produceState<AppArtwork?>(initialValue = source.cached(app), app.packageName, source) {
        value = source.cached(app) ?: withContext(Dispatchers.IO) { runCatching { source.load(app) }.getOrNull() }
    }
}

@Composable
internal fun TileBox(artwork: AppArtwork?, label: String, lifted: Boolean = false, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Dimens.TileCorner)
    val border = if (lifted) Modifier.border(2.dp, Color.White, shape) else Modifier
    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(16f / 9f)
            .clip(shape)
            .background(TileBackground)
            .then(border),
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
