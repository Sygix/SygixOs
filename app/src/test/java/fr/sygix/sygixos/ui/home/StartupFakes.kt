/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Color
import android.graphics.drawable.Animatable
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import fr.sygix.sygixos.data.MascotAnimationSource
import fr.sygix.sygixos.data.SystemMotionSource

internal class FakeMascotDrawable : ColorDrawable(Color.WHITE), Animatable {
    var running = false
        private set
    var starts = 0
        private set

    override fun start() {
        running = true
        starts++
    }

    override fun stop() {
        running = false
    }

    override fun isRunning(): Boolean = running
}

internal class FakeMascotSource(private val drawable: Drawable? = FakeMascotDrawable()) : MascotAnimationSource {
    override suspend fun load(): Result<Drawable> =
        drawable?.let { Result.success(it) } ?: Result.failure(IllegalStateException("mascotte illisible"))
}

internal class FakeMotionSource(private val enabled: Boolean = true) : SystemMotionSource {
    var reads = 0
        private set

    override fun animationsEnabled(): Boolean {
        reads++
        return enabled
    }
}
