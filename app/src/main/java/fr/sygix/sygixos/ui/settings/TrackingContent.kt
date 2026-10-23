/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.*
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.domain.QrCode
import fr.sygix.sygixos.tracking.*

internal fun trackingFailureLabel(failure: TrackingFailure): Int = when (failure) {
    TrackingFailure.NETWORK -> R.string.tracking_error_network
    TrackingFailure.CONFIGURATION -> R.string.tracking_error_configuration
    TrackingFailure.ACCESS -> R.string.tracking_error_access
    TrackingFailure.STORAGE -> R.string.tracking_error_storage
    TrackingFailure.INCOMPLETE -> R.string.tracking_error_incomplete
    TrackingFailure.PROTOCOL -> R.string.tracking_error_protocol
}

@Composable
internal fun TrackingSettingsContent(state: TrackingUiState, viewModel: TrackingViewModel, enabled: Boolean, first: FocusRequester) {
    val runtime = viewModel.runtime
    var dropdown by remember { mutableStateOf(false) }
    val countryFocus = remember { FocusRequester() }
    LaunchedEffect(enabled) { if (!enabled) dropdown = false }
    LaunchedEffect(state.tracking.account?.id, enabled) { if (enabled) { withFrameNanos { }; first.tryRequestFocus() } }
    Column(Modifier.fillMaxWidth().testTag("tracking-settings")) {
        SettingsHeader(R.string.tracking_title, R.string.tracking_description)
        if (state.tracking.account == null) {
            TrackingRow(stringResource(R.string.tracking_connect), "tracking-connect", enabled, first) { runtime.device.open() }
            state.tracking.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) }
        } else {
            Text(state.tracking.account.name, style = TextStyles.MenuTitle, color = SygixColors.OnDark)
            val status = when {
                state.tracking.loading -> R.string.tracking_loading
                state.tracking.failure == TrackingFailure.ACCESS -> R.string.tracking_reconnect
                state.tracking.failure != null -> R.string.tracking_error
                state.tracking.contents.isEmpty() -> R.string.tracking_empty
                else -> R.string.tracking_ready
            }
            Text(stringResource(status), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
            state.tracking.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) }
            TrackingRow(stringResource(if (state.tracking.failure == TrackingFailure.ACCESS) R.string.tracking_reconnect else R.string.tracking_sync), "tracking-sync", enabled, first) {
                if (state.tracking.failure == TrackingFailure.ACCESS) { runtime.disconnect(); runtime.device.open() } else runtime.controller.refresh()
            }
            Box {
                PreferenceRow(stringResource(R.string.tracking_country), enabled, { dropdown = true },
                    Modifier.testTag("tracking-country"), focusRequester = countryFocus, selected = dropdown) { colors ->
                    Text(state.countries.firstOrNull { it.code == (state.country ?: state.countryProposed) }?.label ?: stringResource(R.string.tracking_country_proposed), style = TextStyles.RowState, color = colors.tertiary)
                    Chevron(dropdown, colors.tertiary, Modifier.padding(start = Dimens.SettingsChevronGap).size(Dimens.SettingsChevron))
                }
                if (dropdown) TrackingCountryDropdown(state, { dropdown = false; countryFocus.tryRequestFocus() }) { code ->
                    viewModel.confirmCountry(code); dropdown = false; countryFocus.tryRequestFocus()
                }
            }
            PreferenceRow(stringResource(R.string.tracking_media_access), enabled, viewModel::mediaAccess,
                Modifier.testTag("tracking-media-access"), detail = stringResource(when (state.mediaAccess) {
                    MediaAccessState.GRANTED -> R.string.tracking_media_granted
                    MediaAccessState.ACTIVATING -> R.string.tracking_loading
                    MediaAccessState.DISABLED -> R.string.tracking_media_disabled
                    MediaAccessState.UNAVAILABLE -> R.string.tracking_media_unavailable
                    MediaAccessState.ERROR -> R.string.tracking_error
                    MediaAccessState.MANUAL -> R.string.tracking_media_manual
                }), trailing = {})
            TrackingRow(stringResource(R.string.tracking_disconnect), "tracking-disconnect", enabled) { runtime.disconnect() }
        }
        Text(stringResource(R.string.tracking_attribution), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, modifier = Modifier.padding(top = 16.dp))
    }
}

@Composable
internal fun TrackingRow(label: String, tag: String, enabled: Boolean = true, requester: FocusRequester? = null, onFocused: (Boolean) -> Unit = {}, onClick: () -> Unit) {
    PreferenceRow(label, enabled, onClick, Modifier.testTag(tag), focusRequester = requester, onFocused = onFocused, trailing = {})
}

@Composable
private fun TrackingCountryDropdown(state: TrackingUiState, onClose: () -> Unit, onSelect: (String) -> Unit) {
    val countries = state.countries
    val selected = countries.indexOfFirst { it.code == (state.country ?: state.countryProposed) }.coerceAtLeast(0)
    val list = rememberLazyListState(selected)
    val requesters = remember(countries) { countries.map { FocusRequester() } }
    val position = remember { object : PopupPositionProvider {
        override fun calculatePosition(anchorBounds: IntRect, windowSize: IntSize, layoutDirection: LayoutDirection, popupContentSize: IntSize) =
            IntOffset(anchorBounds.right - popupContentSize.width, minOf(anchorBounds.bottom, windowSize.height - popupContentSize.height))
    } }
    Popup(popupPositionProvider = position, onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        GlassSurface(Modifier.width(320.dp).testTag("tracking-country-list").onPreviewKeyEvent {
            when (it.key) {
                Key.DirectionLeft, Key.DirectionRight -> true
                Key.Back, Key.Escape -> { if (it.type == KeyEventType.KeyUp) onClose(); true }
                else -> false
            }
        }, shape = RoundedCornerShape(Dimens.DropdownCorner), look = GlassLook.Menu) {
            LazyColumn(Modifier.heightIn(max = 240.dp).padding(Dimens.DropdownPadding), state = list) {
                itemsIndexed(countries, key = { _, country -> country.code }) { index, country ->
                    Box(Modifier.focusProperties { if (index == 0) up = FocusRequester.Cancel; if (index == countries.lastIndex) down = FocusRequester.Cancel }) {
                        TrackingRow(country.label, "tracking-country-${country.code}", requester = requesters[index]) { onSelect(country.code) }
                    }
                }
            }
        }
        LaunchedEffect(Unit) { withFrameNanos { }; requesters.getOrNull(selected)?.tryRequestFocus() }
    }
}

internal data class TrackingModalAction(val key: String, val label: String, val click: () -> Unit)

@Composable
internal fun TrackingModal(title: String, tag: String, actions: List<TrackingModalAction>, onClose: () -> Unit, initial: Int = 0, onFocusedAction: (String) -> Unit = {}, focusFallbackKey: String? = null, body: @Composable ColumnScope.() -> Unit = {}) {
    val index = initial.coerceIn(0, actions.lastIndex.coerceAtLeast(0))
    val list = rememberLazyListState(index)
    val keys = actions.map { it.key }
    val requesters = remember(tag, keys) { keys.associateWith { FocusRequester() } }
    var focusedKey by remember(tag) { mutableStateOf<String?>(null) }
    var focusedIndex by remember(tag) { mutableIntStateOf(index) }
    var previousKeys by remember(tag) { mutableStateOf<List<String>>(emptyList()) }
    val previousFocusedKey = focusedKey
    val previousFocusedIndex = focusedIndex
    val focusedActionRemoved = focusedKey != null && focusedKey !in keys
    Popup(onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        Box(Modifier.fillMaxSize().background(SygixColors.Scrim).testTag(tag).onPreviewKeyEvent {
            when (it.key) {
                Key.DirectionLeft, Key.DirectionRight -> true
                Key.Back, Key.Escape -> { if (it.type == KeyEventType.KeyUp) onClose(); true }
                else -> false
            }
        }, contentAlignment = Alignment.Center) {
            GlassSurface(Modifier.width(440.dp), shape = RoundedCornerShape(Dimens.MenuCorner), look = GlassLook.Menu) {
                Column(Modifier.padding(Dimens.MenuPadding), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(title, style = TextStyles.MenuTitle, color = SygixColors.OnDark)
                    body()
                    LazyColumn(Modifier.heightIn(max = 280.dp), state = list) {
                        itemsIndexed(actions, key = { _, action -> action.key }) { i, action ->
                            Box(Modifier.focusProperties { if (i == 0) up = FocusRequester.Cancel; if (i == actions.lastIndex) down = FocusRequester.Cancel }) {
                                TrackingRow(action.label, action.key, requester = requesters[action.key], onFocused = { if (it) { focusedKey = action.key; focusedIndex = i; onFocusedAction(action.key) } }, onClick = action.click)
                            }
                        }
                    }
                }
            }
        }
        LaunchedEffect(tag, keys) {
            val loadingFinished = previousKeys.size == 1 && previousKeys.single().endsWith("-close") && keys.size > 1
            val target = when {
                previousKeys.isEmpty() || loadingFinished -> keys.getOrNull(index)
                focusedActionRemoved && focusFallbackKey in keys -> focusFallbackKey
                else -> fr.sygix.sygixos.domain.FocusFallback.entry(keys, previousFocusedKey, previousFocusedIndex)
            }
            previousKeys = keys
            val targetIndex = keys.indexOf(target)
            if (targetIndex >= 0) { list.scrollToItem(targetIndex); withFrameNanos { }; requesters[target]?.tryRequestFocus() }
        }
    }
}

@Composable
internal fun TrackingQr(code: QrCode) {
    Canvas(Modifier.size(136.dp).testTag("tracking-device-qr")) {
        drawRect(Color.White)
        val cell = size.minDimension / code.size
        for (y in 0 until code.size) for (x in 0 until code.size) if (code[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell, cell))
    }
}
