/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.graphics.drawable.ShapeDrawable
import android.net.Uri
import coil.decode.DataSource
import coil.intercept.Interceptor
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import coil.request.SuccessResult
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork

internal class ProgramArtworkInterceptor(private val context: Context, private val service: ProgramArtworkService) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val data = request.data
        val source = if (data is ProgramArtwork) data else {
            val uri = Uri.parse(data.toString())
            if (uri.scheme !in setOf("content", "http", "https")) return chain.proceed(request)
            val owner = if (uri.scheme == "content") {
                context.packageManager.resolveContentProvider(uri.authority.orEmpty(), 0)?.packageName ?: uri.authority.orEmpty()
            } else uri.host.orEmpty()
            ProgramArtwork(owner, listOf(uri.toString()), uri.toString())
        }
        val policy = request.parameters.value<ArtworkPolicy>(POLICY)
        return kotlinx.coroutines.withTimeoutOrNull(8_000) { when (val result = service.load(source, policy)) {
            is ArtworkResult.Failed -> ErrorResult(null, request, ArtworkLoadException(result.cause))
            is ArtworkResult.Ready -> {
                val image = result.artwork
                if (request.parameters.value<Boolean>(BOUNDS) == true) {
                    SuccessResult(ShapeDrawable().apply { intrinsicWidth = image.dimensions.width; intrinsicHeight = image.dimensions.height }, request, DataSource.DISK)
                } else decode(chain, source, policy, image)
            }
        } } ?: ErrorResult(null, request, ArtworkLoadException(ArtworkFailure.TIMEOUT))
    }

    private suspend fun decode(chain: Interceptor.Chain, source: ProgramArtwork, policy: ArtworkPolicy?, initial: CachedArtwork): ImageResult {
        var image = initial
        val attempted = mutableSetOf<String>()
        val rejectedUris = mutableSetOf<String>()
        while (attempted.add(image.uri + image.version)) {
            val decoded = chain.proceed(decodedRequest(chain.request, source, image, chain.size))
            if (decoded !is ErrorResult) return decoded
            rejectedUris += image.uri
            val previous = service.decodeFailed(source, image)
            if (previous != null && (policy == null || policy.accepts(previous.dimensions))) {
                image = previous
            } else {
                val alternative = service.load(source.withCandidates(source.candidates.filterNot { it in rejectedUris }), policy)
                if (alternative !is ArtworkResult.Ready) return decoded
                image = alternative.artwork
            }
        }
        return ErrorResult(null, chain.request, ArtworkLoadException(ArtworkFailure.TRANSPORT))
    }

    private fun decodedRequest(request: ImageRequest, source: ProgramArtwork, image: CachedArtwork, size: coil.size.Size) = request.newBuilder()
        .data(image.bytes)
        .memoryCacheKey(artworkHash(source.packageName + source.identity + image.uri) + ":" + image.version + ":" + size + ":" + request.transformations.joinToString { it.cacheKey })
        .diskCachePolicy(CachePolicy.DISABLED).build()

    companion object {
        const val POLICY = "program-artwork-policy"
        const val BOUNDS = "program-artwork-bounds"
    }
}

internal class ArtworkLoadException(val reason: ArtworkFailure) : java.io.IOException("Artwork ${reason.name.lowercase()}")

internal fun ImageRequest.Builder.artworkPolicy(policy: ArtworkPolicy) = setParameter(ProgramArtworkInterceptor.POLICY, policy, null)
