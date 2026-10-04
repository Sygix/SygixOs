/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import fr.sygix.sygixos.core.designsystem.AppleEasing
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private class FadeEntry<T>(val value: T, initial: Float) {
    val alpha = Animatable(initial)
}

@Composable
internal fun <T : Any> SequentialFade(
    target: T?,
    durationMillis: Int,
    modifier: Modifier = Modifier,
    content: @Composable (T) -> Unit,
) {
    val entries = remember { mutableStateListOf<FadeEntry<T>>().apply { target?.let { add(FadeEntry(it, 1f)) } } }
    LaunchedEffect(target) {
        val half = durationMillis / 2
        val incoming = target?.let { value -> entries.firstOrNull { it.value == value } ?: FadeEntry(value, 0f).also { entries.add(it) } }
        val outgoing = entries.filter { it !== incoming }
        coroutineScope {
            outgoing.forEach { entry ->
                launch { entry.alpha.animateTo(0f, tween((half * entry.alpha.value).toInt(), easing = LinearEasing)) }
            }
        }
        entries.removeAll(outgoing)
        incoming?.alpha?.animateTo(1f, tween(half, easing = AppleEasing))
    }
    Box(modifier, contentAlignment = Alignment.BottomStart) {
        entries.forEach { entry ->
            key(entry.value) {
                Box(
                    Modifier.graphicsLayer {
                        alpha = entry.alpha.value
                        compositingStrategy = CompositingStrategy.Offscreen
                    },
                ) {
                    content(entry.value)
                }
            }
        }
    }
}
