/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.net.URI
import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
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
    transport: TrackingTransport,
    file: File?,
    clock: () -> Long,
    dispatcher: CoroutineDispatcher,
) {
    private val source = TrackingApplicationConfiguration(file, dispatcher,
        decode = { value -> value.takeIf { it.text("apiHost") == "https://api.betaseries.com" }?.text("apiKey")
            ?.takeIf { it.length <= 512 }?.let { BetaSeriesConfiguration("https://api.betaseries.com", it) } },
        encode = { value: BetaSeriesConfiguration -> JSONObject().put("apiHost", value.apiHost).put("apiKey", value.apiKey).put("fetchedAt", clock()) },
        discover = {
            val resources = TrackingOfficialResources(transport, "www.betaseries.com")
            val page = resources.page()
            var found = BetaSeriesConfigParser.configuration(page)
            if (found == null) for (script in BetaSeriesConfigParser.scripts(page)) {
                found = BetaSeriesConfigParser.configuration(resources.script("https://www.betaseries.com/", script))
                if (found != null) break
            }
            found ?: throw TrackingException(TrackingFailure.CONFIGURATION)
        })

    suspend fun get(): BetaSeriesConfiguration = source.get()
    suspend fun invalidate(previous: BetaSeriesConfiguration) = source.invalidate(previous)
}
