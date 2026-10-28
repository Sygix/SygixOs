/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.json.JSONArray
import org.json.JSONObject

internal val TrackingIdentity.storageKey: String get() = "$provider:${type.name}:$id"

internal object TrackingContentCodec {
    fun encode(content: TrackingContent): String = JSONObject().apply {
        put("identity", identity(content.identity)); put("parent", identity(content.parent))
        put("title", content.title); put("episodeTitle", content.episodeTitle)
        put("season", content.season); put("episode", content.episode); put("year", content.year)
        put("image", content.image); put("poster", content.poster); put("duration", content.durationMillis)
        put("externalIds", JSONObject(content.externalIds)); put("seen", content.seen); put("archived", content.archived)
        put("membership", content.membership.name); put("movieState", content.movieState); put("airDate", content.airDate)
        put("failure", content.failure?.name)
        put("removableFromAccount", content.removableFromAccount)
        put("offers", JSONArray().apply { content.offers.forEach { offer -> put(JSONObject().put("id", offer.id).put("name", offer.name)) } })
    }.toString()

    fun decode(payload: String): TrackingContent = JSONObject(payload).let { value ->
        val external = value.getJSONObject("externalIds")
        val offers = value.getJSONArray("offers")
        TrackingContent(identity(value.getJSONObject("identity")), identity(value.getJSONObject("parent")), value.getString("title"),
            episodeTitle = value.text("episodeTitle"), season = value.integer("season"), episode = value.integer("episode"),
            year = value.integer("year"), image = value.text("image"), poster = value.text("poster"),
            durationMillis = value.text("duration")?.toLong(), externalIds = external.keys().asSequence().associateWith { external.getString(it) },
            seen = value.getBoolean("seen"), archived = if (value.has("archived")) value.getBoolean("archived") else null,
            membership = AccountMembership.valueOf(value.getString("membership")), movieState = value.getInt("movieState"),
            offers = (0 until offers.length()).map { offers.getJSONObject(it).let { PlaybackOffer(it.text("id"), it.text("name")) } },
            failure = value.text("failure")?.let(TrackingFailure::valueOf), airDate = value.text("airDate"),
            removableFromAccount = value.optBoolean("removableFromAccount", true))
    }

    private fun identity(value: TrackingIdentity) = JSONObject().put("provider", value.provider).put("type", value.type.name).put("id", value.id)
    private fun identity(value: JSONObject) = TrackingIdentity(value.getString("provider"), TrackingType.valueOf(value.getString("type")), value.getString("id"))
    private fun JSONObject.integer(key: String): Int? = if (has(key)) getInt(key) else null
}
