package fr.sygix.sygixos.data

import android.content.Context
import android.util.Log
import coil.ImageLoader
import coil.imageLoader
import coil.request.CachePolicy
import coil.request.ImageRequest
import coil.request.SuccessResult
import fr.sygix.sygixos.domain.VisualQuality
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn

/**
 * Valide et précharge les visuels une fois pour toutes : chargement réel via Coil,
 * largeur minimale vérifiée, les premiers gardés en mémoire, les autres sur disque.
 * Émet chaque URI validée dès qu'elle l'est, dans l'ordre demandé.
 */
class VisualValidator(
    private val context: Context,
    private val imageLoader: ImageLoader = context.imageLoader,
) {
    fun validate(uris: List<String>, keepInMemory: Int): Flow<String> = flow {
        uris.forEachIndexed { index, uri ->
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(1920, 1080)
                .memoryCachePolicy(if (index < keepInMemory) CachePolicy.ENABLED else CachePolicy.DISABLED)
                .build()
            val width = (runCatching { imageLoader.execute(request) }.getOrNull() as? SuccessResult)
                ?.drawable?.intrinsicWidth ?: 0
            if (width >= VisualQuality.MIN_WIDTH_PX) emit(uri) else Log.d(TAG, "visuel écarté (${width}px): $uri")
        }
    }.flowOn(Dispatchers.IO)

    private companion object {
        const val TAG = "VisualValidator"
    }
}
