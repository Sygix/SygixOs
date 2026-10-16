/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.imageLoader
import coil.network.HttpException
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import fr.sygix.sygixos.domain.VisualQuality
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

data class VisualCheck(val uri: String, val usable: Boolean, val retryable: Boolean = false)

class VisualValidator(
    private val context: Context,
    private val imageLoader: ImageLoader = context.imageLoader,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    fun validate(uris: List<String>, keepInMemory: Int): Flow<VisualCheck> = flow {
        uris.forEachIndexed { index, uri ->
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(1920, 1080)
                .memoryCachePolicy(if (index < keepInMemory) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .build()
            val result = imageLoader.execute(request)
            val width = (result as? SuccessResult)?.drawable?.intrinsicWidth ?: 0
            val usable = width >= VisualQuality.MIN_WIDTH_PX
            if (!usable) Log.d(TAG, "visuel écarté (${width}px): $uri")
            emit(VisualCheck(uri, usable, (result as? ErrorResult)?.throwable?.isTransient() == true))
        }
    }.flowOn(dispatcher)

    private fun Throwable.isTransient() = this is IOException || this is HttpException && response.code >= 500

    private companion object {
        const val TAG = "VisualValidator"
    }
}
