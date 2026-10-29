/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import fr.sygix.sygixos.model.UpNextItem

data class TrackingOfferResult(val offers: List<PlaybackOffer>, val failure: TrackingFailure? = null)

interface TrackingContentProvider : WatchTrackingProvider {
    val id: String
    suspend fun identify(token: String, item: UpNextItem, known: LocalTrackingIdentity? = null): TrackingContent?
    suspend fun platforms(country: String): RegionalPlatforms
    suspend fun episodes(token: String, content: TrackingContent): List<TrackingContent>
    suspend fun seasons(token: String, content: TrackingContent): List<Int>
    suspend fun mutate(token: String, content: TrackingContent, action: TrackingAction): List<TrackingContent>
    suspend fun sendMutation(token: String, content: TrackingContent, action: TrackingAction)
    suspend fun readTarget(token: String, content: TrackingContent, action: TrackingAction): TrackingContent
    suspend fun verify(token: String, content: TrackingContent, action: TrackingAction): List<TrackingContent>
    suspend fun reserve(token: String, content: TrackingContent, desired: Map<String, Int>): List<TrackingContent>
    fun matches(entry: TrackingContent, action: TrackingAction, original: TrackingContent): Boolean
    suspend fun offers(token: String, content: TrackingContent, country: String): TrackingOfferResult = TrackingOfferResult(content.offers)
    fun confirmOffers(offers: List<PlaybackOffer>, region: RegionalPlatforms?): List<PlaybackOffer> = offers.map { offer ->
        offer.copy(confirmedCountry = region?.country?.takeIf { offer.id in region.ids })
    }
}
