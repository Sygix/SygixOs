/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.net.URI
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject

class BetaSeriesConfiguration(val apiHost: String, val apiKey: String) {
    override fun equals(other: Any?) = other is BetaSeriesConfiguration && apiHost == other.apiHost && apiKey == other.apiKey
    override fun hashCode() = 31 * apiHost.hashCode() + apiKey.hashCode()
    override fun toString() = "BetaSeriesConfiguration(redacted)"
}

object BetaSeriesConfigParser {
    private val script = Regex("""<script\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
    private val key = Regex("""["']?apiKey["']?\s*:\s*["']([^"'\\]{1,512})["']""")
    fun scripts(html: String): List<String> = script.findAll(html).mapNotNull { match ->
        runCatching { URI("https://www.betaseries.com/").resolve(match.groupValues[1]).toString() }.getOrNull()
    }.filter { officialTrackingUrl(it) && URI(it).host == "www.betaseries.com" }.distinct().take(6).toList()

    fun configuration(data: String): BetaSeriesConfiguration? {
        val candidates = linkedSetOf<BetaSeriesConfiguration>()
        for (marker in listOf("\"apiHost\":\"https://api.betaseries.com\"", "apiHost:\"https://api.betaseries.com\"")) {
            var offset = 0
            while (true) {
                val index = data.indexOf(marker, offset)
                if (index < 0) break
                offset = index + marker.length
                val start = data.lastIndexOf('{', index).coerceAtLeast(index - 16_384).coerceAtLeast(0)
                val end = data.indexOf('}', offset).takeIf { it >= 0 } ?: data.length
                val block = data.substring(start, minOf(end, start + 16_384))
                key.find(block)?.groupValues?.get(1)?.takeIf { it.isNotBlank() }?.let { candidates += BetaSeriesConfiguration("https://api.betaseries.com", it) }
                if (candidates.size > 1) return null
            }
        }
        return candidates.singleOrNull()
    }
}

class BetaSeriesConfigurationSource(
    private val transport: TrackingTransport,
    private val file: File?,
    private val clock: () -> Long,
    private val dispatcher: CoroutineDispatcher,
) {
    private val mutex = Mutex()
    private var cached: BetaSeriesConfiguration? = null
    private var fetchedAt = 0L
    private val ttl = 7 * 24 * 60 * 60_000L

    suspend fun get(): BetaSeriesConfiguration = withContext(dispatcher) {
        withTimeoutOrNull(30_000) { mutex.withLock {
            if (cached == null && file?.exists() == true) {
                val json = runCatching { JSONObject(file.readText()) }.getOrNull()
                val timestamp = json?.optLong("fetchedAt") ?: 0
                val config = json?.takeIf { it.text("apiHost") == "https://api.betaseries.com" }?.text("apiKey")?.takeIf { it.length <= 512 }
                    ?.let { BetaSeriesConfiguration("https://api.betaseries.com", it) }
                if (config != null && clock() - timestamp in 0 until ttl) { cached = config; fetchedAt = timestamp }
            }
            cached?.takeIf { clock() - fetchedAt in 0 until ttl }?.let { return@withLock it }
            val page = transport.execute(TrackingRequest("https://www.betaseries.com/", maxBytes = 10 * 1024 * 1024))
            if (page.status != 200) throw TrackingException(TrackingFailure.CONFIGURATION)
            var found = BetaSeriesConfigParser.configuration(page.body)
            if (found == null) for (script in BetaSeriesConfigParser.scripts(page.body)) {
                val response = transport.execute(TrackingRequest(script, maxBytes = 10 * 1024 * 1024))
                if (response.status == 200) found = BetaSeriesConfigParser.configuration(response.body)
                if (found != null) break
            }
            val result = found ?: throw TrackingException(TrackingFailure.CONFIGURATION)
            fetchedAt = clock()
            file?.let {
                val atomic = android.util.AtomicFile(it)
                val output = atomic.startWrite()
                try {
                    output.write(JSONObject().put("apiHost", result.apiHost).put("apiKey", result.apiKey).put("fetchedAt", fetchedAt).toString().toByteArray())
                    atomic.finishWrite(output)
                } catch (error: Exception) { atomic.failWrite(output); throw TrackingException(TrackingFailure.CONFIGURATION) }
            }
            cached = result
            result
        } } ?: throw TrackingException(TrackingFailure.CONFIGURATION)
    }

    suspend fun invalidate(previous: BetaSeriesConfiguration) = withContext(dispatcher) {
        mutex.withLock { if (cached == previous) { cached = null; file?.let { android.util.AtomicFile(it).delete() } } }
    }
}
