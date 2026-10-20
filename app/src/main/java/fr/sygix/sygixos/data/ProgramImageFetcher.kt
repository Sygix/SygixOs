/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.ContentResolver
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.CancellationSignal
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.ImageSource
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.fetch.SourceResult
import coil.request.Options
import fr.sygix.sygixos.domain.ArtworkDimensions
import java.io.Closeable
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InterruptedIOException
import java.util.concurrent.atomic.AtomicBoolean
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer

internal class ImageCancellation {
    val signal = CancellationSignal()
    private val cancelled = AtomicBoolean()
    private val resources = mutableListOf<AutoCloseable>()
    private var call: Call? = null

    @Synchronized fun <T : AutoCloseable> own(value: T): T {
        if (cancelled.get()) { value.close(); throw InterruptedIOException("Artwork cancelled") }
        resources += value
        return value
    }
    @Synchronized fun own(value: Call) { check(); call = value }
    fun check() { if (cancelled.get()) throw InterruptedIOException("Artwork cancelled") }
    fun cancel() {
        cancelled.set(true)
        val held = synchronized(this) { resources.toList().asReversed().also { resources.clear(); call?.cancel(); call = null } }
        held.forEach { runCatching(it::close) }
        signal.cancel()
    }
    @Synchronized fun release() { resources.asReversed().forEach { runCatching(it::close) }; resources.clear(); call = null }
}

@OptIn(coil.annotation.ExperimentalCoilApi::class)
internal class ProgramImageFetcher(
    private val disk: DiskCache,
    private val uri: String,
    private val options: Options,
    private val repository: ArtworkRepository?,
) : Fetcher {
    override suspend fun fetch(): SourceResult {
        val source = options.parameters.value<fr.sygix.sygixos.model.ProgramArtwork>(FETCH)
        if (source != null) {
            val result = checkNotNull(repository).retrieve(source, uri, options.parameters.value<Boolean>(VISIBLE) != false)
            if (result == null) throw ArtworkLoadException(ArtworkFailure.TRANSPORT)
        }
        val snapshot = disk.openSnapshot(uri) ?: throw FileNotFoundException("Artwork evicted")
        return SourceResult(ImageSource(snapshot.data, disk.fileSystem, uri, snapshot), null, DataSource.DISK)
    }

    class Factory(private val disk: DiskCache, private val repository: ArtworkRepository? = null) : Fetcher.Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? =
            if (options.parameters.value<Boolean>(INDEXED) == true || options.parameters.value<fr.sygix.sygixos.model.ProgramArtwork>(FETCH) != null) ProgramImageFetcher(disk, data.toString(), options, repository) else null
    }

    companion object {
        const val INDEXED = "indexed-program-image"
        const val FETCH = "fetch-program-image"
        const val VISIBLE = "visible-program-image"
    }
}

internal fun interface ImageBytesReader { fun read(uri: Uri, cancellation: ImageCancellation): ByteArray }

internal class ProgramImageReader(private val resolver: ContentResolver, private val http: OkHttpClient) : ImageBytesReader {
    override fun read(uri: Uri, cancellation: ImageCancellation): ByteArray {
        cancellation.check()
        return try {
            val stream = when (uri.scheme) {
                "content" -> {
                    val client = cancellation.own(resolver.acquireUnstableContentProviderClient(uri) ?: throw FileNotFoundException("Artwork provider unavailable"))
                    val descriptor = cancellation.own(client.openAssetFile(uri, "r", cancellation.signal) ?: throw FileNotFoundException("Artwork unavailable"))
                    cancellation.own(descriptor.createInputStream())
                }
                "http", "https" -> {
                    val call = http.newCall(Request.Builder().url(uri.toString()).build())
                    cancellation.own(call)
                    val response = cancellation.own(call.execute())
                    if (!response.isSuccessful) throw java.io.IOException("Artwork HTTP ${response.code}")
                    cancellation.own(response.body?.byteStream() ?: throw FileNotFoundException("Artwork body absent"))
                }
                else -> throw FileNotFoundException("Unsupported program image")
            }
            copy(stream, cancellation)
        } finally { cancellation.release() }
    }

    private fun copy(stream: InputStream, cancellation: ImageCancellation): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        while (true) {
            cancellation.check()
            val size = stream.read(buffer)
            if (size < 0) break
            if (output.size() + size > 16 * 1024 * 1024) throw java.io.IOException("Artwork exceeds source limit")
            output.write(buffer, 0, size)
        }
        cancellation.check()
        return output.toByteArray()
    }
}

internal fun verifiedDimensions(bytes: ByteArray): ArtworkDimensions {
    verifyCompleteEncoding(bytes)
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    val dimensions = ArtworkDimensions(bounds.outWidth, bounds.outHeight)
    if (!dimensions.valid) throw java.io.IOException("Unreadable image header")
    val sample = BitmapFactory.Options().apply { inSampleSize = Integer.highestOneBit((maxOf(dimensions.width, dimensions.height) / 64).coerceAtLeast(1)) }
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, sample) ?: throw java.io.IOException("Undecodable image")
    bitmap.recycle()
    return dimensions
}

private fun verifyCompleteEncoding(bytes: ByteArray) {
    fun fail(): Nothing = throw java.io.IOException("Truncated image")
    if (bytes.size >= 8 && bytes.copyOfRange(0, 8).contentEquals(byteArrayOf(-119, 80, 78, 71, 13, 10, 26, 10))) {
        val data = java.nio.ByteBuffer.wrap(bytes)
        data.position(8)
        while (data.remaining() >= 12) {
            val length = data.int
            if (length < 0 || length > data.remaining() - 8) fail()
            val type = data.int
            data.position(data.position() + length + 4)
            if (type == 0x49454e44) return
        }
        fail()
    }
    if (bytes.size >= 2 && bytes[0] == (-1).toByte() && bytes[1] == (-40).toByte() &&
        !(1 until bytes.size).any { bytes[it - 1] == (-1).toByte() && bytes[it] == (-39).toByte() }) fail()
    if (bytes.size >= 12 && String(bytes, 0, 4, Charsets.US_ASCII) == "RIFF" && String(bytes, 8, 4, Charsets.US_ASCII) == "WEBP") {
        val length = java.nio.ByteBuffer.wrap(bytes, 4, 4).order(java.nio.ByteOrder.LITTLE_ENDIAN).int.toLong() and 0xffffffffL
        if (length + 8 > bytes.size) fail()
    }
}

@OptIn(coil.annotation.ExperimentalCoilApi::class)
internal fun DiskCache.writeVerified(uri: String, bytes: ByteArray) {
    val editor = openEditor(uri) ?: throw java.io.IOException("Artwork entry busy")
    try {
        fileSystem.sink(editor.data).buffer().use { it.write(bytes) }
        editor.commitAndOpenSnapshot()?.close()
    } catch (error: Exception) { editor.abort(); throw error }
}

internal fun imageFailure(app: String, uri: Uri, cause: String) {
    android.util.Log.w("ProgramArtwork", "app=$app scheme=${uri.scheme} authority=${diagnosticAuthority(uri)} cause=$cause")
}

internal fun diagnosticAuthority(uri: Uri): String = if (uri.scheme in setOf("content", "android.resource")) uri.host.orEmpty().takeIf { it.matches(Regex("[A-Za-z][A-Za-z0-9_.]*")) } ?: "redacted" else "redacted"
