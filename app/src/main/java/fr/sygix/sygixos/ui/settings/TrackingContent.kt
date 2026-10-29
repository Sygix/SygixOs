/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.*
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
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

internal fun trackingStateLabel(state: TrackingState): Int = when {
    state.failure == TrackingFailure.ACCESS -> R.string.tracking_reconnect
    state.failure == TrackingFailure.STORAGE -> R.string.tracking_error_storage
    state.pendingCount > 0 -> R.string.tracking_saved_pending
    state.loading -> R.string.tracking_loading
    state.failure != null -> trackingFailureLabel(state.failure)
    state.reserveIncomplete -> R.string.tracking_error_incomplete
    state.contents.isEmpty() -> R.string.tracking_empty
    else -> R.string.tracking_ready
}

@Composable
internal fun TrackingSettingsContent(state: TrackingUiState, viewModel: TrackingViewModel, enabled: Boolean, first: FocusRequester) {
    var dropdown by remember { mutableStateOf(false) }
    var providerDropdown by remember { mutableStateOf(false) }
    var returnToConnect by remember { mutableStateOf(false) }
    val countryFocus = remember { FocusRequester() }
    val connectFocus = remember { FocusRequester() }
    val lockedProviderFocus = remember { FocusRequester() }
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    LaunchedEffect(locale) { viewModel.displayLocale(locale) }
    LaunchedEffect(enabled) { if (!enabled) { dropdown = false; providerDropdown = false } }
    LaunchedEffect(state.tracking.hasSession) { if (state.tracking.hasSession) { returnToConnect = false; providerDropdown = false } }
    LaunchedEffect(state.tracking.hasSession, enabled, state.providerSaving) {
        if (enabled && !state.providerSaving) {
            withFrameNanos { }
            if (!state.tracking.hasSession && returnToConnect) connectFocus.tryRequestFocus() else first.tryRequestFocus()
        }
    }
    Column(Modifier.fillMaxWidth().testTag("tracking-settings")) {
        SettingsHeader(R.string.tracking_title, R.string.tracking_description)
        val provider = TrackingProviderKind.fromId(state.providerId) ?: TrackingProviderKind.BETASERIES
        val providerFocus = if (state.tracking.hasSession) lockedProviderFocus else first
        Box {
            PreferenceRow(stringResource(R.string.tracking_provider), enabled && !state.tracking.hasSession,
                { if (!state.providerSaving && !state.tracking.hasSession) { returnToConnect = false; providerDropdown = true } },
                Modifier.testTag("tracking-provider").semantics { if (state.tracking.hasSession) disabled() },
                focusRequester = providerFocus, selected = providerDropdown,
                detail = if (state.tracking.hasSession) stringResource(R.string.tracking_provider_locked) else null,
                dimmed = state.tracking.hasSession, onFocused = { if (it) returnToConnect = false }) { colors ->
                Text(if (state.providerSaving) stringResource(R.string.tracking_loading) else provider.displayName, style = TextStyles.RowState, color = colors.tertiary)
                if (!state.tracking.hasSession) Chevron(providerDropdown, colors.tertiary, Modifier.padding(start = Dimens.SettingsChevronGap).size(Dimens.SettingsChevron))
            }
            if (providerDropdown) SettingsChoiceDropdown(TrackingProviderKind.entries, provider, "tracking-provider", { it.id }, { it.displayName },
                onClose = { providerDropdown = false; providerFocus.tryRequestFocus() },
                onSelect = { viewModel.selectProvider(it.id); providerDropdown = false; providerFocus.tryRequestFocus() })
        }
        if (!state.tracking.hasSession) {
            TrackingRow(stringResource(R.string.tracking_connect), "tracking-connect", enabled && !state.providerSaving, connectFocus,
                onFocused = { if (it) returnToConnect = true }, onClick = viewModel::connect)
            state.tracking.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) }
        } else {
            state.tracking.account?.let { Text(it.name, style = TextStyles.MenuTitle, color = SygixColors.OnDark) }
            val status = trackingStateLabel(state.tracking)
            Text(stringResource(status), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
            state.tracking.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) }
            if (state.tracking.account == null && state.tracking.failure != null && state.tracking.failure != TrackingFailure.ACCESS) {
                Text(stringResource(R.string.tracking_session_retained), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
            }
            val action = when {
                state.tracking.loading -> R.string.tracking_loading
                state.tracking.failure == TrackingFailure.ACCESS -> R.string.tracking_reconnect
                state.tracking.failure != null -> R.string.tracking_retry
                else -> R.string.tracking_sync
            }
            TrackingRow(stringResource(action), "tracking-sync", enabled, first) {
                viewModel.synchronize()
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
            TrackingRow(stringResource(R.string.tracking_disconnect), "tracking-disconnect", enabled, onClick = viewModel::disconnect)
        }
        if (state.tracking.revocationUnconfirmed) Text(stringResource(R.string.tracking_revocation_unconfirmed), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
        Text(stringResource(if (provider == TrackingProviderKind.TRAKT) R.string.tracking_attribution_trakt else R.string.tracking_attribution), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, modifier = Modifier.padding(top = Dimens.MenuPadding).testTag("tracking-attribution"))
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
        GlassSurface(Modifier.width(Dimens.TrackingCountryWidth).testTag("tracking-country-list").onPreviewKeyEvent {
            when (it.key) {
                Key.DirectionLeft, Key.DirectionRight -> true
                Key.Back, Key.Escape -> { if (it.type == KeyEventType.KeyUp) onClose(); true }
                else -> false
            }
        }, shape = RoundedCornerShape(Dimens.DropdownCorner), look = GlassLook.Menu) {
            LazyColumn(Modifier.heightIn(max = Dimens.TrackingCountryHeight).padding(Dimens.DropdownPadding), state = list) {
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
internal fun TrackingModal(title: String, tag: String, actions: List<TrackingModalAction>, onClose: () -> Unit, initial: Int = 0, onFocusedAction: (String) -> Unit = {}, focusFallbackKey: String? = null, focusModel: TrackingFocusViewModel, loading: Boolean = false, body: @Composable ColumnScope.() -> Unit = {}) {
    val index = initial.coerceIn(0, actions.lastIndex.coerceAtLeast(0))
    val list = rememberLazyListState(index)
    val keys = actions.map { it.key }
    val requesters = remember(tag, keys) { keys.associateWith { FocusRequester() } }
    val focusedKey by focusModel.panelFocus.collectAsState()
    LaunchedEffect(tag, keys, loading) { focusModel.panelActions(tag, keys, index, focusFallbackKey, loading) }
    Popup(onDismissRequest = onClose, properties = PopupProperties(focusable = true)) {
        fr.sygix.sygixos.ui.home.ContextMenuPanel(Modifier.testTag(tag).onPreviewKeyEvent {
            when (it.key) {
                Key.DirectionLeft, Key.DirectionRight -> true
                Key.Back, Key.Escape -> { if (it.type == KeyEventType.KeyUp) onClose(); true }
                else -> false
            }
        }) {
                Column(verticalArrangement = Arrangement.spacedBy(Dimens.OnboardingSpacing)) {
                    Text(title, style = TextStyles.MenuTitle, color = SygixColors.OnDark)
                    body()
                    LazyColumn(Modifier.heightIn(max = Dimens.TrackingListHeight), state = list) {
                        itemsIndexed(actions, key = { _, action -> action.key }) { i, action ->
                            Box(Modifier.focusProperties { if (i == 0) up = FocusRequester.Cancel; if (i == actions.lastIndex) down = FocusRequester.Cancel }) {
                                TrackingRow(action.label, action.key, requester = requesters[action.key], onFocused = { if (it && focusModel.panelFocused(action.key, keys)) onFocusedAction(action.key) }, onClick = action.click)
                            }
                        }
                    }
                }
        }
        LaunchedEffect(tag, keys, focusedKey) {
            val targetIndex = keys.indexOf(focusedKey)
            if (targetIndex >= 0) {
                list.scrollToItem(targetIndex); withFrameNanos { }
                if (requesters[focusedKey]?.tryRequestFocus() == true) focusModel.panelRestored(focusedKey)
            }
        }
    }
}

@Composable
internal fun TrackingQr(code: QrCode) {
    Canvas(Modifier.size(Dimens.QrSize).testTag("tracking-device-qr")) {
        drawRect(Color.White)
        val cell = size.minDimension / code.size
        for (y in 0 until code.size) for (x in 0 until code.size) if (code[x, y]) drawRect(Color.Black, Offset(x * cell, y * cell), Size(cell, cell))
    }
}
