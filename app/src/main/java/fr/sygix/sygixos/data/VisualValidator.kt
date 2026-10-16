/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.ShapeDrawable
import android.util.Log
import coil.ImageLoader
import coil.decode.DecodeResult
import coil.decode.DecodeUtils
import coil.decode.Decoder
import coil.imageLoader
import coil.network.HttpException
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.pxOrElse
import fr.sygix.sygixos.domain.VisualQuality
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

data class VisualCheck(val usable: Boolean, val retryable: Boolean = false)

class VisualValidator(
    private val context: Context,
    private val imageLoader: ImageLoader = context.imageLoader,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    suspend fun validate(uri: String): VisualCheck = withContext(dispatcher) {
        val request = ImageRequest.Builder(context).data(uri).size(1920, 1080).decoderFactory(boundsDecoder).build()
        val result = imageLoader.execute(request)
        val width = (result as? SuccessResult)?.drawable?.intrinsicWidth ?: 0
        val usable = width >= VisualQuality.MIN_WIDTH_PX
        if (!usable) Log.d(TAG, "visuel écarté (${width}px): $uri")
        VisualCheck(usable, (result as? ErrorResult)?.throwable?.isTransient() == true)
    }

    private fun Throwable.isTransient() = this is IOException || this is HttpException && response.code >= 500

    private companion object {
        const val TAG = "VisualValidator"
        val boundsDecoder = Decoder.Factory { result, options, _ ->
            Decoder {
                var failure: IOException? = null
                val source = object : ForwardingSource(result.source.source()) {
                    override fun read(sink: Buffer, byteCount: Long) = try { super.read(sink, byteCount) } catch (e: IOException) { failure = e; throw e }
                }
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(source.buffer().inputStream(), null, bounds)
                failure?.let { throw it }
                val (width, height) = bounds.outWidth to bounds.outHeight
                check(width > 0 && height > 0) { "unreadable image" }
                val scale = DecodeUtils.computeSizeMultiplier(width, height, options.size.width.pxOrElse { width }, options.size.height.pxOrElse { height }, options.scale)
                DecodeResult(ShapeDrawable().apply { intrinsicWidth = (width * scale).roundToInt() }, isSampled = false)
            }
        }
    }
}
