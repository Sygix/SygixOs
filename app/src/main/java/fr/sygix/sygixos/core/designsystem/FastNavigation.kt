/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type

class FastNavigation(private val window: Long = Motion.FOCUS_MS.toLong()) {
    var active: Boolean = false
        private set
    private var lastDirectionDown: Long? = null

    fun record(direction: Boolean, down: Boolean, repeatCount: Int, eventTime: Long) {
        if (!down) return
        if (!direction) {
            active = false
            lastDirectionDown = null
            return
        }
        val previous = lastDirectionDown
        active = repeatCount > 0 || (previous != null && eventTime - previous in 1 until window)
        lastDirectionDown = eventTime
    }

    fun onKeyEvent(event: KeyEvent) {
        record(event.key in Directions, event.type == KeyEventType.KeyDown, event.nativeKeyEvent.repeatCount, event.nativeKeyEvent.eventTime)
    }

    private companion object {
        val Directions = setOf(Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight)
    }
}

val LocalFastNavigation = staticCompositionLocalOf { FastNavigation() }

fun Modifier.fastNavigationKeys(navigation: FastNavigation): Modifier = onPreviewKeyEvent { navigation.onKeyEvent(it); false }

@Composable
fun FastNavigationHost(content: @Composable () -> Unit) {
    val navigation = remember { FastNavigation() }
    CompositionLocalProvider(LocalFastNavigation provides navigation) {
        Box(Modifier.fastNavigationKeys(navigation), propagateMinConstraints = true) { content() }
    }
}

suspend fun Animatable<Float, AnimationVector1D>.focusTo(target: Float, fast: Boolean) {
    if (fast) snapTo(target) else animateTo(target, tween(Motion.FOCUS_MS, easing = AppleEasing))
}

@Composable
fun rememberFocusProgress(focused: Boolean): State<Float> {
    val navigation = LocalFastNavigation.current
    val progress = remember { Animatable(if (focused) 1f else 0f) }
    LaunchedEffect(focused) { progress.focusTo(if (focused) 1f else 0f, navigation.active) }
    return progress.asState()
}
