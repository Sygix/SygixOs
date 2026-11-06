/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class TrackingContentCodecTest {
    @Test fun storedAccountContentKeepsItsAliasesAndOlderPayloadsStillDecode() {
        val content = TrackingContent(TrackingIdentity("betaseries", TrackingType.MOVIE, "9"), title = "Fixture", aliases = setOf("Titre localisé"))
        val payload = TrackingContentCodec.encode(content)
        assertEquals(content, TrackingContentCodec.decode(payload))
        val legacy = JSONObject(payload).apply { remove("aliases") }.toString()
        assertEquals(content.copy(aliases = emptySet()), TrackingContentCodec.decode(legacy))
    }
}
