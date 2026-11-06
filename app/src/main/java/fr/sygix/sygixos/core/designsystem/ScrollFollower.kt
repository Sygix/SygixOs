/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.core.designsystem

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Stable

@Stable
class ScrollFollower(private val state: ScrollableState) {
    private val offset = Animatable(0f)

    suspend fun follow(spec: AnimationSpec<Float>?, distance: () -> Float) {
        val running = offset.isRunning
        val velocity = offset.velocity
        if (!running && distance() == 0f) return
        state.scroll {
            val delta = distance()
            var applied = offset.value
            val scope = this
            val consume = { value: Float ->
                scope.scrollBy(value - applied)
                applied = value
            }
            val target = offset.value + delta
            when {
                spec == null -> {
                    offset.snapTo(target)
                    consume(target)
                }
                running -> offset.animateTo(target, Retarget, velocity) { consume(value) }
                else -> offset.animateTo(target, spec) { consume(value) }
            }
        }
    }

    private companion object {
        val Retarget = spring<Float>(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
    }
}
