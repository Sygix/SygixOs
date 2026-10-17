/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import fr.sygix.sygixos.core.designsystem.PillColors
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable

@Composable
internal fun PreferenceRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selected: Boolean = false,
    detail: String? = null,
    dimmed: Boolean = false,
    onFocused: (Boolean) -> Unit = {},
    leading: @Composable () -> Unit = {},
    trailing: @Composable (PillColors) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused && !selected, selected)
    Row(
        modifier
            .fillMaxWidth()
            .tvFocusable(focusRequester = focusRequester, enabled = enabled, onFocused = { focused = it; onFocused(it) })
            .tvClickable(onClick = onClick)
            .focusPill(colors, RowShape)
            .heightIn(min = RowHeight)
            .padding(horizontal = RowPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        leading()
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = if (focused) TextStyles.RowFocused else TextStyles.Row,
                color = if (dimmed && !focused) SygixColors.OnDarkSecondary else colors.content,
            )
            detail?.let { Text(it, style = TextStyles.RowSecondary, color = colors.secondary) }
        }
        trailing(colors)
    }
}

internal fun Modifier.switchSemantics(checked: Boolean, onToggle: () -> Unit, state: String? = null): Modifier = semantics(mergeDescendants = true) {
    role = Role.Switch
    toggleableState = ToggleableState(checked)
    state?.let { stateDescription = it }
    onClick { onToggle(); true }
}

@Composable
internal fun SwitchPreferenceRow(
    label: String,
    checked: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    tag: String,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
) {
    PreferenceRow(
        label = label,
        enabled = enabled,
        onClick = onToggle,
        focusRequester = focusRequester,
        modifier = modifier.testTag(tag).switchSemantics(checked, onToggle),
    ) {
        AppleSwitch(checked = checked, tag = "$tag-switch")
    }
}
