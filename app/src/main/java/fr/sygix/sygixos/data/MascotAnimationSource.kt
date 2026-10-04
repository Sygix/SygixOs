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
    private val resources: Resources,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MascotAnimationSource {

    private var pending: Deferred<Result<Drawable>>? = null

    @Synchronized
    fun prefetch(scope: CoroutineScope) {
        if (pending == null) pending = scope.async(io) { decode() }
    }

    override suspend fun load(): Result<Drawable> =
        synchronized(this) { pending }?.await() ?: withContext(io) { decode() }

    @Synchronized
    override fun release() {
        pending?.cancel()
        pending = null
    }

    private fun decode(): Result<Drawable> = runCatching {
        val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.raw.splash_mascot))
        if (drawable is AnimatedImageDrawable) drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
        drawable
    }.onFailure { Log.w(TAG, "animation de la mascotte illisible", it) }

    private companion object {
        const val TAG = "MascotAnimationSource"
    }
}
