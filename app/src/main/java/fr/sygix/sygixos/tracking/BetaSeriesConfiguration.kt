/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.net.URI
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeout

class BetaSeriesConfiguration(val apiHost: String, val apiKey: String) {
    override fun equals(other: Any?) = other is BetaSeriesConfiguration && apiHost == other.apiHost && apiKey == other.apiKey
    override fun hashCode() = 31 * apiHost.hashCode() + apiKey.hashCode()
    override fun toString() = "BetaSeriesConfiguration(redacted)"
}

object BetaSeriesConfigParser {
    fun scripts(html: String): List<String> = Regex("""<script\b[^>]*\bsrc\s*=\s*["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        .findAll(html).mapNotNull { match -> runCatching { URI("https://www.betaseries.com/").resolve(match.groupValues[1]).toString() }.getOrNull() }
        .filter(::officialTrackingUrl).distinct().take(24).toList()

    fun configuration(data: String): BetaSeriesConfiguration? {
        val candidates = Regex("""\{[^{}]{1,16384}\}""").findAll(data).mapNotNull { block ->
            fun field(name: String) = Regex("""["']?${name}["']?\s*:\s*["']([^"']{1,512})["']""").find(block.value)?.groupValues?.get(1)?.replace("\\/", "/")
            val host = field("apiHost")?.trimEnd('/') ?: return@mapNotNull null
            if (host != "https://api.betaseries.com") return@mapNotNull null
            field("apiKey")?.takeIf { it.isNotBlank() }?.let { BetaSeriesConfiguration(host, it) }
        }.distinct().take(2).toList()
        return candidates.singleOrNull()
    }
}

class BetaSeriesConfigurationSource(private val transport: TrackingTransport) {
    private val mutex = Mutex()
    private var cached: BetaSeriesConfiguration? = null

    suspend fun get(): BetaSeriesConfiguration = withTimeout(60_000) { mutex.withLock {
        cached?.let { return@withLock it }
        val page = transport.execute(TrackingRequest("https://www.betaseries.com/", maxBytes = 10 * 1024 * 1024))
        if (page.status != 200) throw TrackingException(TrackingFailure.CONFIGURATION)
        val direct = BetaSeriesConfigParser.configuration(page.body)
        if (direct != null) { cached = direct; return@withLock direct }
        for (script in BetaSeriesConfigParser.scripts(page.body)) {
            val response = transport.execute(TrackingRequest(script, maxBytes = 10 * 1024 * 1024))
            if (response.status != 200) continue
            BetaSeriesConfigParser.configuration(response.body)?.let { cached = it; return@withLock it }
        }
        throw TrackingException(TrackingFailure.CONFIGURATION)
    } }

    suspend fun invalidate(previous: BetaSeriesConfiguration) = mutex.withLock { if (cached === previous) cached = null }
}
