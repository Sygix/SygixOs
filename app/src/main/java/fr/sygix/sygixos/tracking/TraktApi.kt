/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class TraktApi(
    private val transport: TrackingTransport,
    private val configurationSource: TraktConfigurationSource,
    private val coordinatedSessionStore: CoordinatedTrackingSessionStore,
    internal val dispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val wallClock: () -> Long = System::currentTimeMillis,
    private val monotonicClock: () -> Long = { System.nanoTime() / 1_000_000 },
) {
    private val mutationMutex = Mutex()
    private val refreshMutex = Mutex()
    private var uncertainPair: Pair<String, TraktSessionEnvelope>? = null
    private var lastMutationAt = Long.MIN_VALUE
    private var retryMutationAt = Long.MIN_VALUE
    private val latestSessions = java.util.concurrent.ConcurrentHashMap<String, TraktSessionEnvelope>()
    suspend fun request(path: String, rawToken: String? = null, method: String = "GET", body: JSONObject? = null,
                         oauth: Boolean = false, clientOverride: String? = null, retryAuth: Boolean = true,
                         retryConfiguration: Boolean = true): JSONObject = execute(path, rawToken, method, body, oauth, clientOverride, retryAuth, retryConfiguration)

    suspend fun validateCandidate(raw: String): JSONObject {
        val candidate = session(raw)
        val saved = coordinatedSessionStore.read()?.let(TraktSessionEnvelope::decode)
        if (saved?.binding == candidate.binding) return request("users/settings", raw)
        if (candidate.uncertain || candidate.expiresAt <= wallClock()) throw TrackingException(TrackingFailure.ACCESS)
        return execute("users/settings", raw, "GET", null, false, null, false, false, candidate)
    }

    private suspend fun execute(path: String, rawToken: String?, method: String, body: JSONObject?,
        oauth: Boolean, clientOverride: String?, retryAuth: Boolean, retryConfiguration: Boolean,
        candidate: TraktSessionEnvelope? = null,
    ): JSONObject {
        require(path.matches(Regex("[a-zA-Z0-9_/?=&%+.-]+")) && !path.contains(".."))
        val session = rawToken?.let { TraktSessionEnvelope.decode(it) ?: throw TrackingException(TrackingFailure.PROTOCOL) }
        val prepared = session?.let { if (candidate != null) requireNotNull(rawToken) to candidate else resolveLatest(it) }
        val active = prepared?.second
        val configuration = if (active == null && clientOverride == null) configurationSource.get() else null
        val clientId = active?.clientId ?: clientOverride ?: configuration!!.clientId
        val headers = linkedMapOf("Accept" to "application/json", "trakt-api-version" to "2", "trakt-api-key" to clientId)
        active?.let { headers["Authorization"] = "Bearer ${it.accessToken}" }
        val endpoint = if (oauth) "https://auth.trakt.tv/$path" else "https://api.trakt.tv/$path"
        val response = try {
            if (method != "GET") withTimeoutOrNull(15_000) {
                mutationMutex.withLock {
                    val now = monotonicClock()
                    val budgetWait = if (lastMutationAt == Long.MIN_VALUE) 0 else (1_000 - (now - lastMutationAt)).coerceAtLeast(0)
                    val serverWait = if (retryMutationAt == Long.MIN_VALUE) 0 else (retryMutationAt - now).coerceAtLeast(0)
                    val wait = maxOf(budgetWait, serverWait)
                    if (wait >= 15_000) throw TrackingRetryException(wait)
                    if (wait > 0) kotlinx.coroutines.delay(wait)
                    lastMutationAt = monotonicClock()
                    transport.execute(TrackingRequest(endpoint, method, headers, jsonBody = body?.toString())).also { result ->
                        result.retryAfterMillis?.let { delay -> retryMutationAt = maxOf(retryMutationAt, monotonicClock() + delay) }
                    }
                }
            } ?: throw TrackingException(TrackingFailure.NETWORK)
            else transport.execute(TrackingRequest(endpoint, method, headers, jsonBody = body?.toString()))
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (error: Exception) { throw error }
        val pagination = response.headers.filterKeys { it.startsWith("X-Pagination-", true) }
            .mapKeys { (key, _) -> key.drop(13).lowercase() }
        if (response.status in 200..299) {
            if (response.status == 204 || response.body.isBlank()) return JSONObject()
            return try {
                withContext(dispatcher) {
                    (if (response.body.trimStart().startsWith("[")) JSONObject().put("items", org.json.JSONArray(response.body)) else JSONObject(response.body))
                        .also { if (pagination.isNotEmpty()) it.put("__pagination", JSONObject(pagination)) }
                }
            }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (_: Exception) { throw TrackingException(TrackingFailure.PROTOCOL) }
        }
        if (response.status == 429 && response.retryAfterMillis != null) throw TrackingRetryException(response.retryAfterMillis)
        val error = response.body.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
        val refusal = classifyTraktRefusal(response.status, error?.text("error"))
        if (refusal == TrackingFailure.CONFIGURATION) {
            if (active != null) {
                configurationSource.invalidate(TraktConfiguration(active.clientId))
                try { configurationSource.get() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: TrackingException) { if (error.failure == TrackingFailure.STORAGE) throw error }
                throw TrackingException(TrackingFailure.ACCESS)
            }
            if (configuration != null) {
                configurationSource.invalidate(configuration)
                if (method == "GET" && !oauth && retryConfiguration) {
                    configurationSource.get()
                    return request(path, method = "GET", retryAuth = false, retryConfiguration = false)
                }
            }
        }
        if (active != null && retryAuth && isRefreshableRefusal(response.status, error)) {
            val renewed = refresh(prepared.first, active)
            if (method != "GET") throw TraktMutationNotAppliedException()
            return request(path, renewed.encode(), method, body, oauth, clientOverride, false, false)
        }
        throw TraktResponseException(response.status, error?.text("error"), response.retryAfterMillis,
            refusal ?: TrackingFailure.NETWORK)
    }

    suspend fun session(raw: String): TraktSessionEnvelope = TraktSessionEnvelope.decode(raw)
        ?: throw TrackingException(TrackingFailure.PROTOCOL)

    suspend fun configured(): TraktConfiguration = configurationSource.get()

    suspend fun invalidateConfiguration(configuration: TraktConfiguration) = configurationSource.invalidate(configuration)

    suspend fun exchange(code: String, clientId: String): TraktSessionEnvelope {
        val result = request("oauth/device/token", method = "POST", oauth = true, clientOverride = clientId,
            body = JSONObject().put("code", code).put("client_id", clientId))
        return tokenPair(result, clientId, null)
    }

    suspend fun refresh(raw: String, current: TraktSessionEnvelope): TraktSessionEnvelope = refreshMutex.withLock {
        var expectedRaw = coordinatedSessionStore.read()
        val stored = expectedRaw?.let(TraktSessionEnvelope::decode)
        if (expectedRaw != raw && (stored == null || stored.binding != current.binding)) throw TrackingException(TrackingFailure.ACCESS)
        if (stored != null && stored.binding == current.binding && !stored.uncertain && expectedRaw != raw) return@withLock stored
        if (stored?.uncertain == true) {
            uncertainPair?.takeIf { it.first == expectedRaw }?.let { (_, pair) ->
                if (coordinatedSessionStore.compareAndSet(expectedRaw, pair.encode())) { uncertainPair = null; return@withLock pair }
                throw TrackingException(TrackingFailure.ACCESS)
            }
            throw TrackingException(TrackingFailure.NETWORK)
        }
        val marker = current.copy(uncertain = true)
        val markerRaw = marker.encode()
        if (!coordinatedSessionStore.compareAndSet(expectedRaw, markerRaw)) throw TrackingException(TrackingFailure.ACCESS)
        expectedRaw = markerRaw
        uncertainPair?.takeIf { it.first == expectedRaw }?.let { (_, pair) ->
            if (coordinatedSessionStore.compareAndSet(expectedRaw, pair.encode())) { uncertainPair = null; return@withLock pair }
            throw TrackingException(TrackingFailure.ACCESS)
        }
        val config = JSONObject().put("refresh_token", current.refreshToken).put("client_id", current.clientId)
            .put("grant_type", "refresh_token")
        val response = try {
            withTimeoutOrNull(15_000) { transport.execute(TrackingRequest("https://auth.trakt.tv/oauth/token", "POST",
                mapOf("Accept" to "application/json", "Content-Type" to "application/json", "trakt-api-version" to "2", "trakt-api-key" to current.clientId),
                jsonBody = config.toString())) } ?: throw TrackingException(TrackingFailure.NETWORK)
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            throw TrackingException(TrackingFailure.NETWORK)
        }
        if (response.status !in 200..299) {
            val json = response.body.takeIf { it.isNotBlank() }?.let { runCatching { JSONObject(it) }.getOrNull() }
            if (classifyTraktRefusal(response.status, json?.text("error")) in setOf(TrackingFailure.ACCESS, TrackingFailure.CONFIGURATION)) throw TrackingException(TrackingFailure.ACCESS)
            throw TrackingException(TrackingFailure.NETWORK)
        }
        val pair = tokenPair(JSONObject(response.body), current.clientId, current.binding)
        uncertainPair = expectedRaw to pair
        if (!coordinatedSessionStore.compareAndSet(expectedRaw, pair.encode())) {
            val saved = coordinatedSessionStore.read()?.let(TraktSessionEnvelope::decode)
            if (saved?.binding == current.binding && !saved.uncertain) {
                uncertainPair = null
                if (saved.accessToken != pair.accessToken) revokeOrphan(pair)
                return@withLock saved
            }
            revokeOrphan(pair)
            throw TrackingException(TrackingFailure.ACCESS)
        }
        uncertainPair = null
        pair
    }.also { latestSessions[it.binding] = it }

    private fun tokenPair(json: JSONObject, clientId: String, binding: String?): TraktSessionEnvelope {
        val access = json.text("access_token") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val refresh = json.text("refresh_token") ?: throw TrackingException(TrackingFailure.PROTOCOL)
        val expiry = json.optLong("expires_in", -1).takeIf { it in 1..31_536_000 } ?: throw TrackingException(TrackingFailure.PROTOCOL)
        return TraktSessionEnvelope(clientId, access, refresh, wallClock() + expiry * 1000, binding ?: java.util.UUID.randomUUID().toString())
    }

    private suspend fun resolveLatest(session: TraktSessionEnvelope): Pair<String, TraktSessionEnvelope> {
        val latestRaw = coordinatedSessionStore.read() ?: throw TrackingException(TrackingFailure.ACCESS)
        val latest = TraktSessionEnvelope.decode(latestRaw) ?: throw TrackingException(TrackingFailure.PROTOCOL)
        if (latest.binding != session.binding) throw TrackingException(TrackingFailure.ACCESS)
        latestSessions.putIfAbsent(latest.binding, latest)
        if (!latest.uncertain && latest.expiresAt > wallClock() + 30_000) return latestRaw to latest
        val renewed = refresh(latestRaw, latest)
        return renewed.encode() to renewed
    }

    private fun isRefreshableRefusal(status: Int, json: JSONObject?): Boolean = status == 401 &&
        json?.text("error") in setOf("invalid_token", "token_expired")

    private suspend fun revokeOrphan(pair: TraktSessionEnvelope) {
        try {
            withContext(kotlinx.coroutines.NonCancellable + TrackingRequestAuthorization({ true }, { it() })) {
                withTimeoutOrNull(15_000) { transport.execute(TrackingRequest("https://auth.trakt.tv/oauth/revoke", "POST",
                    mapOf("Accept" to "application/json", "Content-Type" to "application/json", "trakt-api-version" to "2", "trakt-api-key" to pair.clientId),
                    jsonBody = JSONObject().put("token", pair.accessToken).put("client_id", pair.clientId).toString())) }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (_: Exception) { }
    }

    suspend fun revoke(raw: String) {
        val original = session(raw)
        val current = uncertainPair?.second?.takeIf { it.binding == original.binding } ?: latestSessions[original.binding] ?: original
        try {
            request("oauth/revoke", method = "POST", oauth = true, clientOverride = current.clientId,
                body = JSONObject().put("token", current.accessToken).put("client_id", current.clientId))
        } finally {
            latestSessions.remove(original.binding)
            if (uncertainPair?.second?.binding == original.binding) uncertainPair = null
        }
    }
}

class TraktResponseException(val status: Int, val errorCode: String?, val retryAfterMillis: Long?, failure: TrackingFailure = TrackingFailure.NETWORK) : TrackingException(failure)
class TraktMutationNotAppliedException : TrackingException(TrackingFailure.NETWORK)

private fun classifyProviderRefusal(provider: String, status: Int?, code: String?): TrackingFailure? = when (provider) {
    "trakt" -> when {
        code == "invalid_client" && status in setOf(400, 401, 403) -> TrackingFailure.CONFIGURATION
        code == "invalid_grant" && status in setOf(400, 401) -> TrackingFailure.ACCESS
        status == 401 && code in setOf("invalid_token", "token_expired") -> TrackingFailure.ACCESS
        else -> null
    }
    "betaseries" -> when (code?.toIntOrNull()) {
        1001 -> TrackingFailure.CONFIGURATION
        1002, 2002, 2003, 2004 -> TrackingFailure.ACCESS
        null -> null
        else -> TrackingFailure.PROTOCOL
    }
    else -> null
}

internal fun classifyTraktRefusal(status: Int, error: String?): TrackingFailure? = classifyProviderRefusal("trakt", status, error)

internal fun classifyBetaSeriesRefusal(code: Int): TrackingFailure =
    classifyProviderRefusal("betaseries", null, code.toString()) ?: TrackingFailure.PROTOCOL
