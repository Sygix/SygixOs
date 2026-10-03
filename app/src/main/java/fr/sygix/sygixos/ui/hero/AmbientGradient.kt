/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.hero

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import fr.sygix.sygixos.core.designsystem.Motion

private const val RestingPhase = 0.35f

private val Colors = listOf(Color(0xFF0E1A33), Color(0xFF101014), Color(0xFF1B1030))

@Composable
fun AmbientGradient(modifier: Modifier = Modifier, animated: Boolean = false) {
    val phase = remember { Animatable(if (animated) 0f else RestingPhase) }
    LaunchedEffect(animated) {
        if (animated) {
            val remaining = 1f - phase.value
            phase.animateTo(1f, tween((Motion.AMBIENT_PASS_MS * remaining).toInt(), easing = LinearEasing))
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .drawBehind {
                val p = phase.value
                drawRect(
                    Brush.linearGradient(
                        colors = Colors,
                        start = Offset(size.width * (0.2f * p), 0f),
                        end = Offset(size.width, size.height * (1f - 0.3f * p)),
                    ),
                )
            },
    )
}
