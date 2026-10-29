/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.model.UpNextPosition

class HomeScreenActions(
    val onToggleUpNext: () -> Unit = {},
    val onUpNextPosition: (UpNextPosition) -> Unit = {},
    val onToggleVisualMotion: () -> Unit = {},
)

@Composable
internal fun HomeScreenContent(
    visible: Boolean,
    position: UpNextPosition,
    visualMotion: Boolean,
    actions: HomeScreenActions,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    modifier: Modifier = Modifier,
    additionalRows: @Composable ColumnScope.(focusEnabled: Boolean) -> Unit = {},
) {
    val positionFocus = remember { FocusRequester() }
    var dropdown by remember { mutableStateOf(false) }
    var refocusPosition by remember { mutableStateOf(false) }
    LaunchedEffect(focusEnabled) { if (!focusEnabled) dropdown = false }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        if (dropdown) {
            dropdown = false
            refocusPosition = true
        }
    }
    LaunchedEffect(refocusPosition) {
        if (!refocusPosition) return@LaunchedEffect
        withFrameNanos { }
        positionFocus.tryRequestFocus()
        refocusPosition = false
    }
    Column(modifier.fillMaxWidth().testTag("settings-home-screen")) {
        SettingsHeader(R.string.settings_category_home_screen, R.string.home_screen_description)
        SwitchPreferenceRow(
            label = stringResource(R.string.upnext_visible),
            checked = visible,
            enabled = focusEnabled,
            onToggle = actions.onToggleUpNext,
            tag = "setting-upnext-visible",
            focusRequester = contentFocus,
        )
        Spacer(Modifier.height(RowSpacing))
        Box {
            PreferenceRow(
                label = stringResource(R.string.upnext_position),
                focusRequester = positionFocus,
                enabled = focusEnabled,
                onClick = { dropdown = true },
                modifier = Modifier.testTag("setting-upnext-position"),
                selected = dropdown,
            ) { colors ->
                Spacer(Modifier.width(RowGap))
                Text(stringResource(position.label()), style = TextStyles.RowState, color = colors.tertiary, modifier = Modifier.testTag("setting-upnext-position-value"))
                Spacer(Modifier.width(Dimens.SettingsChevronGap))
                Chevron(up = dropdown, color = colors.tertiary, modifier = Modifier.size(Dimens.SettingsChevron).testTag("setting-upnext-position-chevron"))
            }
            if (dropdown) {
                UpNextPositionDropdown(position, onClose = { dropdown = false; positionFocus.tryRequestFocus() }) {
                    actions.onUpNextPosition(it)
                    dropdown = false
                    positionFocus.tryRequestFocus()
                }
            }
        }
        Spacer(Modifier.height(RowSpacing))
        SwitchPreferenceRow(
            label = stringResource(R.string.visual_motion),
            detail = stringResource(R.string.visual_motion_detail),
            checked = visualMotion,
            enabled = focusEnabled && !dropdown,
            onToggle = actions.onToggleVisualMotion,
            tag = "setting-visual-motion",
        )
        additionalRows(focusEnabled && !dropdown)
    }
}

private fun UpNextPosition.label() = when (this) {
    UpNextPosition.BEFORE_APPS -> R.string.upnext_position_before
    UpNextPosition.AFTER_APPS -> R.string.upnext_position_after
}

@Composable
internal fun Chevron(up: Boolean, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val top = if (up) size.height * .65f else size.height * .35f
        val bottom = size.height - top
        drawLine(color, Offset(size.width * .2f, top), Offset(size.width * .5f, bottom), GlyphStroke.toPx(), StrokeCap.Round)
        drawLine(color, Offset(size.width * .5f, bottom), Offset(size.width * .8f, top), GlyphStroke.toPx(), StrokeCap.Round)
    }
}

@Composable
internal fun CheckGlyph(color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val stroke = GlyphStroke.toPx()
        drawLine(color, Offset(size.width * .15f, size.height * .55f), Offset(size.width * .4f, size.height * .8f), stroke, StrokeCap.Round)
        drawLine(color, Offset(size.width * .4f, size.height * .8f), Offset(size.width * .85f, size.height * .25f), stroke, StrokeCap.Round)
    }
}

private val GlyphStroke = 2.dp

@Composable
private fun UpNextPositionDropdown(
    selected: UpNextPosition,
    onClose: () -> Unit,
    onSelect: (UpNextPosition) -> Unit,
) {
    SettingsChoiceDropdown(UpNextPosition.entries, selected, "setting-upnext-position", { it.name },
        { stringResource(it.label()) }, onClose, onSelect)
}
