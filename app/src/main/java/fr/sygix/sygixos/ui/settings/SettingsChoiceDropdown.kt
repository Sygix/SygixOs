/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.domain.FocusFallback

@Composable
internal fun <T> SettingsChoiceDropdown(options: List<T>, selected: T, tag: String, key: (T) -> String,
    label: @Composable (T) -> String, onClose: () -> Unit, onSelect: (T) -> Unit,
) {
    val keys = options.map(key)
    val requesters = remember(keys) { options.map { FocusRequester() } }
    var focusedIndex by remember { mutableIntStateOf(options.indexOf(selected).coerceAtLeast(0)) }
    var focusedKey by remember { mutableStateOf(key(selected)) }
    val density = LocalDensity.current
    val offset = with(density) { Dimens.DropdownOffset.roundToPx() }
    val inset = with(density) { Dimens.DropdownInsetEnd.roundToPx() }
    val position = remember(offset, inset) { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
            IntOffset(anchorBounds.right - popupContentSize.width - inset, anchorBounds.bottom + offset)
    } }
    Popup(popupPositionProvider = position, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        GlassSurface(shape = RoundedCornerShape(Dimens.DropdownCorner), look = GlassLook.Menu,
            modifier = Modifier.width(Dimens.DropdownWidth).testTag("$tag-list").onPreviewKeyEvent {
                when (it.key) {
                    Key.Back, Key.Escape -> { if (it.type == KeyEventType.KeyUp) onClose(); true }
                    Key.DirectionLeft, Key.DirectionRight -> true
                    Key.DirectionUp, Key.DirectionDown -> {
                        if (it.type == KeyEventType.KeyDown && requesters.isNotEmpty()) {
                            val next = (focusedIndex + if (it.key == Key.DirectionDown) 1 else -1).coerceIn(0, requesters.lastIndex)
                            requesters[next].tryRequestFocus()
                        }
                        true
                    }
                    else -> false
                }
            }) {
            Column(Modifier.padding(Dimens.DropdownPadding), verticalArrangement = Arrangement.spacedBy(Dimens.DropdownOptionGap)) {
                options.forEachIndexed { index, option ->
                    var focused by remember(keys[index]) { mutableStateOf(false) }
                    val colors = animatedPillColors(focused)
                    Row(Modifier.fillMaxWidth().height(Dimens.DropdownOptionHeight).testTag("$tag-${key(option)}")
                        .tvFocusable(focusRequester = requesters[index], onFocused = {
                            focused = it
                            if (it) { focusedIndex = index; focusedKey = keys[index] }
                        }).tvClickable(onClick = { onSelect(option) }).semantics { this.selected = option == selected }
                        .focusPill(colors, RoundedCornerShape(Dimens.DropdownOptionCorner))
                        .padding(start = Dimens.DropdownOptionPaddingStart, end = Dimens.DropdownOptionPaddingEnd),
                        verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(Dimens.DropdownCheck), contentAlignment = Alignment.Center) {
                            if (option == selected) CheckGlyph(colors.content, Modifier.fillMaxSize().testTag("$tag-check"))
                        }
                        Spacer(Modifier.width(Dimens.DropdownCheckGap))
                        Text(label(option), style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
                    }
                }
            }
        }
        LaunchedEffect(keys) {
            val target = FocusFallback.entry(keys, focusedKey, focusedIndex)
            val index = keys.indexOf(target)
            if (index < 0) onClose() else requesters[index].tryRequestFocus()
        }
    }
}
