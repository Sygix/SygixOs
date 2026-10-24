/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class LocalTrackingIdentity(val identity: TrackingIdentity, val parent: TrackingIdentity)

class TrackingIdentityCache(private val file: File?, private val dispatcher: CoroutineDispatcher) {
    private val mutex = Mutex()
    private var values: JSONObject? = null
    private fun values(): JSONObject = values ?: runCatching {
        file?.takeIf { it.exists() && it.length() <= 512 * 1024 }?.readText()?.let(::JSONObject)
    }.getOrNull().orEmptyJson().also { values = it }

    suspend fun get(key: String, fingerprint: String): LocalTrackingIdentity? = withContext(dispatcher) { mutex.withLock {
        val entry = values().optJSONObject(key)?.takeIf { it.optString("fingerprint") == fingerprint } ?: return@withLock null
        runCatching {
            val identity = TrackingIdentity("betaseries", TrackingType.valueOf(entry.getString("type")), entry.getString("id"))
            LocalTrackingIdentity(identity, if (identity.type == TrackingType.EPISODE) TrackingIdentity("betaseries", TrackingType.SHOW, entry.getString("parent")) else identity)
        }.getOrNull()
    } }

    suspend fun put(key: String, fingerprint: String, content: TrackingContent) = withContext(dispatcher) { mutex.withLock {
        val data = values()
        data.put(key, JSONObject().put("fingerprint", fingerprint).put("type", content.identity.type.name).put("id", content.identity.id).put("parent", content.parent.id))
        while (data.length() > 200) data.remove(data.keys().next())
        file?.let {
            val atomic = android.util.AtomicFile(it)
            val output = atomic.startWrite()
            try { output.write(data.toString().toByteArray()); atomic.finishWrite(output) }
            catch (error: Exception) { atomic.failWrite(output); throw TrackingException(TrackingFailure.STORAGE) }
        }
    } }

    private fun JSONObject?.orEmptyJson() = this ?: JSONObject()
}
