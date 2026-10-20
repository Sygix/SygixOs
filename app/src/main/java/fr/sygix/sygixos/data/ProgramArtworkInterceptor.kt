/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.net.Uri
import coil.intercept.Interceptor
import coil.request.CachePolicy
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.ImageResult
import fr.sygix.sygixos.domain.ArtworkPolicy
import fr.sygix.sygixos.model.ProgramArtwork

internal class ProgramArtworkInterceptor(private val repository: ArtworkRepository) : Interceptor {
    override suspend fun intercept(chain: Interceptor.Chain): ImageResult {
        val request = chain.request
        val source = request.data as? ProgramArtwork ?: run {
            val uri = Uri.parse(request.data.toString())
            if (uri.scheme !in setOf("content", "http", "https", "android.resource")) return chain.proceed(request)
            if (uri.scheme == "android.resource" && uri.authority == repository.context.packageName) return chain.proceed(request)
            if (request.parameters.value<Boolean>(ProgramImageFetcher.INDEXED) == true || request.parameters.value<ProgramArtwork>(ProgramImageFetcher.FETCH) != null) return chain.proceed(request)
            val owner = if (uri.scheme == "content") repository.context.packageManager.resolveContentProvider(uri.authority.orEmpty(), 0)?.packageName ?: "unknown" else "unknown"
            ProgramArtwork(owner, listOf(uri.toString()), uri.toString())
        }
        val policy = request.parameters.value<ArtworkPolicy>(POLICY) ?: ArtworkPolicy.Card(1)
        val (image, failure) = repository.resolve(source, policy)
        if (image == null) return ErrorResult(null, request, ArtworkLoadException(failure ?: ArtworkFailure.ABSENT))
        val indexed = Uri.parse(image.uri).scheme in setOf("http", "https", "content")
        val rewritten = request.newBuilder().data(Uri.parse(image.uri))
            .diskCacheKey(image.diskKey)
            .memoryCacheKey(image.uri + ":" + image.hash + ":" + chain.size + ":" + request.transformations.joinToString { it.cacheKey })
            .setParameter(ProgramImageFetcher.INDEXED, indexed, null)
            .build()
        return chain.proceed(rewritten)
    }

    companion object { const val POLICY = "program-artwork-policy" }
}

internal class ArtworkLoadException(val reason: ArtworkFailure) : java.io.IOException("Artwork ${reason.name.lowercase()}")
internal fun ImageRequest.Builder.artworkPolicy(policy: ArtworkPolicy) = setParameter(ProgramArtworkInterceptor.POLICY, policy, null)
