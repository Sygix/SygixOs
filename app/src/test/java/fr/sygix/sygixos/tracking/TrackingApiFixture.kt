/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.StandardTestDispatcher

internal fun TestScope.trackingApi(transport: TrackingTransport): BetaSeriesApi {
    val dispatcher = StandardTestDispatcher(testScheduler)
    return BetaSeriesApi(transport, BetaSeriesConfigurationSource(transport, null, { testScheduler.currentTime }, dispatcher), dispatcher)
}
