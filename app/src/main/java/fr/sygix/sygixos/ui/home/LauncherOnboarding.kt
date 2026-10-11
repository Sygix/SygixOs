/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.zIndex
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.GlassLook
import fr.sygix.sygixos.core.designsystem.GlassSurface
import fr.sygix.sygixos.core.designsystem.PillColors
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.SygixTypography
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.domain.LauncherGuide
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.GuideStepState
import fr.sygix.sygixos.model.LauncherSystemState
import fr.sygix.sygixos.model.StepStatus
import fr.sygix.sygixos.ui.settings.AppleSwitch
import fr.sygix.sygixos.ui.settings.LauncherSystemActions
import fr.sygix.sygixos.ui.settings.PreferenceRow
import fr.sygix.sygixos.ui.settings.RowGap
import fr.sygix.sygixos.ui.settings.StateLabel
import fr.sygix.sygixos.ui.settings.dotColor
import fr.sygix.sygixos.ui.settings.labelRes
import fr.sygix.sygixos.ui.settings.tag
import fr.sygix.sygixos.ui.settings.textRes
import fr.sygix.sygixos.ui.settings.titleRes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal const val LauncherPanelZ = 30f

private const val DoneTarget = "onboarding-done"

@Composable
internal fun LauncherGuidePanel(state: LauncherSystemState, actions: LauncherSystemActions) {
    val steps = LauncherGuide.steps(state)
    val progress = LauncherGuide.progress(steps)
    val targets = steps.map { it.step.tag() } + DoneTarget
    val requesters = remember { mutableMapOf<String, FocusRequester>() }
    fun requesterFor(target: String) = requesters.getOrPut(target) { FocusRequester() }
    var focused by remember { mutableStateOf<String?>(null) }
    val current by rememberUpdatedState(targets)
    LaunchedEffect(Unit) {
        withFrameNanos { }
        requesterFor(LauncherGuide.initialFocus(steps)?.tag() ?: DoneTarget).tryRequestFocus()
    }
    LaunchedEffect(state.focusReturn, state.systemPending) {
        val target = state.focusReturn ?: return@LaunchedEffect
        if (state.systemPending) return@LaunchedEffect
        withFrameNanos { }
        requesterFor(if (target.tag() in current) target.tag() else DoneTarget).tryRequestFocus()
        actions.focusReturned()
    }
    LauncherGlassPanel("onboarding-launcher", Modifier.onPreviewKeyEvent { event ->
        if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
        when (event.key) {
            Key.DirectionDown, Key.DirectionUp -> {
                val index = current.indexOf(focused).coerceAtLeast(0)
                val next = (index + if (event.key == Key.DirectionDown) 1 else -1).coerceIn(0, current.lastIndex)
                requesterFor(current[next]).tryRequestFocus()
                true
            }
            Key.DirectionLeft, Key.DirectionRight -> true
            Key.Back -> {
                actions.close()
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                OnboardingMascot()
                Spacer(Modifier.width(Dimens.GuideMascotGap))
                Text(
                    stringResource(R.string.launcher_guide_title),
                    style = TextStyles.SettingsTitle,
                    color = SygixColors.OnDark,
                    modifier = Modifier.testTag("onboarding-title"),
                )
            }
            Text(stringResource(R.string.launcher_guide_intro), style = SygixTypography.bodyMedium, color = SygixColors.OnDarkSecondary)
            if (progress.total > 0) {
                Text(
                    pluralStringResource(R.plurals.launcher_guide_progress, LauncherGuide.progressQuantity(progress.done), progress.done, progress.total),
                    style = TextStyles.RowState,
                    color = SygixColors.OnDarkSecondary,
                    modifier = Modifier.testTag("onboarding-progress"),
                )
            }
        }
        steps.forEach { step ->
            GuideStepRow(
                step = step,
                requester = requesterFor(step.step.tag()),
                onFocused = { focused = step.step.tag() },
                onClick = { actions.activate(step.step) },
            )
        }
        Row(
            Modifier.fillMaxWidth().padding(start = Dimens.OnboardingHeaderInsetH, top = Dimens.OnboardingFooterTop),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.launcher_guide_hint),
                style = TextStyles.RowSecondary,
                color = SygixColors.OnDarkSecondary,
                modifier = Modifier.testTag("onboarding-hint"),
            )
            GuideDoneButton(
                requester = requesterFor(DoneTarget),
                onFocused = { focused = DoneTarget },
                onClick = actions.close,
            )
        }
    }
}

@Composable
private fun GuideStepRow(step: GuideStepState, requester: FocusRequester, onFocused: () -> Unit, onClick: () -> Unit) {
    val tag = step.step.tag()
    PreferenceRow(
        label = stringResource(step.step.titleRes()),
        enabled = true,
        onClick = onClick,
        focusRequester = requester,
        detail = stringResource(step.detail.textRes()),
        partsTag = tag,
        dimmed = step.dimmed,
        onFocused = { if (it) onFocused() },
        leading = { colors -> StepMarker(step, colors, tag) },
        modifier = Modifier
            .testTag(tag)
            .semantics(mergeDescendants = true) {
                step.checked?.let {
                    role = Role.Switch
                    toggleableState = ToggleableState(it)
                }
                onClick { onClick(); true }
            },
    ) { colors ->
        Spacer(Modifier.width(RowGap))
        val checked = step.checked
        if (checked != null) {
            AppleSwitch(checked, tag = "$tag-switch")
        } else {
            StateLabel(stringResource(step.status.labelRes()), step.status.dotColor(), colors.tertiary, tag)
        }
    }
}

@Composable
private fun StepMarker(step: GuideStepState, colors: PillColors, tag: String) {
    val sub = step.step == GuideStep.OVERLAY
    if (sub) Spacer(Modifier.width(Dimens.GuideSubStepIndent))
    val size = if (sub) Dimens.GuideSubStepMarker else Dimens.GuideStepMarker
    val marker = Modifier.testTag("$tag-marker").size(size)
    if (step.status == StepStatus.DONE) {
        Box(marker.background(SygixColors.SwitchOn, CircleShape), contentAlignment = Alignment.Center) {
            CheckMark(Modifier.fillMaxSize().testTag("$tag-check"))
        }
    } else {
        Box(marker.border(Dimens.GuideMarkerStroke, colors.tertiary, CircleShape), contentAlignment = Alignment.Center) {
            Text(stepNumber(step.step), style = if (sub) TextStyles.RowSecondary else TextStyles.RowState, color = colors.tertiary)
        }
    }
    Spacer(Modifier.width(Dimens.GuideMarkerGap))
}

@Composable
private fun stepNumber(step: GuideStep): String = when (step) {
    GuideStep.ACCESSIBILITY -> 1.toString()
    GuideStep.BOOT -> 2.toString()
    GuideStep.OVERLAY -> stringResource(R.string.launcher_step_overlay_marker)
    GuideStep.HOME_ROLE -> 3.toString()
}

@Composable
private fun CheckMark(modifier: Modifier) {
    Canvas(modifier) {
        val stroke = Dimens.GuideMarkerStroke.toPx() * CheckStrokeScale
        drawLine(SygixColors.OnDark, Offset(size.width * .3f, size.height * .52f), Offset(size.width * .45f, size.height * .66f), stroke, StrokeCap.Round)
        drawLine(SygixColors.OnDark, Offset(size.width * .45f, size.height * .66f), Offset(size.width * .72f, size.height * .36f), stroke, StrokeCap.Round)
    }
}

private const val CheckStrokeScale = 1.4f

@Composable
private fun GuideDoneButton(requester: FocusRequester, onFocused: () -> Unit, onClick: () -> Unit) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused, rest = SygixColors.PillRest)
    Box(
        Modifier
            .testTag(DoneTarget)
            .tvFocusable(focusRequester = requester, onFocused = { focused = it; if (it) onFocused() })
            .tvClickable(onClick = onClick)
            .focusPill(colors, RoundedCornerShape(Dimens.PillCorner))
            .height(Dimens.MenuActionHeight)
            .padding(horizontal = Dimens.OnboardingButtonPaddingH),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.launcher_guide_done), style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
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
