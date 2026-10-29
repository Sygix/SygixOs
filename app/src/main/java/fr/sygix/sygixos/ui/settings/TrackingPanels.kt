/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.tracking.*

@Composable
internal fun TrackingPanels(state: TrackingUiState, viewModel: TrackingViewModel) {
    val close = stringResource(R.string.tracking_close)
    val retry = stringResource(R.string.tracking_retry)
    when {
        state.statusOpen -> TrackingModal(stringResource(trackingStateLabel(state.tracking)), "tracking-status-detail", listOf(
            TrackingModalAction("tracking-status-retry", stringResource(if (state.tracking.failure == TrackingFailure.ACCESS) R.string.tracking_reconnect else R.string.tracking_retry), viewModel::retryStatus),
            TrackingModalAction("tracking-status-close", close, viewModel::close),
        ), viewModel::close, focusModel = viewModel) { state.tracking.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) } }
        state.player != null -> {
            val player = state.player
            val other = stringResource(R.string.tracking_other_app)
            val actions = if (player.loading) emptyList() else state.playerApps.map { app ->
                TrackingModalAction("tracking-player-${app.packageName}", app.label) { viewModel.choosePlayer(app.packageName) }
            } + (if (!player.otherApps) listOf(TrackingModalAction("tracking-other-app", other, viewModel::otherApps)) else emptyList()) +
                (if (player.offersFailure != null) listOf(TrackingModalAction("tracking-offers-retry", retry, viewModel::retryOffers)) else emptyList())
            TrackingModal(stringResource(R.string.tracking_player_title), "tracking-player-picker", actions + TrackingModalAction("tracking-player-close", close, viewModel::close), viewModel::close,
                initial = actions.indexOfFirst { it.key == "tracking-player-${player.preferredPackage}" }.coerceAtLeast(0), focusModel = viewModel, loading = player.loading) {
                Text(player.request.content.title, style = TextStyles.Row, color = SygixColors.OnDark)
                Text(stringResource(if (player.loading) R.string.tracking_loading else if (player.failure != null) R.string.tracking_error else if (player.apps.isEmpty()) R.string.tracking_no_apps else R.string.tracking_unconfirmed), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                if (player.failure != null) Text(stringResource(R.string.tracking_player_error), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                if (player.offersLoading) Text(stringResource(R.string.tracking_offer_loading), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                if (player.offersFailure != null) Text(stringResource(R.string.tracking_offer_error), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                if (player.missingPreferred) Text(stringResource(R.string.tracking_player_missing), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                val services = player.request.content.offers.mapNotNull { it.name }.distinct()
                if (services.isNotEmpty()) Text(stringResource(R.string.tracking_offer_services, services.joinToString(", ")), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, maxLines = 2)
                val confirmed = player.request.content.offers.filter { it.confirmedCountry != null }.mapNotNull { it.name }.distinct()
                if (confirmed.isNotEmpty()) Text(stringResource(R.string.tracking_offer_confirmed, confirmed.joinToString(", ")), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, maxLines = 2)
            }
        }
        state.episodes != null -> {
            val episodes = state.episodes
            if (episodes.choosingSeason) {
                val actions = episodes.seasons.map { season -> TrackingModalAction("tracking-season-$season", stringResource(R.string.tracking_season, season)) { viewModel.chooseSeason(season) } } +
                    if (episodes.failure != null) listOf(TrackingModalAction("tracking-seasons-retry", retry, viewModel::showSeasons)) else emptyList()
                TrackingModal(stringResource(R.string.tracking_seasons), "tracking-season-picker", actions + TrackingModalAction("tracking-season-back", stringResource(R.string.tracking_episode_back), viewModel::close), viewModel::close,
                    initial = episodes.seasons.indexOf(episodes.content.season ?: 1).coerceAtLeast(0), focusModel = viewModel, loading = episodes.loading) {
                    if (episodes.loading || episodes.failure != null || episodes.seasons.isEmpty()) Text(stringResource(if (episodes.loading) R.string.tracking_loading else if (episodes.failure != null) R.string.tracking_error else R.string.tracking_empty), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                }
                return
            }
            val selected = episodes.items.firstOrNull { it.identity == episodes.selected }
            if (selected != null) {
                TrackingModal(selected.episodeTitle ?: selected.title, "tracking-episode-actions", listOf(
                    TrackingModalAction("tracking-episode-open", stringResource(R.string.tracking_open_episode), viewModel::openSelectedEpisode),
                    TrackingModalAction("tracking-episode-seen", stringResource(if (selected.seen) R.string.tracking_unseen else R.string.tracking_seen), viewModel::markSelectedEpisode),
                    TrackingModalAction("tracking-episode-back", stringResource(R.string.tracking_episode_back), viewModel::close),
                ), viewModel::close, focusModel = viewModel)
                return
            }
            val actions = episodes.items.map { content ->
                val numbers = fr.sygix.sygixos.domain.UpNextCardText.episodeNumbers(TrackingProjection.card(content))
                val label = listOfNotNull(numbers?.let { stringResource(R.string.upnext_episode_number, it.first, it.second) }, content.episodeTitle).joinToString(stringResource(R.string.hero_details_separator))
                TrackingModalAction("tracking-episode-${content.identity.id}", if (content.seen) stringResource(R.string.tracking_episode_seen_label, label) else label) { viewModel.selectEpisode(content) }
            } + if (episodes.failure != null) listOf(TrackingModalAction("tracking-episodes-retry", stringResource(if (episodes.failure == TrackingFailure.ACCESS) R.string.tracking_reconnect else R.string.tracking_retry),
                if (episodes.failure == TrackingFailure.ACCESS) viewModel::reconnectEpisodes else viewModel::retryEpisodes)) else emptyList()
            TrackingModal(stringResource(R.string.tracking_episodes), "tracking-episodes", actions + listOf(TrackingModalAction("tracking-seasons", stringResource(R.string.tracking_seasons), viewModel::showSeasons), TrackingModalAction("tracking-episodes-close", close, viewModel::close)), viewModel::close,
                initial = episodes.items.indexOfFirst { it.identity == (episodes.focused ?: episodes.content.identity) }.coerceAtLeast(0), onFocusedAction = viewModel::episodeFocused,
                focusFallbackKey = episodes.focused?.let { "tracking-episode-${it.id}" }, focusModel = viewModel, loading = episodes.loading) {
                if (episodes.loading || episodes.failure != null || episodes.items.isEmpty()) Text(stringResource(if (episodes.loading) R.string.tracking_loading else if (episodes.failure != null) R.string.tracking_error else R.string.tracking_empty), style = TextStyles.Row, color = SygixColors.OnDarkSecondary)
            }
        }
        state.menu != null -> {
            val item = state.menu
            val actions = state.menuActions.map { action -> TrackingModalAction(action.tag, stringResource(action.label)) { viewModel.menuAction(action.tag) } }
            TrackingModal(item.displayTitle, "upnext-menu", actions, viewModel::close, focusModel = viewModel)
        }
        state.device != DeviceAssociationState.Closed && state.device != DeviceAssociationState.Connected -> {
            val device = state.device
            val actions = if (device is DeviceAssociationState.Error || device == DeviceAssociationState.Expired) listOf(TrackingModalAction("tracking-device-retry", retry, viewModel::connect)) else emptyList()
            TrackingModal(stringResource(R.string.tracking_device_title), "tracking-device", actions + TrackingModalAction("tracking-device-close", close, viewModel::close), viewModel::close, focusModel = viewModel) {
                if (device is DeviceAssociationState.Waiting) {
                    Text(stringResource(R.string.tracking_device_description), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                    Column(verticalArrangement = Arrangement.spacedBy(Dimens.OnboardingSpacing)) {
                        state.qr?.let { TrackingQr(it) }
                        Column {
                            Text(device.association.userCode, style = TextStyles.HeroTitle, color = SygixColors.OnDark, modifier = Modifier.testTag("tracking-device-code"))
                            Text(device.association.verificationUrl, style = TextStyles.Row, color = SygixColors.OnDark)
                        }
                    }
                } else Text(stringResource(if (device == DeviceAssociationState.Expired) R.string.tracking_device_expired else if (device is DeviceAssociationState.Error) R.string.tracking_error else R.string.tracking_loading), style = TextStyles.Row, color = SygixColors.OnDarkSecondary)
                if (device is DeviceAssociationState.Error) Text(stringResource(trackingFailureLabel(device.failure)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
            }
        }
    }
}

@Composable
internal fun TrackingNotification(notice: TrackingNotice, viewModel: TrackingViewModel, focusEnabled: Boolean, focus: FocusRequester, modifier: Modifier = Modifier) {
    val text = when {
        notice.returnPrompt -> R.string.tracking_return
        notice.busy -> R.string.tracking_loading
        notice.failure != null && notice.undoing -> R.string.tracking_undo_error
        notice.failure != null -> R.string.tracking_error
        notice.action == TrackingAction.REMOVE -> R.string.tracking_remove_pending
        notice.locallySaved && notice.confirmed -> R.string.tracking_synced
        notice.locallySaved -> R.string.tracking_saved_pending
        else -> R.string.tracking_success
    }
    GlassSurface(modifier.widthIn(min = Dimens.TrackingNotificationMinWidth, max = Dimens.TrackingNotificationMaxWidth).testTag("tracking-notification"), shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.MenuCorner), look = GlassLook.Menu) {
        Column(Modifier.padding(Dimens.MenuPadding)) {
            Text(stringResource(text), style = TextStyles.Row, color = SygixColors.OnDark)
            Text(notice.content.title, style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, maxLines = 1)
            if (notice.blocked) Text(stringResource(R.string.tracking_action_blocked), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
            TrackingRow(stringResource(if (notice.returnPrompt) R.string.tracking_seen else if (notice.failure == TrackingFailure.ACCESS) R.string.tracking_reconnect else if (notice.failure != null) R.string.tracking_retry else if (notice.busy) R.string.tracking_loading else R.string.tracking_undo),
                if (notice.returnPrompt) "tracking-return-prompt" else "tracking-undo", focusEnabled, focus, viewModel::noticeFocused, viewModel::noticeAction)
        }
    }
}
