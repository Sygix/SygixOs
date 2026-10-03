/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

object DailyCheckPolicy {

    const val INTERVAL_MS = 24L * 60 * 60 * 1000

    fun isDue(now: Long, lastCheckAt: Long?, retryAt: Long?): Boolean {
        if (!canRequest(now, retryAt)) return false
        return lastCheckAt == null || lastCheckAt > now || now - lastCheckAt >= INTERVAL_MS
    }

    fun canRequest(now: Long, retryAt: Long?): Boolean = retryAt == null || now >= retryAt
}
