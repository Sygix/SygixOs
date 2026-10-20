/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import coil.disk.DiskCache
import fr.sygix.sygixos.domain.ArtworkAge
import fr.sygix.sygixos.domain.ArtworkDimensions
import fr.sygix.sygixos.model.ProgramArtwork
import java.security.MessageDigest
import okio.buffer

internal data class CachedArtwork(
    val uri: String,
    val bytes: ByteArray,
    val dimensions: ArtworkDimensions,
    val fetchedAt: Long,
    val version: String,
    val associations: Map<String, String> = emptyMap(),
    val sharedIdentities: Set<String> = emptySet(),
) {
    val expiresAt get() = fetchedAt + ArtworkAge.RETAIN_MS

    fun compatible(source: ProgramArtwork): Boolean {
        val associated = associations[artworkHash(source.publicationId)]
        if (associated != null) return associated == artworkHash(source.identity)
        return source.sharedIdentity?.let { artworkHash(it) in sharedIdentities } == true
    }

    fun associatedWith(source: ProgramArtwork): CachedArtwork = copy(
        associations = (associations + (artworkHash(source.publicationId) to artworkHash(source.identity))).entries.toList().takeLast(64).associate { it.toPair() },
        sharedIdentities = (sharedIdentities + listOfNotNull(source.sharedIdentity?.let(::artworkHash))).toList().takeLast(64).toSet(),
    )
}

@OptIn(coil.annotation.ExperimentalCoilApi::class)
internal class ArtworkDiskStore(private val disk: DiskCache) {
    fun read(key: String, uri: String, now: Long): CachedArtwork? = runCatching {
        disk.openSnapshot(key)?.use { snapshot ->
            val metadata = disk.fileSystem.source(snapshot.metadata).buffer().use { it.readUtf8().split('\n') }
            if (metadata.size != 7 || metadata[0] != "artwork-v2") return@use null
            val fetchedAt = metadata[1].toLongOrNull() ?: return@use null
            if (!ArtworkAge.usable(fetchedAt, now)) return@use null
            val dimensions = ArtworkDimensions(metadata[2].toInt(), metadata[3].toInt())
            if (!dimensions.valid) return@use null
            val bytes = disk.fileSystem.source(snapshot.data).buffer().use { it.readByteArray() }
            if (artworkHash(bytes) != metadata[4]) return@use null
            val associations = metadata[5].split(',').filter { it.isNotEmpty() }.associate { val (id, fingerprint) = it.split(':'); id to fingerprint }
            val shared = metadata[6].split(',').filter { it.isNotEmpty() }.toSet()
            CachedArtwork(uri, bytes, dimensions, fetchedAt, metadata[4], associations, shared)
        }
    }.getOrNull()

    @Synchronized fun write(key: String, artwork: CachedArtwork) {
        val previous = read(key, artwork.uri, artwork.fetchedAt)
        if (previous != null && previous.version != artwork.version) writeEntry("$key:previous", previous)
        writeEntry(key, artwork)
    }

    @Synchronized fun associate(key: String, artwork: CachedArtwork, source: ProgramArtwork, now: Long): CachedArtwork {
        val current = read(key, artwork.uri, now) ?: return artwork
        if (current.version != artwork.version || current.fetchedAt != artwork.fetchedAt) return artwork
        val associated = current.associatedWith(source)
        if (associated.associations != current.associations || associated.sharedIdentities != current.sharedIdentities) writeEntry(key, associated)
        return associated
    }

    @Synchronized fun reject(key: String, uri: String, version: String, now: Long): CachedArtwork? {
        val current = read(key, uri, now)
        if (current != null && current.version != version) return current
        val previous = read("$key:previous", uri, now)
        if (previous != null) writeEntry(key, previous) else disk.remove(key)
        disk.remove("$key:previous")
        return previous
    }

    private fun writeEntry(key: String, artwork: CachedArtwork) {
        val editor = disk.openEditor(key) ?: error("Artwork cache entry unavailable")
        try {
            disk.fileSystem.sink(editor.data).buffer().use { it.write(artwork.bytes) }
            disk.fileSystem.sink(editor.metadata).buffer().use {
                it.writeUtf8(listOf("artwork-v2", artwork.fetchedAt, artwork.dimensions.width, artwork.dimensions.height, artwork.version,
                    artwork.associations.entries.joinToString(",") { (id, fingerprint) -> "$id:$fingerprint" }, artwork.sharedIdentities.joinToString(",")).joinToString("\n"))
            }
            editor.commit()
        } catch (error: Exception) {
            editor.abort()
            throw error
        }
    }
}

internal fun artworkHash(value: String): String = artworkHash(value.toByteArray())
internal fun artworkHash(value: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(value).joinToString("") { "%02x".format(it) }
