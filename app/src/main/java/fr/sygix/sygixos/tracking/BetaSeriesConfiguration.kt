/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.net.URI
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

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
        val jsonCandidates = Regex("""JSON\.parse\(\s*("(?:\\.|[^"\\])*"|'(?:\\.|[^'\\])*')\s*\)""").findAll(data).mapNotNull { match ->
            val decoded = decodeLiteral(match.groupValues[1]) ?: return@mapNotNull null
            val json = runCatching { org.json.JSONObject(decoded) }.getOrNull() ?: return@mapNotNull null
            val host = json.optString("apiHost").trimEnd('/')
            val key = json.optString("apiKey")
            if (host == "https://api.betaseries.com" && key.isNotBlank() && key.length <= 512) BetaSeriesConfiguration(host, key) else null
        }
        val objectCandidates = Regex("""\{[^{}]{1,16384}\}""").findAll(data).mapNotNull { block ->
            fun field(name: String) = Regex("""["']?${name}["']?\s*:\s*["']([^"']{1,512})["']""").find(block.value)?.groupValues?.get(1)?.replace("\\/", "/")
            val host = field("apiHost")?.trimEnd('/') ?: return@mapNotNull null
            if (host != "https://api.betaseries.com") return@mapNotNull null
            field("apiKey")?.takeIf { it.isNotBlank() }?.let { BetaSeriesConfiguration(host, it) }
        }
        val candidates = (jsonCandidates + objectCandidates).distinct().take(2).toList()
        return candidates.singleOrNull()
    }

    private fun decodeLiteral(literal: String): String? {
        if (literal.length > 262_144) return null
        val output = StringBuilder()
        var index = 1
        while (index < literal.lastIndex) {
            val character = literal[index++]
            if (character != '\\') { output.append(character); continue }
            if (index >= literal.lastIndex) return null
            when (val escape = literal[index++]) {
                '\\', '\'', '"', '/' -> output.append(escape)
                'n' -> output.append('\n')
                'r' -> output.append('\r')
                't' -> output.append('\t')
                'b' -> output.append('\b')
                'f' -> output.append('\u000c')
                'u', 'x' -> {
                    val digits = if (escape == 'u') 4 else 2
                    if (index + digits > literal.lastIndex) return null
                    val code = literal.substring(index, index + digits).toIntOrNull(16) ?: return null
                    output.append(code.toChar()); index += digits
                }
                else -> return null
            }
        }
        return output.toString()
    }
}

class BetaSeriesConfigurationSource(private val transport: TrackingTransport) {
    private val mutex = Mutex()
    private var cached: BetaSeriesConfiguration? = null

    suspend fun get(): BetaSeriesConfiguration = withTimeoutOrNull(60_000) { mutex.withLock {
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
    } } ?: throw TrackingException(TrackingFailure.CONFIGURATION)

    suspend fun invalidate(previous: BetaSeriesConfiguration) = mutex.withLock { if (cached === previous) cached = null }
}
