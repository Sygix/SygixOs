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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import fr.sygix.sygixos.core.designsystem.PillColors
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.pillColors
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable

internal val RowTitleColor = SemanticsPropertyKey<Color>("RowTitleColor")
private var SemanticsPropertyReceiver.rowTitleColor by RowTitleColor

internal fun preferenceLabelColor(focused: Boolean, selected: Boolean = false, dimmed: Boolean = false): Color =
    if (dimmed && !focused) SygixColors.OnDarkSecondary else pillColors(focused && !selected, selected).content

@Composable
internal fun PreferenceRow(
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    selected: Boolean = false,
    detail: String? = null,
    partsTag: String? = null,
    dimmed: Boolean = false,
    onFocused: (Boolean) -> Unit = {},
    trailing: @Composable (PillColors) -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused && !selected, selected)
    val labelColor = preferenceLabelColor(focused, selected, dimmed)
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
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = if (focused) TextStyles.RowFocused else TextStyles.Row,
                color = if (dimmed && !focused) labelColor else colors.content,
                modifier = Modifier.partTag(partsTag, "title").semantics { rowTitleColor = labelColor },
            )
            detail?.let { Text(it, style = TextStyles.RowSecondary, color = colors.secondary, modifier = Modifier.partTag(partsTag, "detail")) }
        }
        trailing(colors)
    }
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
        modifier = modifier.testTag(tag).semantics(mergeDescendants = true) {
            role = Role.Switch
            toggleableState = ToggleableState(checked)
            onClick { onToggle(); true }
        },
    ) {
        AppleSwitch(checked = checked, tag = "$tag-switch")
    }
}

private fun Modifier.partTag(tag: String?, part: String): Modifier = if (tag == null) this else testTag("$tag-$part")
