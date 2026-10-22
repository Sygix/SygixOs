/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope

internal fun TestScope.virtualQueries(): ProviderQueries =
    ProviderQueries(ArtworkScheduler(3, 32, virtualExecutors(StandardTestDispatcher(testScheduler))))

internal fun TestScope.blockingQueries(): ProviderQueries {
    val threads = ArtworkExecutors.threads(3)
    val timer = virtualExecutors(StandardTestDispatcher(testScheduler))
    return ProviderQueries(ArtworkScheduler(3, 32, ArtworkExecutors(threads.workers, threads.cancellations, timer.schedule) { threads.shutdown(); timer.shutdown() }))
}

internal fun threadedQueries(): ProviderQueries = ProviderQueries(ArtworkScheduler(3, 32, ManualArtworkTimer().executors(3)))
