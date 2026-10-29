/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONObject

data class TraktSessionEnvelope(
    val clientId: String,
    val accessToken: String,
    val refreshToken: String,
    val expiresAt: Long,
    val binding: String,
    val uncertain: Boolean = false,
) {
    override fun toString() = "TraktSessionEnvelope(redacted)"

    fun encode(): String = JSONObject().put("provider", PROVIDER).put("version", VERSION)
        .put("clientId", clientId).put("accessToken", accessToken).put("refreshToken", refreshToken)
        .put("expiresAt", expiresAt).put("binding", binding).put("uncertain", uncertain)
        .toString().toByteArray().let { "trakt:v1:" + java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    companion object {
        private const val PROVIDER = "trakt"
        private const val VERSION = 1

        fun provider(raw: String?): String {
            if (raw?.startsWith("trakt:") == true) {
                if (decode(raw) == null) throw TrackingException(TrackingFailure.PROTOCOL)
                return PROVIDER
            }
            return "betaseries"
        }

        fun binding(raw: String?): String {
            if (raw?.startsWith("trakt:") == true) return decode(raw)?.binding ?: throw TrackingException(TrackingFailure.PROTOCOL)
            return raw.orEmpty()
        }

        fun decode(raw: String?): TraktSessionEnvelope? {
            if (raw?.startsWith("trakt:v1:") != true) return null
            return runCatching {
                val json = JSONObject(java.util.Base64.getUrlDecoder().decode(raw.removePrefix("trakt:v1:")).toString(Charsets.UTF_8))
                require(json.optInt("version") == VERSION && json.optString("provider") == PROVIDER)
                val client = json.getString("clientId")
                val access = json.getString("accessToken")
                val refresh = json.getString("refreshToken")
                val binding = json.getString("binding")
                require(client.isNotBlank() && access.isNotBlank() && refresh.isNotBlank() && binding.isNotBlank())
                TraktSessionEnvelope(client, access, refresh, json.getLong("expiresAt"), binding, json.optBoolean("uncertain"))
            }.getOrNull()
        }

        fun create(clientId: String, accessToken: String, refreshToken: String, expiresAt: Long): TraktSessionEnvelope =
            TraktSessionEnvelope(clientId, accessToken, refreshToken, expiresAt, UUID.randomUUID().toString())
    }
}

class CoordinatedTrackingSessionStore(private val delegate: TrackingSessionStore) : TrackingSessionStore {
    private val mutex = Mutex()
    private var loaded = false
    private var cached: String? = null
    override suspend fun read(): String? = mutex.withLock {
        if (loaded) return@withLock cached
        try { delegate.read().also { cached = it; loaded = true } } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
    }
    override suspend fun write(token: String?) = mutex.withLock {
        try { delegate.write(token); cached = token; loaded = true } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
    }

    suspend fun compareAndSet(expected: String?, replacement: String?): Boolean = mutex.withLock {
        try {
            val current = delegate.read()
            if (current != expected) { cached = current; loaded = true; return@withLock false }
            delegate.write(replacement)
            cached = replacement
            loaded = true
            true
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
    }
}
