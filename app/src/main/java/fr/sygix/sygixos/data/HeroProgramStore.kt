/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.util.AtomicFile
import fr.sygix.sygixos.model.HeroItem
import fr.sygix.sygixos.model.ProgramKind
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

interface HeroProgramStore {
    suspend fun read(): Map<String, List<HeroItem>>
    suspend fun write(table: String, items: List<HeroItem>)
}

class FileHeroProgramStore(file: File, private val io: CoroutineDispatcher = Dispatchers.IO) : HeroProgramStore {
    private val target = AtomicFile(file)
    private val mutex = Mutex()
    private var tables: Map<String, List<HeroItem>>? = null

    override suspend fun read(): Map<String, List<HeroItem>> = withContext(io) { mutex.withLock { loaded() } }

    override suspend fun write(table: String, items: List<HeroItem>) = withContext(io) {
        mutex.withLock {
            val updated = loaded() + (table to items)
            tables = updated
            val stream = target.startWrite()
            try {
                stream.write(encode(updated).toByteArray(Charsets.UTF_8))
                target.finishWrite(stream)
            } catch (error: Exception) {
                target.failWrite(stream)
                throw error
            }
        }
    }

    private fun loaded(): Map<String, List<HeroItem>> = tables ?: decodeStored().also { tables = it }

    private fun decodeStored(): Map<String, List<HeroItem>> {
        if (!target.baseFile.exists()) return emptyMap()
        return try {
            decode(String(target.readFully(), Charsets.UTF_8))
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            android.util.Log.w("HeroPrograms", "cause=snapshot-unreadable", error)
            target.delete()
            emptyMap()
        }
    }

    internal companion object {
        fun encode(tables: Map<String, List<HeroItem>>): String = JSONObject().apply {
            tables.forEach { (table, items) -> put(table, JSONArray().apply { items.forEach { put(encode(it)) } }) }
        }.toString()

        fun decode(text: String): Map<String, List<HeroItem>> {
            val root = JSONObject(text)
            return root.keys().asSequence().associateWith { table ->
                val items = root.getJSONArray(table)
                (0 until items.length()).map { decode(items.getJSONObject(it)) }
            }
        }

        private fun encode(item: HeroItem) = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("videoUrl", item.videoUrl)
            put("imageUrl", item.imageUrl)
            put("sourcePackage", item.sourcePackage)
            put("launchUri", item.launchUri)
            put("engagement", item.engagement)
            put("kind", item.kind.name)
            put("season", item.season)
            put("episode", item.episode)
            put("durationMillis", item.durationMillis)
            put("positionMillis", item.positionMillis)
            put("posterUrl", item.posterUrl)
            put("thumbnailUrl", item.thumbnailUrl)
            put("posterAspectRatio", item.posterAspectRatio)
            put("expiresAt", item.expiresAt)
            put("internalProviderId", item.internalProviderId)
            put("contentId", item.contentId)
            put("releaseDate", item.releaseDate)
            put("episodeArtwork", item.episodeArtwork)
            put("seriesId", item.seriesId)
        }

        private fun decode(o: JSONObject) = HeroItem(
            id = o.getString("id"),
            title = o.getString("title"),
            videoUrl = o.text("videoUrl"),
            imageUrl = o.text("imageUrl"),
            sourcePackage = o.text("sourcePackage"),
            launchUri = o.text("launchUri"),
            engagement = o.getLong("engagement"),
            kind = ProgramKind.valueOf(o.getString("kind")),
            season = o.text("season"),
            episode = o.text("episode"),
            durationMillis = o.number("durationMillis"),
            positionMillis = o.number("positionMillis"),
            posterUrl = o.text("posterUrl"),
            thumbnailUrl = o.text("thumbnailUrl"),
            posterAspectRatio = o.getInt("posterAspectRatio"),
            expiresAt = o.getLong("expiresAt"),
            internalProviderId = o.text("internalProviderId"),
            contentId = o.text("contentId"),
            releaseDate = o.text("releaseDate"),
            episodeArtwork = o.getBoolean("episodeArtwork"),
            seriesId = o.text("seriesId"),
        )

        private fun JSONObject.text(key: String): String? = if (has(key) && !isNull(key)) getString(key) else null
        private fun JSONObject.number(key: String): Long? = if (has(key) && !isNull(key)) getLong(key) else null
    }
}
