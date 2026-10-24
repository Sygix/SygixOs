/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackAppOrderTest {
    private val apps = listOf(
        PlaybackApp("fixture.films", "Play Films", true),
        PlaybackApp("fixture.francetv", "france.tv", true),
        PlaybackApp("fixture.nova", "Nova", true),
        PlaybackApp("org.jellyfin.androidtv", "Jellyfin", true),
        PlaybackApp("fixture.netflix", "Netflix", true),
        PlaybackApp("fixture.prime", "Prime Video", true),
        PlaybackApp("fixture.disney", "Disney+", true),
    )

    @Test fun publishersThenConfirmedThenMentionedServicesThenAlphabetical() {
        val offers = listOf(PlaybackOffer("1", "Netflix", confirmedCountry = "FR"), PlaybackOffer("2", "Amazon Prime Video"), PlaybackOffer("3", "Disney Plus", confirmedCountry = "FR"))
        assertEquals(listOf("org.jellyfin.androidtv", "fixture.disney", "fixture.netflix", "fixture.prime", "fixture.francetv", "fixture.nova", "fixture.films"),
            PlaybackAppOrder.sort(apps, setOf("org.jellyfin.androidtv"), offers).map { it.packageName })
    }

    @Test fun mentionedServicesAndPublishersAreOfferedEvenWithoutTheVideoCategory() {
        val installed = listOf(
            PlaybackApp("fixture.francetv", "france.tv", true),
            PlaybackApp("fixture.netflix", "Netflix", false),
            PlaybackApp("fixture.prime", "Prime Video", false),
            PlaybackApp("fixture.publisher", "Médiathèque", false),
            PlaybackApp("fixture.music", "Spotify", false),
        )
        val offers = listOf(PlaybackOffer("1", "Netflix"), PlaybackOffer("2", "Amazon Prime Video"))
        assertEquals(listOf("fixture.publisher", "fixture.netflix", "fixture.prime", "fixture.francetv"),
            PlaybackAppOrder.sort(installed, setOf("fixture.publisher"), offers, includeAll = false).map { it.packageName })
        assertEquals(5, PlaybackAppOrder.sort(installed, emptySet(), offers, includeAll = true).size)
    }

    @Test fun withoutPublisherOrOfferTheOrderIsAlphabetical() {
        assertEquals(listOf("Disney+", "france.tv", "Jellyfin", "Netflix", "Nova", "Play Films", "Prime Video"),
            PlaybackAppOrder.sort(apps.reversed(), emptySet(), emptyList()).map { it.label })
    }
}
