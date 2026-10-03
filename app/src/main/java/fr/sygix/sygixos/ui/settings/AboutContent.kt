/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.entity.Library
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.TextStyles
import fr.sygix.sygixos.core.designsystem.animatedPillColors
import fr.sygix.sygixos.core.designsystem.focusPill
import fr.sygix.sygixos.core.designsystem.tvFocusable

@Composable
internal fun AboutContent(
    version: String,
    listState: LazyListState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .testTag("settings-about")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
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
        Spacer(Modifier.height(24.dp))
        Text("Licences open source", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        val libraries by produceLibraries(R.raw.aboutlibraries)
        val entries = libraries?.libraries
        when {
            entries == null -> AboutStatus("Chargement des licences…", "about-licenses-loading")
            entries.isEmpty() -> AboutStatus("Aucune licence à afficher", "about-licenses-empty")
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxWidth().weight(1f).testTag("about-licenses"),
            ) {
                itemsIndexed(entries, key = { _, library -> library.uniqueId }) { index, library ->
                    LicenseRow(
                        library = library,
                        focusEnabled = focusEnabled,
                        focusRequester = if (index == 0) contentFocus else null,
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
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
    focusRequester: FocusRequester?,
) {
    var focused by remember { mutableStateOf(false) }
    val colors = animatedPillColors(focused)
    val licenses = library.licenses.joinToString { it.name }
    Column(
        Modifier
            .testTag("license-row-${library.uniqueId}")
            .fillMaxWidth()
            .tvFocusable(focusRequester = focusRequester, enabled = focusEnabled, onFocused = { focused = it })
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
