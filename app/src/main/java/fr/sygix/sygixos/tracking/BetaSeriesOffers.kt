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
        return entries.map(::offer).distinct()
    }

    fun movie(value: JSONObject): List<PlaybackOffer> = objects(value.optJSONArray("platforms_svod")).map(::offer).distinct()

    fun confirm(offers: List<PlaybackOffer>, region: RegionalPlatforms?): List<PlaybackOffer> = offers.map { it.copy(confirmedCountry = region?.country?.takeIf { _ -> it.id in region.ids }) }

    private fun offer(value: JSONObject) = PlaybackOffer(value.text("id"), value.text("name"))

    private fun objects(array: JSONArray?): List<JSONObject> = if (array == null) emptyList() else (0 until array.length()).mapNotNull(array::optJSONObject)
}
