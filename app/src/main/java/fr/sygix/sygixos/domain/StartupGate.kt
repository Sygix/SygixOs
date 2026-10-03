/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

enum class StartupPhase { Splash, FadingOut, Done }

data class StartupTimings(
    val minMs: Long,
    val visualCapMs: Long,
    val capMs: Long,
    val fadeMs: Long,
)

class StartupGate(
    private val timings: StartupTimings,
    private val animationsEnabled: Boolean,
    private val now: () -> Long,
) {
    private var shownAt: Long? = null
    private var catalogAt: Long? = null
    private var visualAt: Long? = null

    fun splashShown() {
        if (shownAt == null) shownAt = now()
    }

    fun catalogReady() {
        if (catalogAt == null) catalogAt = now()
    }

    fun heroVisualReady() {
        if (visualAt == null) visualAt = now()
    }

    fun phase(): StartupPhase {
        val start = shownAt ?: return StartupPhase.Splash
        val elapsed = now() - start
        val fadeAt = fadeStart(start)
        return when {
            elapsed < fadeAt -> StartupPhase.Splash
            elapsed < fadeEnd(fadeAt) -> StartupPhase.FadingOut
            else -> StartupPhase.Done
        }
    }

    fun millisUntilChange(): Long? {
        val start = shownAt ?: return null
        val elapsed = now() - start
        val fadeAt = fadeStart(start)
        return listOf(fadeAt, fadeEnd(fadeAt)).firstOrNull { it > elapsed }?.minus(elapsed)
    }

    private fun fadeEnd(fadeAt: Long): Long = if (animationsEnabled) fadeAt + timings.fadeMs else fadeAt

    private fun fadeStart(start: Long): Long {
        val visual = visualAt?.minus(start) ?: Long.MAX_VALUE
        val ready = catalogAt?.let { maxOf(it - start, minOf(visual, timings.visualCapMs)) } ?: Long.MAX_VALUE
        return minOf(maxOf(timings.minMs, ready), timings.capMs)
    }
}
