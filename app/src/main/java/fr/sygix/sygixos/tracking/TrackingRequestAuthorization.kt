/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext

internal class TrackingSessionChangedException : Exception()

internal class TrackingRequestAuthorization(
    private val allowed: suspend () -> Boolean,
    private val departure: (() -> Unit) -> Unit,
) : AbstractCoroutineContextElement(Key) {
    suspend fun validate() { if (!allowed()) throw TrackingSessionChangedException() }
    fun dispatch(block: () -> Unit) = departure(block)
    companion object Key : CoroutineContext.Key<TrackingRequestAuthorization>
}
