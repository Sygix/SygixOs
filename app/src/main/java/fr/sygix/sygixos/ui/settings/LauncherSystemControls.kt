/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.model.LauncherSystemState

class LauncherSystemActions(
    val openGuide: () -> Unit = {},
    val focusReturned: () -> Unit = {},
    val setup: (fr.sygix.sygixos.ui.home.SetupAction) -> Unit = {},
)

@Composable
internal fun StateLabel(label: String, dot: Color?, color: Color, tag: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.StateDotGap)) {
        dot?.let {
            Box(Modifier.testTag("$tag-dot").size(Dimens.Badge).background(it, CircleShape))
        }
        Text(label, style = TextStyles.RowState, color = color, modifier = Modifier.testTag("$tag-state"))
    }
}

@Composable
internal fun ColumnScope.LauncherSystemControls(
    state: LauncherSystemState,
    actions: LauncherSystemActions,
    focusEnabled: Boolean,
) {
    SettingsGroupTitle(stringResource(R.string.launcher_system_group), "setting-launcher-group")
    val requester = remember { FocusRequester() }
    var handled by remember { mutableLongStateOf(state.settingsFocus) }
    LaunchedEffect(state.settingsFocus, focusEnabled) {
        if (handled == state.settingsFocus || !focusEnabled) return@LaunchedEffect
        handled = state.settingsFocus
        withFrameNanos { }
        requester.tryRequestFocus()
    }
    val tag = "setting-launcher-guide"
    PreferenceRow(
        label = stringResource(R.string.launcher_guide_row),
        enabled = focusEnabled,
        onClick = actions.openGuide,
        focusRequester = requester,
        detail = launcherSummaryDetail(state.defaultLauncher, state),
        modifier = Modifier
            .testTag(tag)
            .onPreviewKeyEvent { it.type == KeyEventType.KeyDown && (it.key == Key.DirectionRight || it.key == Key.DirectionDown) },
    ) { colors ->
        Spacer(Modifier.width(RowGap))
        val p7 = state.defaultLauncher
        StateLabel(stringResource(launcherSummaryLabel(p7, state)), launcherSummaryDot(p7, state), colors.tertiary, tag)
    }
}
