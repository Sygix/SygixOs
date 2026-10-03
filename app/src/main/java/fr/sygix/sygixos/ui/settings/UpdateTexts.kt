/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import fr.sygix.sygixos.domain.CheckLine
import fr.sygix.sygixos.domain.InstallDetail
import fr.sygix.sygixos.domain.InstallFailure
import fr.sygix.sygixos.domain.UpdateError
import java.util.Date

@Composable
internal fun checkLineText(line: CheckLine): String = when (line) {
    CheckLine.NeverChecked -> stringResource(R.string.update_never_checked)
    CheckLine.Checking -> stringResource(R.string.update_checking)
    CheckLine.UpToDate -> stringResource(R.string.update_up_to_date)
    is CheckLine.Available -> stringResource(
        if (line.prerelease) R.string.update_prerelease_available else R.string.update_available,
        line.versionName,
    )
    is CheckLine.Failed -> updateErrorText(line.error)
}

@Composable
internal fun installDetailText(detail: InstallDetail): String = when (detail) {
    is InstallDetail.Downloading -> stringResource(R.string.update_step_downloading, detail.percent)
    InstallDetail.Verifying -> stringResource(R.string.update_step_verifying)
    InstallDetail.Installing -> stringResource(R.string.update_step_installing)
    is InstallDetail.Failed -> updateErrorText(detail.error)
}

@Composable
internal fun updateErrorText(error: UpdateError): String = when (error) {
    UpdateError.NoNetwork -> stringResource(R.string.update_error_no_network)
    UpdateError.Timeout -> stringResource(R.string.update_error_timeout)
    is UpdateError.RateLimited -> stringResource(R.string.update_error_rate_limited, localTime(error.retryAt))
    is UpdateError.Unavailable -> stringResource(R.string.update_error_unavailable)
    UpdateError.Unreadable -> stringResource(R.string.update_error_unreadable)
    UpdateError.Withdrawn -> stringResource(R.string.update_error_withdrawn)
    UpdateError.AssetNotFound -> stringResource(R.string.update_error_asset_not_found)
    UpdateError.Corrupt -> stringResource(R.string.update_error_corrupt)
    UpdateError.SignatureMismatch -> stringResource(R.string.update_error_signature)
    UpdateError.Inconsistent -> stringResource(R.string.update_error_inconsistent)
    UpdateError.Interrupted -> stringResource(R.string.update_error_interrupted)
    UpdateError.NoSpace -> stringResource(R.string.update_error_no_space)
    UpdateError.InstallAborted -> stringResource(R.string.update_error_aborted)
    is UpdateError.InstallFailed -> stringResource(R.string.update_error_install_failed, failureText(error.family))
    UpdateError.SystemScreenUnavailable -> stringResource(R.string.update_error_system_screen)
}

@Composable
private fun failureText(family: InstallFailure): String = stringResource(
    when (family) {
        InstallFailure.BLOCKED -> R.string.update_failure_blocked
        InstallFailure.CONFLICT -> R.string.update_failure_conflict
        InstallFailure.INCOMPATIBLE -> R.string.update_failure_incompatible
        InstallFailure.INVALID -> R.string.update_failure_invalid
        InstallFailure.STORAGE -> R.string.update_failure_storage
        InstallFailure.TIMEOUT -> R.string.update_failure_timeout
        InstallFailure.OTHER -> R.string.update_failure_other
    },
)

@Composable
private fun localTime(epochMillis: Long): String = DateFormat.getTimeFormat(LocalContext.current).format(Date(epochMillis))
