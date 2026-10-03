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
)

class StartupGate(
    private val timings: StartupTimings,
    private val animationsEnabled: Boolean,
    private val now: () -> Long,
) {
    private var shownAt: Long? = null
    private var catalogAt: Long? = null
    private var visualAt: Long? = null
    private var fadeDone = false

    fun splashShown() {
        if (shownAt == null) shownAt = now()
    }

    fun catalogReady() {
        if (catalogAt == null) catalogAt = now()
    }

    fun heroVisualReady() {
        if (visualAt == null) visualAt = now()
    }

    fun fadeFinished() {
        if (phase() == StartupPhase.FadingOut) fadeDone = true
    }

    fun phase(): StartupPhase {
        val start = shownAt ?: return StartupPhase.Splash
        return when {
            now() - start < fadeStart(start) -> StartupPhase.Splash
            !animationsEnabled || fadeDone -> StartupPhase.Done
            else -> StartupPhase.FadingOut
        }
    }

    fun millisUntilChange(): Long? {
        val start = shownAt ?: return null
        return (fadeStart(start) - (now() - start)).takeIf { it > 0 }
    }

    private fun fadeStart(start: Long): Long {
        val visual = visualAt?.minus(start) ?: Long.MAX_VALUE
        val ready = catalogAt?.let { maxOf(it - start, minOf(visual, timings.visualCapMs)) } ?: Long.MAX_VALUE
        return minOf(maxOf(timings.minMs, ready), timings.capMs)
    }
}
