/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
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
)

@Composable
internal fun HomeScreenContent(
    visible: Boolean,
    position: UpNextPosition,
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
        Text(stringResource(R.string.settings_category_home_screen), style = MaterialTheme.typography.headlineSmall, color = SygixColors.OnDark)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.home_screen_description), style = MaterialTheme.typography.bodyMedium, color = SygixColors.OnDarkSecondary)
        Spacer(Modifier.height(Dimens.SettingsHeaderGap))
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
        additionalRows(focusEnabled && !dropdown)
    }
}

private fun UpNextPosition.label() = when (this) {
    UpNextPosition.BEFORE_APPS -> R.string.upnext_position_before
    UpNextPosition.AFTER_APPS -> R.string.upnext_position_after
}

@Composable
private fun Chevron(up: Boolean, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val top = if (up) size.height * .65f else size.height * .35f
        val bottom = size.height - top
        drawLine(color, Offset(size.width * .2f, top), Offset(size.width * .5f, bottom), GlyphStroke.toPx(), StrokeCap.Round)
        drawLine(color, Offset(size.width * .5f, bottom), Offset(size.width * .8f, top), GlyphStroke.toPx(), StrokeCap.Round)
    }
}

@Composable
private fun CheckGlyph(color: Color, modifier: Modifier) {
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
    val requesters = remember { UpNextPosition.entries.map { FocusRequester() } }
    var focusedIndex by remember { mutableStateOf(selected.ordinal) }
    val density = LocalDensity.current
    val offset = with(density) { Dimens.DropdownOffset.roundToPx() }
    val inset = with(density) { Dimens.DropdownInsetEnd.roundToPx() }
    val provider = remember(offset, inset) {
        object : PopupPositionProvider {
            override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
                IntOffset(anchorBounds.right - popupContentSize.width - inset, anchorBounds.bottom + offset)
        }
    }
    Popup(popupPositionProvider = provider, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        GlassSurface(shape = RoundedCornerShape(Dimens.DropdownCorner), look = GlassLook.Menu,
            modifier = Modifier.width(Dimens.DropdownWidth).testTag("setting-upnext-position-list").onPreviewKeyEvent {
                when (it.key) {
                    Key.Back, Key.Escape -> { if (it.type == KeyEventType.KeyUp) onClose(); true }
                    Key.DirectionLeft, Key.DirectionRight -> true
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (it.type == KeyEventType.KeyDown) {
                            val next = (focusedIndex + if (it.key == Key.DirectionDown) 1 else -1).coerceIn(0, requesters.lastIndex)
                            requesters[next].tryRequestFocus()
                        }
                        true
                    }
                    else -> false
                }
            },
        ) {
            Column(Modifier.padding(Dimens.DropdownPadding), verticalArrangement = Arrangement.spacedBy(Dimens.DropdownOptionGap)) {
                UpNextPosition.entries.forEachIndexed { index, option ->
                    var focused by remember { mutableStateOf(false) }
                    val colors = animatedPillColors(focused)
                    Row(
                        Modifier.fillMaxWidth().height(Dimens.DropdownOptionHeight)
                            .testTag("setting-upnext-position-${option.name}")
                            .tvFocusable(focusRequester = requesters[index], onFocused = { focused = it; if (it) focusedIndex = index })
                            .tvClickable(onClick = { onSelect(option) })
                            .semantics { this.selected = option == selected }
                            .focusPill(colors, RoundedCornerShape(Dimens.DropdownOptionCorner))
                            .padding(start = Dimens.DropdownOptionPaddingStart, end = Dimens.DropdownOptionPaddingEnd),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(Dimens.DropdownCheck), contentAlignment = Alignment.Center) {
                            if (option == selected) CheckGlyph(colors.content, Modifier.fillMaxSize().testTag("setting-upnext-position-check"))
                        }
                        Spacer(Modifier.width(Dimens.DropdownCheckGap))
                        Text(stringResource(option.label()), style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
                    }
                }
            }
        }
        LaunchedEffect(Unit) { requesters[selected.ordinal].tryRequestFocus() }
    }
}
