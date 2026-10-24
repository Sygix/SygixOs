/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.drawable.ShapeDrawable
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
import fr.sygix.sygixos.domain.ArtworkDimensions
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork
import java.io.IOException
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Buffer
import okio.ForwardingSource
import okio.buffer

data class VisualCheck(val visual: String?, val retryable: Boolean = false) {
    val usable get() = visual != null
}

private val YouTubeThumbnail = Regex("""^https?://(?:i\d?\.ytimg\.com|img\.youtube\.com)/vi(?:_webp)?/([\w-]{11})/""")

fun visualCandidates(uri: String): List<String> =
    listOfNotNull(YouTubeThumbnail.find(uri)?.let { "https://i.ytimg.com/vi/${it.groupValues[1]}/maxresdefault.jpg" }, uri).distinct()

class VisualValidator(
    private val context: Context,
    private val imageLoader: ImageLoader = context.imageLoader,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    internal var repository: ArtworkRepository? = null
    suspend fun validate(source: ProgramArtwork, policy: ArtworkPolicy): VisualCheck {
        repository?.let {
            val (image, failure) = it.resolve(source, policy, visible = false)
            return VisualCheck(image?.uri, image == null && failure != ArtworkFailure.QUALITY && source.candidates.isNotEmpty())
        }
        return validateWithinBudget(source, policy)
    }

    private suspend fun validateWithinBudget(source: ProgramArtwork, policy: ArtworkPolicy): VisualCheck = withContext(dispatcher) {
        var retryable = false
        for (candidate in source.candidates.flatMap(::visualCandidates).distinct()) {
            val request = ImageRequest.Builder(context).data(source.withCandidates(listOf(candidate)))
                .size(1920, 1080).decoderFactory(boundsDecoder)
                .artworkPolicy(policy).build()
            val result = imageLoader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable
            if (drawable != null && policy.accepts(ArtworkDimensions(drawable.intrinsicWidth, drawable.intrinsicHeight))) return@withContext VisualCheck(candidate)
            retryable = retryable || (result as? ErrorResult)?.throwable?.isTransient() == true
        }
        VisualCheck(null, retryable)
    }

    suspend fun validate(uri: String): VisualCheck = withContext(dispatcher) {
        var retryable = false
        for (candidate in visualCandidates(uri)) {
            val request = ImageRequest.Builder(context).data(candidate).size(1920, 1080).decoderFactory(boundsDecoder).build()
            val result = imageLoader.execute(request)
            val drawable = (result as? SuccessResult)?.drawable
            val width = drawable?.intrinsicWidth ?: 0
            if (width >= VisualQuality.MIN_IMAGE_WIDTH_PX && width >= drawable!!.intrinsicHeight) return@withContext VisualCheck(candidate)
            retryable = retryable || (result as? ErrorResult)?.throwable?.isTransient() == true
        }
        VisualCheck(null, retryable)
    }

    private fun Throwable.isTransient() = when (this) {
        is ArtworkLoadException -> reason in setOf(ArtworkFailure.TIMEOUT, ArtworkFailure.TRANSPORT, ArtworkFailure.PROVIDER)
        else -> this is IOException || this is HttpException && response.code >= 500
    }

    private companion object {
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
                    .coerceAtMost(1.0)
                DecodeResult(ShapeDrawable().apply { intrinsicWidth = (width * scale).roundToInt(); intrinsicHeight = (height * scale).roundToInt() }, isSampled = false)
            }
        }
    }
}
