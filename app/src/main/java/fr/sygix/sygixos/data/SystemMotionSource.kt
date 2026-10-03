/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.animation.ValueAnimator

interface SystemMotionSource {
    fun animationsEnabled(): Boolean
}

class AnimatorMotionSource : SystemMotionSource {
    override fun animationsEnabled(): Boolean = ValueAnimator.areAnimatorsEnabled()
}
