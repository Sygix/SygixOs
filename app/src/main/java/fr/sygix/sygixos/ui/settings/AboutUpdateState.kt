/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.runtime.Immutable
import fr.sygix.sygixos.domain.CheckLine
import fr.sygix.sygixos.domain.InstallLine
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.domain.UpdateCandidate
import fr.sygix.sygixos.domain.UpdateStatus
import fr.sygix.sygixos.domain.UpdateStatusText
import fr.sygix.sygixos.domain.UpdateUrlPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Immutable
data class AboutUpdateState(
    val checkLine: CheckLine = CheckLine.NeverChecked,
    val installLine: InstallLine? = null,
    val withdrawn: UpdateCandidate? = null,
    val includePrereleases: Boolean = false,
    val releaseNotesQr: QrCode? = null,
    val badge: Boolean = false,
    val systemScreenReturns: Int = 0,
)

@Immutable
data class UpdateActions(
    val onCheck: () -> Unit = {},
    val onInstall: (String) -> Unit = {},
    val onTogglePrereleases: () -> Unit = {},
)

internal class AboutUpdateMapper(
    private val encode: (String) -> QrCode? = QrCode::encode,
    private val encoding: CoroutineDispatcher = Dispatchers.Default,
) {

    private var lastUrl: String? = null
    private var lastQr: QrCode? = null

    suspend fun map(status: UpdateStatus, systemScreenReturns: Int): AboutUpdateState {
        val notesUrl = status.proposed?.htmlUrl?.takeIf(UpdateUrlPolicy::isReleasePage)
        return AboutUpdateState(
            checkLine = UpdateStatusText.checkLine(status),
            installLine = UpdateStatusText.installLine(status),
            withdrawn = UpdateStatusText.withdrawn(status),
            includePrereleases = status.includePrereleases,
            releaseNotesQr = notesUrl?.let { qrFor(it) },
            badge = status.badge,
            systemScreenReturns = systemScreenReturns,
        )
    }

    private suspend fun qrFor(url: String): QrCode? {
        if (url != lastUrl) {
            lastQr = withContext(encoding) { encode(url) }
            lastUrl = url
        }
        return lastQr
    }
}
