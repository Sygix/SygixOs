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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface MascotAnimationSource {
    suspend fun load(): Result<Drawable>
}

class RawMascotAnimationSource(
    private val resources: Resources,
    private val io: CoroutineDispatcher = Dispatchers.IO,
) : MascotAnimationSource {

    override suspend fun load(): Result<Drawable> = withContext(io) {
        runCatching {
            val drawable = ImageDecoder.decodeDrawable(ImageDecoder.createSource(resources, R.raw.splash_mascot))
            if (drawable is AnimatedImageDrawable) drawable.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            drawable
        }.onFailure { Log.w(TAG, "animation de la mascotte illisible", it) }
    }

    private companion object {
        const val TAG = "MascotAnimationSource"
    }
}
