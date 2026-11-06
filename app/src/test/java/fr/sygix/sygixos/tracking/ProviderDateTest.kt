/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProviderDateTest {
    @Test fun `provider timestamps keep their calendar day`() {
        assertEquals(LocalDate.of(2026, 3, 4), ProviderDate.day("2026-03-04T21:30:00.000Z"))
        assertEquals(LocalDate.of(2026, 3, 4), ProviderDate.day("2026-03-04"))
    }

    @Test fun `missing or malformed provider dates are unknown`() {
        assertNull(ProviderDate.day(null))
        assertNull(ProviderDate.day("0000-00-00"))
        assertNull(ProviderDate.day("soon"))
    }
}
