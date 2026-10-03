/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.core.designsystem.tvClickable
import fr.sygix.sygixos.core.designsystem.tvFocusable
import fr.sygix.sygixos.domain.QrCode

private val QrSize = 120.dp
private val QrGap = 16.dp
private const val INSTALL_ROW = 1

@Composable
internal fun AboutContent(
    version: String,
    update: AboutUpdateState,
    actions: UpdateActions,
    listState: LazyListState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val installFocus = remember { FocusRequester() }
    val installLine = update.installLine
    val focusActive by rememberUpdatedState(focusEnabled)
    val initialReturns = remember { update.systemScreenReturns }
    LaunchedEffect(update.systemScreenReturns) {
        if (update.systemScreenReturns == initialReturns || !focusActive) return@LaunchedEffect
        if (installLine == null || !installFocus.tryRequestFocus()) contentFocus.tryRequestFocus()
    }
    val libraries by produceLibraries(R.raw.aboutlibraries)
    val entries = libraries?.libraries
    LazyColumn(
        state = listState,
        modifier = modifier
            .testTag("settings-about")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        item(key = "about-header") { AboutHeader(version) }
        item(key = "update-block") {
            UpdateBlock(qr = update.releaseNotesQr.takeIf { installLine != null }) {
                AboutActionRow(
                    title = stringResource(R.string.update_check_title),
                    detail = checkLineText(update.checkLine),
                    focusEnabled = focusEnabled,
                    focusRequester = contentFocus,
                    onClick = actions.onCheck,
                    modifier = Modifier.testTag("update-check"),
                )
                if (installLine != null) {
                    AboutActionRow(
                        title = stringResource(R.string.update_install_title, installLine.candidate.versionName),
                        detail = installLine.detail?.let { installDetailText(it) },
                        focusEnabled = focusEnabled,
                        focusRequester = installFocus,
                        onClick = { actions.onInstall(installLine.candidate.tag) },
                        modifier = Modifier.testTag("update-install"),
                    )
                }
                update.withdrawn?.let { withdrawn ->
                    AboutActionRow(
                        title = stringResource(R.string.update_install_title, withdrawn.versionName),
                        detail = stringResource(R.string.update_error_withdrawn),
                        focusEnabled = focusEnabled,
                        focusRequester = null,
                        onClick = {},
                        modifier = Modifier.testTag("update-withdrawn"),
                    )
                }
                PrereleasesRow(
                    checked = update.includePrereleases,
                    focusEnabled = focusEnabled,
                    onToggle = actions.onTogglePrereleases,
                )
            }
            Spacer(Modifier.height(24.dp))
        }
        item(key = "about-licenses-title") {
            Text(
                "Licences open source",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                modifier = Modifier.testTag("about-licenses"),
            )
            Spacer(Modifier.height(8.dp))
        }
        when {
            entries == null -> item(key = "about-licenses-loading") { AboutStatus("Chargement des licences…", "about-licenses-loading") }
            entries.isEmpty() -> item(key = "about-licenses-empty") { AboutStatus("Aucune licence à afficher", "about-licenses-empty") }
            else -> items(entries, key = { it.uniqueId }) { library ->
                LicenseRow(
                    library = library,
                    focusEnabled = focusEnabled,
                    )
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun UpdateBlock(qr: QrCode?, rows: @Composable () -> Unit) {
    Layout(
        content = {
            rows()
            if (qr != null) ReleaseNotesQr(qr)
        },
    ) { measurables, constraints ->
        val qrPlaceable = if (qr != null) measurables.last().measure(Constraints()) else null
        val rowMeasurables = if (qr != null) measurables.dropLast(1) else measurables
        val reserved = qrPlaceable?.let { it.width + QrGap.roundToPx() } ?: 0
        val rowWidth = (constraints.maxWidth - reserved).coerceAtLeast(0)
        val rowPlaceables = rowMeasurables.map { it.measure(Constraints.fixedWidth(rowWidth)) }
        val gap = RowSpacing.roundToPx()
        val tops = rowPlaceables.runningFold(0) { top, placeable -> top + placeable.height + gap }
        val rowsBottom = (tops.last() - gap).coerceAtLeast(0)
        val qrTop = tops.getOrElse(INSTALL_ROW) { 0 }
        val height = maxOf(rowsBottom, qrPlaceable?.let { qrTop + it.height } ?: 0)
        layout(constraints.maxWidth, height) {
            rowPlaceables.forEachIndexed { index, placeable -> placeable.place(0, tops[index]) }
            qrPlaceable?.place(constraints.maxWidth - qrPlaceable.width, qrTop)
        }
    }
}

@Composable
private fun AboutHeader(version: String) {
    Column {
        Text(stringResource(R.string.settings_category_about), style = MaterialTheme.typography.headlineSmall, color = Color.White)
        Spacer(Modifier.height(12.dp))
        Text(
            "SygixOs",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.testTag("about-version"),
        )
        Text(
            "Version $version",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            "SygixOs est un logiciel libre distribué sous licence GNU AGPL-3.0. " +
                "Le code source est disponible sur github.com/Sygix/SygixOs.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(20.dp))
    }
}

@Composable
private fun AboutActionRow(
    title: String,
    detail: String?,
    focusEnabled: Boolean,
    focusRequester: FocusRequester?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    Column(
        modifier
            .fillMaxWidth()
            .tvFocusable(
                focusRequester = focusRequester,
                enabled = focusEnabled,
                onFocused = { focused = it },
            )
            .tvClickable(onClick = onClick)
            .semantics(mergeDescendants = true) { onClick { onClick(); true } }
            .focusPill(colors, RowShape)
            .heightIn(min = RowHeight)
            .padding(horizontal = RowPadding, vertical = RowVerticalPadding),
        verticalArrangement = Arrangement.Center,
    ) {
        Text(title, style = if (focused) TextStyles.RowFocused else TextStyles.Row, color = colors.content)
        if (detail != null) Text(detail, style = TextStyles.RowSecondary, color = colors.secondary)
    }
}

@Composable
private fun PrereleasesRow(
    checked: Boolean,
    focusEnabled: Boolean,
    onToggle: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    Row(
        Modifier
            .testTag("update-prereleases")
            .fillMaxWidth()
            .tvFocusable(
                enabled = focusEnabled,
                onFocused = { focused = it },
            )
            .tvClickable(onClick = onToggle)
            .semantics(mergeDescendants = true) {
                role = Role.Switch
                toggleableState = ToggleableState(checked)
                onClick { onToggle(); true }
            }
            .focusPill(colors, RowShape)
            .heightIn(min = RowHeight)
            .padding(horizontal = RowPadding, vertical = RowVerticalPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.update_prereleases),
            style = if (focused) TextStyles.RowFocused else TextStyles.Row,
            color = colors.content,
            modifier = Modifier.weight(1f),
        )
        AppleSwitch(checked = checked, tag = "update-prereleases-switch")
    }
}

@Composable
private fun ReleaseNotesQr(qr: QrCode) {
    Column(
        Modifier.testTag("update-release-notes-qr"),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(QrSize)) {
            drawRect(SygixColors.QrLight)
            val module = size.width / qr.size
            for (y in 0 until qr.size) {
                for (x in 0 until qr.size) {
                    if (qr[x, y]) drawRect(SygixColors.QrDark, Offset(x * module, y * module), Size(module, module))
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(stringResource(R.string.update_release_notes), style = TextStyles.RowSecondary, color = SygixColors.OnDarkSecondary)
    }
}

@Composable
private fun AboutStatus(label: String, tag: String) {
    Text(
        label,
        style = MaterialTheme.typography.bodyMedium,
        color = Color.White.copy(alpha = 0.5f),
        modifier = Modifier.testTag(tag),
    )
}

@Composable
private fun LicenseRow(
    library: Library,
    focusEnabled: Boolean,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    val licenses = library.licenses.joinToString { it.name }
    Column(
        Modifier
            .testTag("license-row-${library.uniqueId}")
            .fillMaxWidth()
            .tvFocusable(enabled = focusEnabled, onFocused = { focused = it })
            .focusable()
            .focusPill(colors, RowShape)
            .padding(horizontal = RowPadding, vertical = 8.dp),
    ) {
        Text(
            listOfNotNull(library.name, library.artifactVersion).joinToString(" "),
            style = if (focused) TextStyles.RowFocused else TextStyles.Row,
            color = colors.content,
        )
        Text(
            licenses.ifEmpty { "Licence non renseignée" },
            style = TextStyles.RowSecondary,
            color = colors.secondary,
        )
    }
}
