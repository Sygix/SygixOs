/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.Dp
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.SygixTypography
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.model.SystemControl
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.model.SystemRow

class LauncherSystemActions(
    val activate: (SystemControl) -> Unit = {},
    val dismiss: () -> Unit = {},
    val confirmOverlay: () -> Unit = {},
    val declineOverlay: () -> Unit = {},
    val focusReturned: () -> Unit = {},
)

internal class LauncherRowTags(private val homeRole: String, private val boot: String, private val accessibility: String) {
    fun of(control: SystemControl): String = when (control) {
        SystemControl.HOME_ROLE -> homeRole
        SystemControl.BOOT_START -> boot
        SystemControl.ACCESSIBILITY -> accessibility
    }

    companion object {
        val Onboarding = LauncherRowTags("onboarding-row-home-role", "onboarding-row-boot", "onboarding-row-accessibility")
        val Settings = LauncherRowTags("setting-home-role", "setting-boot-start", "setting-accessibility-home")
    }
}

internal val StateDotColor = SemanticsPropertyKey<Color>("StateDotColor")
private var SemanticsPropertyReceiver.stateDotColor by StateDotColor

internal fun SystemControlState.dotColor(): Color? = when (this) {
    SystemControlState.ACTIVE -> SygixColors.SwitchOn
    SystemControlState.PENDING -> SygixColors.Badge
    SystemControlState.INACTIVE, SystemControlState.UNAVAILABLE -> null
}

@Composable
internal fun ColumnScope.LauncherSystemControls(
    state: LauncherSystemState,
    actions: LauncherSystemActions,
    focusEnabled: Boolean,
) {
    Text(
        stringResource(R.string.launcher_system_group),
        style = SygixTypography.labelMedium,
        color = SygixColors.OnDarkSecondary,
        modifier = Modifier
            .testTag("setting-launcher-group")
            .focusProperties { canFocus = false }
            .padding(start = RowPadding, top = Dimens.SettingsGroupTitleTop, bottom = Dimens.SettingsGroupTitleBottom),
    )
    var focused by remember { mutableIntStateOf(-1) }
    LauncherSystemRows(
        state = state,
        actions = actions,
        tags = LauncherRowTags.Settings,
        spacing = RowSpacing,
        focusEnabled = focusEnabled,
        onFocused = { focused = it },
        modifier = Modifier.onPreviewKeyEvent {
            it.type == KeyEventType.KeyDown && it.key == Key.DirectionDown && focused == state.rows.lastIndex
        },
    )
}

@Composable
internal fun LauncherSystemRows(
    state: LauncherSystemState,
    actions: LauncherSystemActions,
    tags: LauncherRowTags,
    spacing: Dp,
    focusEnabled: Boolean,
    modifier: Modifier = Modifier,
    requesters: List<FocusRequester> = remember { List(state.rows.size) { FocusRequester() } },
    onFocused: (Int) -> Unit = {},
) {
    val rows = state.rows
    LaunchedEffect(state.focusReturn, state.systemPending, focusEnabled) {
        val target = state.focusReturn ?: return@LaunchedEffect
        if (!focusEnabled || state.systemPending) return@LaunchedEffect
        requesters[rows.indexOfFirst { it.control == target }].tryRequestFocus()
        actions.focusReturned()
    }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(spacing)) {
        rows.forEachIndexed { index, row ->
            LauncherControlRow(
                row = row,
                tag = tags.of(row.control),
                focusEnabled = focusEnabled,
                requester = requesters[index],
                onFocused = { onFocused(index) },
                onClick = { actions.activate(row.control) },
            )
        }
    }
}

@Composable
private fun LauncherControlRow(
    row: SystemRow,
    tag: String,
    focusEnabled: Boolean,
    requester: FocusRequester,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    PreferenceRow(
        label = stringResource(row.control.titleRes()),
        enabled = focusEnabled,
        onClick = onClick,
        focusRequester = requester,
        detail = stringResource(row.detail.textRes()),
        partsTag = tag,
        dimmed = row.dimmed,
        onFocused = { if (it) onFocused() },
        modifier = Modifier
            .testTag(tag)
            .onPreviewKeyEvent { it.type == KeyEventType.KeyDown && it.key == Key.DirectionRight }
            .semantics(mergeDescendants = true) {
                row.checked?.let {
                    role = Role.Switch
                    toggleableState = ToggleableState(it)
                }
                onClick { onClick(); true }
            },
    ) { colors ->
        Spacer(Modifier.width(RowGap))
        row.checked?.let { AppleSwitch(it, tag = "$tag-switch") }
        row.state?.let { state ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Dimens.StateDotGap)) {
                state.dotColor()?.let { dot ->
                    Box(Modifier.testTag("$tag-dot").semantics { stateDotColor = dot }.size(Dimens.Badge).background(dot, CircleShape))
                }
                Text(stringResource(state.labelRes()), style = TextStyles.RowState, color = colors.tertiary, modifier = Modifier.testTag("$tag-state"))
            }
        }
    }
}
