/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.domain

sealed interface UpdateStep {
    val candidate: UpdateCandidate

    data class Downloading(override val candidate: UpdateCandidate, val percent: Int) : UpdateStep
    data class Verifying(override val candidate: UpdateCandidate) : UpdateStep
    data class Installing(override val candidate: UpdateCandidate) : UpdateStep
    data class Failed(override val candidate: UpdateCandidate, val error: UpdateError) : UpdateStep

    val inProgress: Boolean get() = this !is Failed
}

data class UpdateStatus(
    val checking: Boolean = false,
    val includePrereleases: Boolean = false,
    val proposed: UpdateCandidate? = null,
    val lastResult: CheckResult? = null,
    val step: UpdateStep? = null,
) {
    val badge: Boolean get() = proposed != null
}

sealed interface CheckLine {
    data object NeverChecked : CheckLine
    data object Checking : CheckLine
    data object UpToDate : CheckLine
    data class Available(val versionName: String, val prerelease: Boolean) : CheckLine
    data class Failed(val error: UpdateError) : CheckLine
}

sealed interface InstallDetail {
    data class Downloading(val percent: Int) : InstallDetail
    data object Verifying : InstallDetail
    data object Installing : InstallDetail
    data class Failed(val error: UpdateError) : InstallDetail
}

data class InstallLine(val candidate: UpdateCandidate, val detail: InstallDetail?)

object UpdateStatusText {

    fun checkLine(status: UpdateStatus): CheckLine {
        val proposed = status.proposed
        val last = status.lastResult
        return when {
            status.checking -> CheckLine.Checking
            proposed != null -> CheckLine.Available(proposed.versionName, proposed.prerelease)
            last is CheckResult.Ok -> CheckLine.UpToDate
            last is CheckResult.Error -> CheckLine.Failed(last.error)
            else -> CheckLine.NeverChecked
        }
    }

    fun installLine(status: UpdateStatus): InstallLine? {
        val step = status.step
        val proposed = status.proposed
        return when {
            step != null && step.inProgress -> InstallLine(step.candidate, detailOf(step))
            proposed == null -> null
            step != null && step.candidate.tag == proposed.tag -> InstallLine(proposed, detailOf(step))
            else -> InstallLine(proposed, null)
        }
    }

    fun withdrawn(status: UpdateStatus): UpdateCandidate? =
        (status.step as? UpdateStep.Failed)?.takeIf { it.error == UpdateError.Withdrawn }?.candidate

    private fun detailOf(step: UpdateStep): InstallDetail = when (step) {
        is UpdateStep.Downloading -> InstallDetail.Downloading(step.percent)
        is UpdateStep.Verifying -> InstallDetail.Verifying
        is UpdateStep.Installing -> InstallDetail.Installing
        is UpdateStep.Failed -> InstallDetail.Failed(step.error)
    }
}
