/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.core.designsystem.tryRequestFocus
import fr.sygix.sygixos.domain.AppLanguage
import fr.sygix.sygixos.domain.LanguageState

@Composable
internal fun LanguageContent(
    state: LanguageState,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    onSelect: (AppLanguage?) -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(focusEnabled) {
        if (focusEnabled) {
            withFrameNanos { }
            contentFocus.tryRequestFocus()
        }
    }
    Column(modifier.fillMaxWidth().testTag("settings-language")) {
        SettingsHeader(R.string.settings_category_language, R.string.language_description)
        LanguageRow(
            label = stringResource(R.string.language_system),
            detail = state.system?.nativeName ?: stringResource(R.string.language_system_unsupported, AppLanguage.Fallback.nativeName),
            selected = state.selected == null,
            focusEnabled = focusEnabled,
            contentFocus = contentFocus,
            tag = "setting-language-system",
            edge = Modifier.focusProperties { up = FocusRequester.Cancel },
            onClick = { onSelect(null) },
        )
        AppLanguage.entries.forEach { language ->
            Spacer(Modifier.height(RowSpacing))
            LanguageRow(
                label = language.nativeName,
                detail = null,
                selected = state.selected == language,
                focusEnabled = focusEnabled,
                contentFocus = contentFocus,
                tag = "setting-language-${language.tag}",
                edge = if (language == AppLanguage.entries.last()) Modifier.focusProperties { down = FocusRequester.Cancel } else Modifier,
                onClick = { onSelect(language) },
            )
        }
    }
}

@Composable
private fun LanguageRow(
    label: String,
    detail: String?,
    selected: Boolean,
    focusEnabled: Boolean,
    contentFocus: FocusRequester,
    tag: String,
    edge: Modifier,
    onClick: () -> Unit,
) {
    PreferenceRow(
        label = label,
        detail = detail,
        enabled = focusEnabled,
        onClick = onClick,
        focusRequester = contentFocus.takeIf { selected },
        modifier = edge.testTag(tag).semantics { this.selected = selected },
    ) { colors ->
        Spacer(Modifier.width(RowGap))
        Box(Modifier.size(Dimens.DropdownCheck), contentAlignment = Alignment.Center) {
            if (selected) CheckGlyph(colors.content, Modifier.fillMaxSize().testTag("$tag-check"))
        }
    }
}
