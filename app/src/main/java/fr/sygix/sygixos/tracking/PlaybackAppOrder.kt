/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.text.Collator
import java.text.Normalizer
import java.util.Locale

object PlaybackAppOrder {
    fun sort(apps: List<PlaybackApp>, publishers: Set<String>, offers: List<PlaybackOffer>): List<PlaybackApp> {
        val confirmed = offers.filter { it.confirmedCountry != null }.mapNotNull { normalize(it.name) }
        val mentioned = offers.filter { it.confirmedCountry == null }.mapNotNull { normalize(it.name) }
        val collator = Collator.getInstance(Locale.FRENCH)
        return apps.sortedWith(compareBy<PlaybackApp> { app ->
            val label = normalize(app.label)
            when {
                app.packageName in publishers -> 0
                confirmed.any { matches(label, it) } -> 1
                mentioned.any { matches(label, it) } -> 2
                else -> 3
            }
        }.thenBy(collator) { it.label })
    }

    private fun matches(label: String?, offer: String): Boolean =
        label != null && (label == offer || minOf(label.length, offer.length) >= 4 && (label in offer || offer in label))

    private fun normalize(value: String?): String? = value?.let { Normalizer.normalize(it.replace("+", " plus "), Normalizer.Form.NFD) }
        ?.lowercase(Locale.ROOT)?.filter { it in 'a'..'z' || it in '0'..'9' }?.takeIf { it.length >= 2 }
}
