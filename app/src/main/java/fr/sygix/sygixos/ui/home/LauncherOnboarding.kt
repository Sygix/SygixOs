/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassLook
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.SygixTypography
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.ui.settings.LauncherRowTags
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import fr.sygix.sygixos.ui.settings.LauncherSystemRows
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val LauncherPanelZ = 30f

@Composable
internal fun LauncherOnboarding(state: LauncherSystemState, actions: LauncherSystemActions) {
    val rowCount = state.rows.size
    val requesters = remember { List(rowCount + 1) { FocusRequester() } }
    var focused by remember { mutableIntStateOf(0) }
    val enabled = !state.showOverlayConfirmation
    LaunchedEffect(Unit) { requesters[0].tryRequestFocus() }
    LauncherGlassPanel("onboarding-launcher", Modifier.onPreviewKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionDown, Key.DirectionUp -> {
                val next = (focused + if (event.key == Key.DirectionDown) 1 else -1).coerceIn(0, rowCount)
                requesters[next].tryRequestFocus()
                true
            }
            Key.DirectionLeft, Key.DirectionRight -> true
            Key.Back -> {
                actions.dismiss()
                true
            }
            else -> false
        }
    }) {
        Column(
            Modifier.fillMaxWidth().padding(
                start = Dimens.OnboardingHeaderInsetH,
                end = Dimens.OnboardingHeaderInsetH,
                top = Dimens.OnboardingHeaderInsetTop,
                bottom = Dimens.OnboardingHeaderInsetBottom,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.OnboardingHeaderSpacing),
        ) {
            OnboardingMascot()
            Text(stringResource(R.string.launcher_onboarding_title), style = TextStyles.SettingsTitle, color = SygixColors.OnDark)
            Text(stringResource(R.string.launcher_onboarding_description), style = SygixTypography.bodyMedium, color = SygixColors.OnDarkSecondary)
        }
        LauncherSystemRows(
            state = state,
            actions = actions,
            tags = LauncherRowTags.Onboarding,
            spacing = Dimens.OnboardingSpacing,
            focusEnabled = enabled,
            requesters = requesters.take(rowCount),
            onFocused = { focused = it },
        )
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.OnboardingHeaderInsetH, top = Dimens.OnboardingFooterTop),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.launcher_onboarding_hint),
                style = TextStyles.RowSecondary,
                color = SygixColors.OnDarkSecondary,
                modifier = Modifier.testTag("onboarding-hint"),
            )
            LauncherDialogButton(
                label = stringResource(R.string.launcher_continue),
                tag = "onboarding-continue",
                requester = requesters[rowCount],
                enabled = enabled,
                onFocused = { focused = rowCount },
                onClick = actions.dismiss,
            )
        }
    }
}

@Composable
private fun OnboardingMascot() {
    val resources = LocalContext.current.resources
    val target = with(LocalDensity.current) { Dimens.OnboardingMascot.roundToPx() }
    val bitmap by produceState<Bitmap?>(null, resources, target) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resources.openRawResource(R.raw.splash_mascot).use { BitmapFactory.decodeStream(it, null, bounds) }
                val sample = ImageBounds.sampleSize(bounds.outWidth, target)
                resources.openRawResource(R.raw.splash_mascot).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }.getOrNull()
        }
    }
    Box(Modifier.size(Dimens.OnboardingMascot).testTag("onboarding-mascot")) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}

@Composable
internal fun OverlayPermissionConfirmation(actions: LauncherSystemActions) {
    val requesters = remember { List(2) { FocusRequester() } }
    var focused by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { requesters[0].tryRequestFocus() }
    LauncherGlassPanel("launcher-overlay-confirmation", Modifier.onPreviewKeyEvent {
        if (it.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (it.key) {
            Key.Back -> {
                actions.declineOverlay()
                true
            }
            Key.DirectionDown, Key.DirectionUp -> {
                requesters[(focused + if (it.key == Key.DirectionDown) 1 else -1).coerceIn(0, 1)].tryRequestFocus()
                true
            }
            Key.DirectionLeft, Key.DirectionRight -> true
            else -> false
        }
    }) {
        Text(stringResource(R.string.launcher_overlay_title), style = TextStyles.SettingsTitle, color = SygixColors.OnDark)
        Text(stringResource(R.string.launcher_overlay_description), style = SygixTypography.bodyMedium, color = SygixColors.OnDarkSecondary)
        LauncherDialogButton(
            label = stringResource(R.string.launcher_overlay_open),
            tag = "launcher-overlay-open",
            requester = requesters[0],
            enabled = true,
            onFocused = { focused = 0 },
            onClick = actions.confirmOverlay,
        )
        LauncherDialogButton(
            label = stringResource(R.string.launcher_overlay_decline),
            tag = "launcher-overlay-decline",
            requester = requesters[1],
            enabled = true,
            onFocused = { focused = 1 },
            onClick = actions.declineOverlay,
        )
    }
}

@Composable
private fun LauncherGlassPanel(tag: String, modifier: Modifier, content: @Composable ColumnScope.() -> Unit) {
    Box(
        modifier.testTag(tag).fillMaxSize().zIndex(LauncherPanelZ).background(SygixColors.Scrim),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(Modifier.width(Dimens.OnboardingWidth), shape = RoundedCornerShape(Dimens.OnboardingCorner), look = GlassLook.Menu) {
            Column(Modifier.padding(Dimens.OnboardingPadding), verticalArrangement = Arrangement.spacedBy(Dimens.OnboardingSpacing), content = content)
        }
    }
}

@Composable
private fun LauncherDialogButton(
    label: String,
    tag: String,
    requester: FocusRequester,
    enabled: Boolean,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused, rest = SygixColors.PillRest)
    Box(
        Modifier
            .testTag(tag)
            .tvFocusable(focusRequester = requester, enabled = enabled, onFocused = { focused = it; if (it) onFocused() })
            .tvClickable(onClick = { if (enabled) onClick() })
            .focusPill(colors, RoundedCornerShape(Dimens.PillCorner))
            .height(Dimens.MenuActionHeight)
            .padding(horizontal = Dimens.OnboardingButtonPaddingH),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
    }
}
