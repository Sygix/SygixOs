/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.io.File
import kotlinx.coroutines.CoroutineDispatcher
import org.json.JSONObject

class TraktConfiguration(val clientId: String) {
    override fun equals(other: Any?) = other is TraktConfiguration && clientId == other.clientId
    override fun hashCode() = clientId.hashCode()
    override fun toString() = "TraktConfiguration(redacted)"
}

object TraktConfigParser {
    private val entry = Regex("""import\(["']([^"']*/entry/app\.[^"']+\.js)["']\)""")
    private val root = Regex("""["']([^"']*nodes/0\.[^"']+\.js)["']""")
    private val client = Regex("""trakt-api-key=([a-fA-F0-9]{64})(?![a-fA-F0-9])""")

    fun entry(html: String): String? = unique(entry, html)
    fun root(script: String): String? = unique(root, script)
    fun configuration(script: String): TraktConfiguration? = unique(client, script)?.let(::TraktConfiguration)
    private fun unique(pattern: Regex, text: String): String? = pattern.findAll(text).map { it.groupValues[1] }.distinct().take(2).toList().singleOrNull()
}

class TraktConfigurationSource(transport: TrackingTransport, file: File?, clock: () -> Long, dispatcher: CoroutineDispatcher) {
    private val source = TrackingApplicationConfiguration(file, dispatcher,
        decode = { value -> value.text("clientId")?.takeIf { it.matches(Regex("[a-fA-F0-9]{64}")) }?.let(::TraktConfiguration) },
        encode = { value: TraktConfiguration -> JSONObject().put("clientId", value.clientId).put("fetchedAt", clock()) },
        discover = {
            val resources = TrackingOfficialResources(transport, "app.trakt.tv")
            val page = resources.page()
            val entry = TraktConfigParser.entry(page) ?: throw TrackingException(TrackingFailure.CONFIGURATION)
            val entryUrl = resources.resolve("https://app.trakt.tv/", entry)
            val script = resources.script("https://app.trakt.tv/", entry)
            val root = TraktConfigParser.root(script) ?: throw TrackingException(TrackingFailure.CONFIGURATION)
            TraktConfigParser.configuration(resources.script(entryUrl, root)) ?: throw TrackingException(TrackingFailure.CONFIGURATION)
        })

    suspend fun get(): TraktConfiguration = source.get()
    suspend fun invalidate(previous: TraktConfiguration) = source.invalidate(previous)
}
