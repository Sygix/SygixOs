/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.model.*

fun launcherSummaryLabel(state: DefaultLauncherState, system: LauncherSystemState): Int = when {
    state.automatic == AutomaticHomeState.ACTIVE || state.manual(system) == ManualHomeState.ACTIVE -> R.string.setup_state_active_summary
    state.manual(system) == ManualHomeState.BLOCKED -> R.string.setup_state_blocked
    state.manual(system) == ManualHomeState.PARTIAL -> R.string.setup_state_partial
    else -> R.string.setup_state_configure
}

fun launcherSummaryDot(state: DefaultLauncherState, system: LauncherSystemState) =
    if (launcherSummaryLabel(state, system) == R.string.setup_state_active_summary) SygixColors.SwitchOn else null

@Composable
internal fun launcherSummaryDetail(state: DefaultLauncherState, system: LauncherSystemState): String = stringResource(
    R.string.launcher_summary_routes,
    stringResource(when (state.automatic) {
        AutomaticHomeState.ACTIVE -> R.string.launcher_summary_auto_active
        AutomaticHomeState.TODO -> R.string.launcher_summary_auto_todo
        AutomaticHomeState.UNAVAILABLE -> R.string.launcher_summary_auto_unavailable
    }),
    stringResource(when (state.manual(system)) {
        ManualHomeState.ACTIVE -> R.string.launcher_summary_manual_active
        ManualHomeState.PARTIAL -> R.string.launcher_summary_manual_partial
        ManualHomeState.BLOCKED -> R.string.launcher_summary_manual_blocked
        ManualHomeState.UNUSED -> R.string.launcher_summary_manual_unused
        ManualHomeState.TODO -> R.string.launcher_summary_manual_todo
    }),
)
