/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.net.URLEncoder
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class BetaSeriesApi(
    private val transport: TrackingTransport,
    private val configuration: BetaSeriesConfigurationSource,
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.Default,
) {
    suspend fun request(path: String, token: String? = null, method: String = "GET", fields: Map<String, String> = emptyMap()): JSONObject {
        require(path.matches(Regex("[a-z_]+/[a-z_]+")))
        val config = configuration.get()
        val result = execute(config, path, token, method, fields)
        if (result.errorsCode() == 1001) {
            configuration.invalidate(config)
            if (method == "GET") {
                val renewed = configuration.get()
                val replayed = execute(renewed, path, token, method, fields)
                if (replayed.errorsCode() == 1001) configuration.invalidate(renewed)
                return checked(replayed)
            }
        }
        return checked(result)
    }

    private suspend fun execute(config: BetaSeriesConfiguration, path: String, token: String?, method: String, fields: Map<String, String>): JSONObject {
        val parameters = if (path.startsWith("oauth/")) fields + ("client_id" to config.apiKey) else fields
        val query = if (method in setOf("GET", "DELETE") && parameters.isNotEmpty()) parameters.entries.joinToString("&", "?") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" } else ""
        val headers = mutableMapOf("Accept" to "application/json", "X-BetaSeries-Version" to "3.0", "X-BetaSeries-Key" to config.apiKey)
        if (token != null) headers["X-BetaSeries-Token"] = token
        val response = transport.execute(TrackingRequest("${config.apiHost}/$path$query", method, headers, parameters.takeUnless { method in setOf("GET", "DELETE") } ?: emptyMap()))
        val json = try { kotlinx.coroutines.withContext(dispatcher) { JSONObject(response.body) } }
        catch (_: Exception) { throw TrackingException(if (response.status in setOf(401, 403)) TrackingFailure.ACCESS else TrackingFailure.PROTOCOL) }
        if (response.status in setOf(401, 403) && json.errorsCode() != 1001) throw TrackingException(TrackingFailure.ACCESS)
        if (response.status !in 200..299 && json.errorsCode() == null) throw TrackingException(TrackingFailure.NETWORK)
        return json
    }

    private fun checked(value: JSONObject): JSONObject {
        val code = value.errorsCode() ?: return value
        throw BetaSeriesApiException(code)
    }

    suspend fun pages(path: String, key: String, token: String, fields: Map<String, String> = emptyMap()): List<JSONObject> = withTimeoutOrNull(90_000) {
        val result = linkedMapOf<String, JSONObject>()
        var offset = 0
        try { for (page in 1..200) {
            val cursor = when (path) {
                "shows/member" -> "offset" to offset.toString()
                "movies/member" -> "start" to offset.toString()
                else -> "page" to page.toString()
            }
            val json = request(path, token, fields = mapOf("limit" to "100") + fields + cursor)
            val array = json.optJSONArray(key) ?: throw TrackingException(TrackingFailure.PROTOCOL)
            val batch = (0 until array.length()).map { array.getJSONObject(it) }
            val previousSize = result.size
            batch.forEach { result[it.requiredId()] = it }
            if (batch.isNotEmpty() && result.size == previousSize) throw TrackingException(TrackingFailure.INCOMPLETE)
            offset += batch.size
            val total = json.optInt("total", -1)
            if (total >= 0 && result.size >= total) return@withTimeoutOrNull result.values.toList()
            if (batch.isEmpty()) {
                if (total > result.size) throw TrackingException(TrackingFailure.INCOMPLETE)
                return@withTimeoutOrNull result.values.toList()
            }
        }
        throw TrackingException(TrackingFailure.INCOMPLETE)
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException || error.trackingFailure() == TrackingFailure.ACCESS || result.isEmpty()) throw error
            throw TrackingPagesException(result.values.toList(), error.trackingFailure())
        }
    } ?: throw TrackingException(TrackingFailure.INCOMPLETE)
}

class BetaSeriesApiException(val code: Int) : Exception("BetaSeries error $code")
class TrackingPagesException(val items: List<JSONObject>, failure: TrackingFailure) : TrackingException(failure)
internal fun JSONObject.errorsCode(): Int? = optJSONArray("errors")?.optJSONObject(0)?.optInt("code")?.takeIf { it > 0 }
internal fun JSONObject.text(name: String): String? = optString(name).takeUnless { it.isBlank() || it == "null" }
internal fun JSONObject.requiredId(): String = text("id") ?: throw TrackingException(TrackingFailure.PROTOCOL)
