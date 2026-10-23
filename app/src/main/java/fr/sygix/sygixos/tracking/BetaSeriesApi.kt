/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.net.URLEncoder
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

class BetaSeriesApi(
    private val transport: TrackingTransport,
    private val configuration: BetaSeriesConfigurationSource,
) {
    suspend fun request(path: String, token: String? = null, method: String = "GET", fields: Map<String, String> = emptyMap()): JSONObject {
        require(path.matches(Regex("[a-z_]+/[a-z_]+")))
        val config = configuration.get()
        val result = execute(config, path, token, method, fields)
        if (result.errorsCode() == 1001 && method == "GET") {
            configuration.invalidate(config)
            return checked(execute(configuration.get(), path, token, method, fields))
        }
        return checked(result)
    }

    private suspend fun execute(config: BetaSeriesConfiguration, path: String, token: String?, method: String, fields: Map<String, String>): JSONObject {
        val parameters = if (path.startsWith("oauth/")) fields + ("client_id" to config.apiKey) else fields
        val query = if (method == "GET" && parameters.isNotEmpty()) parameters.entries.joinToString("&", "?") { (key, value) -> "$key=${URLEncoder.encode(value, "UTF-8")}" } else ""
        val headers = mutableMapOf("Accept" to "application/json", "X-BetaSeries-Version" to "3.0", "X-BetaSeries-Key" to config.apiKey)
        if (token != null) headers["X-BetaSeries-Token"] = token
        val response = transport.execute(TrackingRequest("${config.apiHost}/$path$query", method, headers, parameters.takeUnless { method == "GET" } ?: emptyMap()))
        val json = try { JSONObject(response.body) } catch (_: Exception) { throw TrackingException(TrackingFailure.PROTOCOL) }
        if (response.status in setOf(401, 403) && json.errorsCode() != 1001) throw TrackingException(TrackingFailure.ACCESS)
        if (response.status !in 200..299 && json.errorsCode() == null) throw TrackingException(TrackingFailure.NETWORK)
        return json
    }

    private fun checked(value: JSONObject): JSONObject {
        val code = value.errorsCode() ?: return value
        throw BetaSeriesApiException(code)
    }

    suspend fun pages(path: String, key: String, token: String, fields: Map<String, String> = emptyMap()): List<JSONObject> = withTimeout(90_000) {
        val result = linkedMapOf<String, JSONObject>()
        var previous: Set<String> = emptySet()
        for (page in 1..200) {
            val json = request(path, token, fields = fields + mapOf("page" to page.toString(), "limit" to "100"))
            val array = json.optJSONArray(key) ?: throw TrackingException(TrackingFailure.PROTOCOL)
            val batch = (0 until array.length()).map { array.getJSONObject(it) }
            val ids = batch.map { it.requiredId() }.toSet()
            if (batch.isNotEmpty() && ids == previous) throw TrackingException(TrackingFailure.INCOMPLETE)
            previous = ids
            batch.forEach { result[it.requiredId()] = it }
            val missingKey = when (key) { "shows" -> "totalMissingShows"; "movies" -> "totalMissingMovies"; else -> "totalMissingEpisodes" }
            val total = json.optInt("total", -1)
            if (json.has(missingKey) && json.optInt(missingKey, -1) == 0 || total >= 0 && result.size >= total) return@withTimeout result.values.toList()
            if (batch.isEmpty()) {
                if (json.optInt(missingKey, 0) > 0 || total > result.size) throw TrackingException(TrackingFailure.INCOMPLETE)
                return@withTimeout result.values.toList()
            }
        }
        throw TrackingException(TrackingFailure.INCOMPLETE)
    }
}

class BetaSeriesApiException(val code: Int) : Exception("BetaSeries error $code")
internal fun JSONObject.errorsCode(): Int? = optJSONArray("errors")?.optJSONObject(0)?.optInt("code")?.takeIf { it > 0 }
internal fun JSONObject.text(name: String): String? = optString(name).takeUnless { it.isBlank() || it == "null" }
internal fun JSONObject.requiredId(): String = text("id") ?: throw TrackingException(TrackingFailure.PROTOCOL)
