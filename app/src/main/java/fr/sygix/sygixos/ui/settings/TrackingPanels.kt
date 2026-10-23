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
                player.failure?.let { Text(stringResource(trackingFailureLabel(it)), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary) }
            }
        }
        state.episodes != null -> {
            val episodes = state.episodes
            val actions = episodes.items.map { content ->
                TrackingModalAction("tracking-episode-${content.identity.id}", "S${content.season}E${content.episode} · ${content.episodeTitle.orEmpty()}${if (content.seen) " ✓" else ""}") { viewModel.openEpisode(content) }
            } + if (episodes.failure != null) listOf(TrackingModalAction("tracking-episodes-retry", retry, viewModel::retryEpisodes)) else emptyList()
            TrackingModal(stringResource(R.string.tracking_episodes), "tracking-episodes", actions + TrackingModalAction("tracking-episodes-close", close, viewModel::close), viewModel::close,
                initial = episodes.items.indexOfFirst { it.identity == episodes.content.identity }.coerceAtLeast(0)) {
                if (episodes.loading || episodes.failure != null || episodes.items.isEmpty()) Text(stringResource(if (episodes.loading) R.string.tracking_loading else if (episodes.failure != null) R.string.tracking_error else R.string.tracking_empty), style = TextStyles.Row, color = SygixColors.OnDarkSecondary)
            }
        }
        state.menu != null -> {
            val item = state.menu
            val content = item.tracking ?: return
            val actions = listOf(
                TrackingModalAction("tracking-action-player", stringResource(R.string.upnext_open_with), viewModel::openWith),
                TrackingModalAction("tracking-action-seen", stringResource(if (content.seen) R.string.tracking_unseen else R.string.tracking_seen)) { viewModel.action(if (content.seen) TrackingAction.UNSEEN else TrackingAction.SEEN) },
            ) + if (content.identity.type == TrackingType.EPISODE) listOf(
                TrackingModalAction("tracking-action-episodes", stringResource(R.string.tracking_episodes), viewModel::showEpisodes),
                TrackingModalAction("tracking-action-archive", stringResource(R.string.tracking_archive)) { viewModel.action(TrackingAction.ARCHIVE) },
            ) else emptyList()
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
