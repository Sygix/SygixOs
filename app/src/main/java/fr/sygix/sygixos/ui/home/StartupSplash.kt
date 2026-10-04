/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.animation.core.Animatable as FloatAnimatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.viewinterop.AndroidView
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.AppleEasing
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.Motion
import fr.sygix.sygixos.data.MascotAnimationSource
import fr.sygix.sygixos.domain.StartupPhase
import kotlinx.coroutines.delay

internal val StartupHomeAlpha = SemanticsPropertyKey<Float>("StartupHomeAlpha")
private var SemanticsPropertyReceiver.startupHomeAlpha by StartupHomeAlpha

private fun Modifier.startupOpacity(alpha: Float): Modifier =
    graphicsLayer { this.alpha = alpha }.semantics { startupHomeAlpha = alpha }

@Composable
internal fun StartupHost(
    state: HomeState,
    phase: StartupPhase,
    animated: Boolean,
    mascot: MascotAnimationSource,
    onSplashShown: () -> Unit,
    onFadeFinished: () -> Unit,
    onMascotShown: () -> Unit = {},
    onMascotUnavailable: () -> Unit = {},
    windowFocused: Boolean = true,
    home: @Composable (HomeState.Ready, interactive: Boolean) -> Unit,
) {
    val interactive = phase != StartupPhase.Splash
    val fadingOut = phase == StartupPhase.FadingOut
    var mascotSettled by remember { mutableStateOf(false) }
    var focusWaitOver by remember { mutableStateOf(false) }
    LaunchedEffect(mascotSettled) {
        if (!mascotSettled) return@LaunchedEffect
        delay(Motion.SPLASH_FOCUS_WAIT_MS)
        focusWaitOver = true
    }
    val homeComposed = interactive || (mascotSettled && (windowFocused || focusWaitOver))
    val splashAlpha = remember { FloatAnimatable(1f) }
    val currentOnFadeFinished by rememberUpdatedState(onFadeFinished)
    LaunchedEffect(fadingOut) {
        if (!fadingOut) return@LaunchedEffect
        splashAlpha.animateTo(0f, tween(Motion.SPLASH_FADE_MS, easing = AppleEasing))
        currentOnFadeFinished()
    }
    Box(Modifier.fillMaxSize()) {
        Box(Modifier.fillMaxSize().startupOpacity(if (interactive) 1f else 0f)) {
            when (state) {
                HomeState.Loading -> Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                is HomeState.Ready -> if (homeComposed) home(state, interactive)
            }
        }
        if (phase != StartupPhase.Done) {
            StartupSplash(
                animated = animated,
                mascot = mascot,
                onShown = onSplashShown,
                onMascotShown = onMascotShown,
                onMascotUnavailable = onMascotUnavailable,
                onSettled = { mascotSettled = true },
                modifier = Modifier.graphicsLayer {
                    alpha = splashAlpha.value
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                },
            )
        }
    }
}

@Composable
internal fun StartupSplash(
    animated: Boolean,
    mascot: MascotAnimationSource,
    onShown: () -> Unit,
    modifier: Modifier = Modifier,
    onMascotShown: () -> Unit = {},
    onMascotUnavailable: () -> Unit = {},
    onSettled: () -> Unit = {},
) {
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnUnavailable by rememberUpdatedState(onMascotUnavailable)
    val currentOnSettled by rememberUpdatedState(onSettled)
    val drawable by produceState<Drawable?>(null, mascot) {
        val loaded = mascot.load().getOrNull()
        if (loaded == null) {
            currentOnUnavailable()
            currentOnSettled()
        }
        value = loaded
    }
    LaunchedEffect(Unit) {
        withFrameNanos { }
        currentOnShown()
    }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag("startup-splash"),
        contentAlignment = Alignment.Center,
    ) {
        drawable?.let { MascotView(it, animated, onMascotShown, onSettled) }
    }
}

@Composable
private fun MascotView(drawable: Drawable, animated: Boolean, onShown: () -> Unit, onSettled: () -> Unit) {
    val description = stringResource(R.string.startup_mascot_description)
    val appear = remember { FloatAnimatable(if (animated) 0f else 1f) }
    val currentOnShown by rememberUpdatedState(onShown)
    val currentOnSettled by rememberUpdatedState(onSettled)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        currentOnShown()
        appear.animateTo(1f, tween(Motion.SPLASH_APPEAR_MS, easing = AppleEasing))
        currentOnSettled()
    }
    DisposableEffect(drawable, animated) {
        val animation = drawable as? Animatable
        if (animated) animation?.start()
        onDispose { animation?.stop() }
    }
    AndroidView(
        factory = { ImageView(it) },
        update = { it.setImageDrawable(drawable) },
        onRelease = { it.setImageDrawable(null) },
        modifier = Modifier
            .size(Dimens.SplashMascot)
            .graphicsLayer { alpha = appear.value }
            .testTag("startup-splash-mascot")
            .semantics { contentDescription = description },
    )
}
