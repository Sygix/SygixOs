/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.mikepenz.aboutlibraries.ui.compose.android.produceLibraries
import com.mikepenz.aboutlibraries.ui.compose.m3.LibrariesContainer
import fr.sygix.sygixos.R

// Catégorie « À propos » : version du launcher et licences OSS (aboutlibraries).
@Composable
internal fun AboutContent(
    version: String,
    focusEnabled: Boolean,
    contentFocus: androidx.compose.ui.focus.FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .testTag("settings-about")
            .fillMaxWidth()
            .fillMaxHeight(),
    ) {
        Text("À propos", style = MaterialTheme.typography.headlineSmall, color = Color.White)
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
            "SygixOs est un logiciel libre distribué sous licence GNU GPL-3.0. " +
                "Le code source est disponible sur github.com/Sygix/SygixOs.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.7f),
        )
        Spacer(Modifier.height(24.dp))
        Text("Licences open source", style = MaterialTheme.typography.titleMedium, color = Color.White)
        Spacer(Modifier.height(8.dp))
        val libraries by produceLibraries(R.raw.aboutlibraries)
        // contentFocus attaché au premier élément focalisable (liste des licences) :
        // le passage en volet CONTENT a toujours quelque chose à focaliser.
        LibrariesContainer(
            libraries = libraries,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("about-licenses")
                .focusRequester(contentFocus)
                .focusable(),
        )
    }
}
