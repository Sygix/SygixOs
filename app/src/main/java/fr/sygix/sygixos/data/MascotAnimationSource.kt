/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.res.Resources
import android.graphics.ImageDecoder
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import fr.sygix.sygixos.R
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext

interface MascotAnimationSource {
    suspend fun load(): Result<Drawable>

    fun release() = Unit
}

class RawMascotAnimationSource(
    private val io: CoroutineDispatcher = Dispatchers.IO,
    private val decoder: () -> Drawable,
) : MascotAnimationSource {

    constructor(resources: Resources) : this(decoder = { decodeMascot(resources) })

    private var pending: Deferred<Result<Drawable>>? = null
    private var requested = false

    val prefetched: Boolean
        @Synchronized get() = pending != null

    @Synchronized
    fun prefetch(scope: CoroutineScope) {
        if (pending == null && !requested) pending = scope.async(io) { decode() }
    }

    override suspend fun load(): Result<Drawable> {
        val prefetch = synchronized(this) {
            requested = true
            pending
        }
        return prefetch?.await() ?: withContext(io) { decode() }
    }

    @Synchronized
    fun trimMemory() {
        if (!requested) release()
    }

    @Synchronized
    override fun release() {
        pending?.cancel()
        pending = null
    }

    private fun decode(): Result<Drawable> = runCatching(decoder)
        .onFailure { Log.w(TAG, "animation de la mascotte illisible", it) }

    private companion object {
        const val TAG = "MascotAnimationSource"

        fun decodeMascot(resources: Resources): Drawable {
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.raw.splash_mascot))
            if (drawable is AnimatedImageDrawable) drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            return drawable
        }
    }
}
