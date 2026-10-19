/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.CancellationSignal
import fr.sygix.sygixos.domain.ArtworkDimensions
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

internal data class ArtworkBytes(val bytes: ByteArray, val dimensions: ArtworkDimensions)

internal class ArtworkCancellation {
    val signal = CancellationSignal()
    val cancelled = AtomicBoolean()
    val connection = AtomicReference<HttpURLConnection?>()
    fun cancel() {
        cancelled.set(true)
        connection.get()?.disconnect()
        signal.cancel()
    }
    fun check() { if (cancelled.get()) throw java.io.InterruptedIOException("Artwork cancelled") }
}

internal fun interface ArtworkTransport {
    fun read(uri: String, cancellation: ArtworkCancellation): ArtworkBytes
}

internal val ArtworkDecodeDispatcher = kotlinx.coroutines.Dispatchers.IO.limitedParallelism(2)

internal class AndroidArtworkTransport(private val resolver: ContentResolver) : ArtworkTransport {
    override fun read(uri: String, cancellation: ArtworkCancellation): ArtworkBytes {
        cancellation.check()
        val parsed = Uri.parse(uri)
        val bytes = when (parsed.scheme) {
            "file" -> java.io.File(java.net.URI(uri)).inputStream().use { copy(it, cancellation) }
            "content" -> {
                val client = resolver.acquireUnstableContentProviderClient(parsed) ?: throw FileNotFoundException("Artwork provider unavailable")
                client.use {
                    cancellation.check()
                    val descriptor = client.openTypedAssetFileDescriptor(parsed, "image/*", null, cancellation.signal)
                        ?: throw FileNotFoundException("Artwork unavailable")
                    var stream: InputStream? = null
                    try {
                        cancellation.check()
                        stream = descriptor.createInputStream()
                        copy(stream, cancellation)
                    } finally {
                        if (stream == null) descriptor.close() else stream.close()
                    }
                }
            }
            "http", "https" -> {
                val connection = URL(uri).openConnection() as HttpURLConnection
                cancellation.connection.set(connection)
                try {
                    cancellation.check()
                    connection.connectTimeout = 8_000
                    connection.readTimeout = 8_000
                    connection.instanceFollowRedirects = true
                    if (connection.responseCode !in 200..299) throw ArtworkHttpException(connection.responseCode)
                    connection.inputStream.use { copy(it, cancellation) }
                } finally {
                    cancellation.connection.set(null)
                    connection.disconnect()
                }
            }
            else -> throw FileNotFoundException("Unsupported artwork scheme")
        }
        cancellation.check()
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val dimensions = ArtworkDimensions(bounds.outWidth, bounds.outHeight)
        require(dimensions.valid) { "Unreadable artwork" }
        val sample = BitmapFactory.Options().apply {
            inSampleSize = Integer.highestOneBit((maxOf(dimensions.width, dimensions.height) / 64).coerceAtLeast(1))
        }
        kotlinx.coroutines.runBlocking(ArtworkDecodeDispatcher) {
            val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sample) ?: error("Undecodable artwork")
            decoded.recycle()
        }
        return ArtworkBytes(bytes, dimensions)
    }

    private fun copy(stream: InputStream, cancellation: ArtworkCancellation): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            cancellation.check()
            val count = stream.read(buffer)
            if (count < 0) break
            if (output.size() + count > MAX_BYTES) throw java.io.IOException("Artwork exceeds source limit")
            output.write(buffer, 0, count)
        }
        cancellation.check()
        return output.toByteArray()
    }

    private companion object { const val MAX_BYTES = 16 * 1024 * 1024 }
}

internal class ArtworkHttpException(val status: Int) : java.io.IOException("Artwork HTTP $status")
