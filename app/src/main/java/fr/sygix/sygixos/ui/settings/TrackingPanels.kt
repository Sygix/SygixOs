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
import androidx.compose.ui.unit.dp
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.*
import fr.sygix.sygixos.tracking.*

@Composable
internal fun TrackingPanels(state: TrackingUiState, viewModel: TrackingViewModel) {
    val close = stringResource(R.string.tracking_close)
    val retry = stringResource(R.string.tracking_retry)
    val runtime = viewModel.runtime
    when {
        state.statusOpen -> TrackingModal(stringResource(R.string.tracking_error), "tracking-status-detail", listOf(
            TrackingModalAction("tracking-status-retry", stringResource(if (state.tracking.failure == TrackingFailure.ACCESS) R.string.tracking_reconnect else R.string.tracking_retry), viewModel::retryStatus),
            TrackingModalAction("tracking-status-close", close, viewModel::close),
        ), viewModel::close) { state.tracking.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) } }
        state.player != null -> {
            val player = state.player
            val other = stringResource(R.string.tracking_other_app)
            val actions = if (player.loading) emptyList() else player.apps.filter { player.otherApps || it.video }.map { app ->
                TrackingModalAction("tracking-player-${app.packageName}", app.label) { runtime.playback.choose(app.packageName) }
            } + if (!player.otherApps) listOf(TrackingModalAction("tracking-other-app", other, runtime.playback::otherApps)) else emptyList()
            TrackingModal(stringResource(R.string.tracking_player_title), "tracking-player-picker", actions + TrackingModalAction("tracking-player-close", close, viewModel::close), viewModel::close,
                initial = actions.indexOfFirst { it.key == "tracking-player-${player.preferredPackage}" }.coerceAtLeast(0)) {
                Text(player.request.content.title, style = TextStyles.Row, color = SygixColors.OnDark)
                Text(stringResource(if (player.loading) R.string.tracking_loading else if (player.failure != null) R.string.tracking_error else if (player.apps.isEmpty()) R.string.tracking_no_apps else R.string.tracking_unconfirmed), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                if (player.failure != null) Text(stringResource(R.string.tracking_player_error), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                if (player.missingPreferred) Text(stringResource(R.string.tracking_player_missing), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                val services = player.request.content.offers.mapNotNull { it.name }.distinct()
                if (services.isNotEmpty()) Text(stringResource(R.string.tracking_offer_services, services.joinToString(", ")), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, maxLines = 2)
            }
        }
        state.episodes != null -> {
            val episodes = state.episodes
            val selected = episodes.items.firstOrNull { it.identity == episodes.selected }
            if (selected != null) {
                TrackingModal(selected.episodeTitle ?: selected.title, "tracking-episode-actions", listOf(
                    TrackingModalAction("tracking-episode-open", stringResource(R.string.tracking_open_episode), viewModel::openSelectedEpisode),
                    TrackingModalAction("tracking-episode-seen", stringResource(if (selected.seen) R.string.tracking_unseen else R.string.tracking_seen), viewModel::markSelectedEpisode),
                    TrackingModalAction("tracking-episode-back", stringResource(R.string.tracking_episode_back), viewModel::close),
                ), viewModel::close)
                return
            }
            val actions = episodes.items.map { content ->
                val numbers = fr.sygix.sygixos.domain.UpNextCardText.episodeNumbers(TrackingProjection.card(content))
                val label = listOfNotNull(numbers?.let { stringResource(R.string.upnext_episode_number, it.first, it.second) }, content.episodeTitle).joinToString(stringResource(R.string.hero_details_separator))
                TrackingModalAction("tracking-episode-${content.identity.id}", label + if (content.seen) " ✓" else "") { viewModel.selectEpisode(content) }
            } + if (episodes.failure != null) listOf(TrackingModalAction("tracking-episodes-retry", stringResource(if (episodes.failure == TrackingFailure.ACCESS) R.string.tracking_reconnect else R.string.tracking_retry),
                if (episodes.failure == TrackingFailure.ACCESS) viewModel::reconnectEpisodes else viewModel::retryEpisodes)) else emptyList()
            TrackingModal(stringResource(R.string.tracking_episodes), "tracking-episodes", actions + TrackingModalAction("tracking-episodes-close", close, viewModel::close), viewModel::close,
                initial = episodes.items.indexOfFirst { it.identity == (episodes.focused ?: episodes.content.identity) }.coerceAtLeast(0), onFocusedAction = viewModel::episodeFocused,
                focusFallbackKey = episodes.focused?.let { "tracking-episode-${it.id}" }) {
                if (episodes.loading || episodes.failure != null || episodes.items.isEmpty()) Text(stringResource(if (episodes.loading) R.string.tracking_loading else if (episodes.failure != null) R.string.tracking_error else R.string.tracking_empty), style = TextStyles.Row, color = SygixColors.OnDarkSecondary)
            }
        }
        state.menu != null -> {
            val item = state.menu
            val content = item.tracking ?: return
            val episodesAction = if (content.identity.type == TrackingType.EPISODE) listOf(
                TrackingModalAction("tracking-action-episodes", stringResource(R.string.tracking_episodes), viewModel::showEpisodes),
            ) else emptyList()
            val archived = content.archived
            val archiveAction = if (content.parent.type == TrackingType.SHOW && archived != null) listOf(
                TrackingModalAction("tracking-action-archive", stringResource(if (archived) R.string.tracking_unarchive else R.string.tracking_archive)) { viewModel.action(if (archived) TrackingAction.UNARCHIVE else TrackingAction.ARCHIVE) },
            ) else emptyList()
            val actions = listOf(
                TrackingModalAction("tracking-action-player", stringResource(R.string.upnext_open_with), viewModel::openWith),
                TrackingModalAction("tracking-action-seen", stringResource(if (content.seen) R.string.tracking_unseen else R.string.tracking_seen)) { viewModel.action(if (content.seen) TrackingAction.UNSEEN else TrackingAction.SEEN) },
            ) + episodesAction + archiveAction
            TrackingModal(item.displayTitle, "upnext-menu", actions + listOf(
                TrackingModalAction("tracking-action-remove", stringResource(R.string.tracking_remove)) { viewModel.action(TrackingAction.REMOVE) },
                TrackingModalAction("upnext-menu-remove", stringResource(R.string.upnext_remove), viewModel::removeLocal),
            ), viewModel::close)
        }
        state.device != DeviceAssociationState.Closed && state.device != DeviceAssociationState.Connected -> {
            val device = state.device
            val actions = if (device is DeviceAssociationState.Error || device == DeviceAssociationState.Expired) listOf(TrackingModalAction("tracking-device-retry", retry, runtime.device::open)) else emptyList()
            TrackingModal(stringResource(R.string.tracking_device_title), "tracking-device", actions + TrackingModalAction("tracking-device-close", close, viewModel::close), viewModel::close) {
                if (device is DeviceAssociationState.Waiting) {
                    Text(stringResource(R.string.tracking_device_description), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        state.qr?.let { TrackingQr(it) }
                        Column {
                            Text(device.association.verificationUrl, style = TextStyles.Row, color = SygixColors.OnDark)
                            Text(device.association.userCode, style = TextStyles.MenuTitle, color = SygixColors.OnDark, modifier = Modifier.testTag("tracking-device-code"))
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
    val runtime = viewModel.runtime
    val text = when {
        notice.returnPrompt -> R.string.tracking_return
        notice.busy -> R.string.tracking_loading
        notice.failure != null && notice.undoing -> R.string.tracking_undo_error
        notice.failure != null -> R.string.tracking_error
        notice.action == TrackingAction.REMOVE -> R.string.tracking_remove_pending
        else -> R.string.tracking_success
    }
    GlassSurface(modifier.widthIn(min = 220.dp, max = 340.dp).testTag("tracking-notification"), shape = androidx.compose.foundation.shape.RoundedCornerShape(Dimens.MenuCorner), look = GlassLook.Menu) {
        Column(Modifier.padding(12.dp)) {
            Text(stringResource(text), style = TextStyles.Row, color = SygixColors.OnDark)
            Text(notice.content.title, style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary, maxLines = 1)
            TrackingRow(stringResource(if (notice.returnPrompt) R.string.tracking_seen else if (notice.failure != null) R.string.tracking_retry else if (notice.busy) R.string.tracking_loading else R.string.tracking_undo),
                if (notice.returnPrompt) "tracking-return-prompt" else "tracking-undo", focusEnabled, focus, runtime.actions::setFocused) {
                when {
                    notice.returnPrompt -> runtime.actions.confirmReturn()
                    notice.failure != null -> runtime.actions.retry()
                    !notice.busy -> runtime.actions.undo()
                }
            }
        }
    }
}
