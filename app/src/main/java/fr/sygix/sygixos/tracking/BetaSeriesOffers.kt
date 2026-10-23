/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.json.JSONArray
import org.json.JSONObject

object BetaSeriesOffers {
    fun show(value: JSONObject): List<PlaybackOffer> {
        val platforms = value.optJSONObject("platforms") ?: return emptyList()
        val entries = objects(platforms.optJSONArray("svods")) + listOfNotNull(platforms.optJSONObject("svod"))
        return entries.map { entry ->
            val available = entry.optJSONObject("available")
            val first = available?.optInt("first", -1)?.takeIf { it >= 0 }
            val last = available?.optInt("last", -1)?.takeIf { it >= 0 }
            offer(entry, if (first != null || last != null) OfferScope.SEASONS else OfferScope.SHOW).copy(firstSeason = first, lastSeason = last)
        }.distinct()
    }

    fun movie(value: JSONObject): List<PlaybackOffer> = objects(value.optJSONArray("platforms_svod")).map { offer(it, OfferScope.MOVIE) }.distinct()

    private fun offer(value: JSONObject, scope: OfferScope) = PlaybackOffer(value.text("id"), value.text("name"),
        value.text("logo")?.takeIf(::safeUrl), value.text("link_url")?.takeIf(::safeUrl), scope)

    private fun objects(array: JSONArray?): List<JSONObject> = if (array == null) emptyList() else (0 until array.length()).mapNotNull(array::optJSONObject)
    private fun safeUrl(value: String): Boolean = runCatching { java.net.URI(value).let { it.scheme == "https" && !it.host.isNullOrBlank() && it.userInfo == null } }.getOrDefault(false)
}
