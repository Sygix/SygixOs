/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import fr.sygix.sygixos.data.AppLanguageGateway
import fr.sygix.sygixos.domain.AppLanguage
import fr.sygix.sygixos.domain.LanguageState

internal class FakeLanguageGateway(var state: LanguageState = LanguageState(system = AppLanguage.FRENCH)) : AppLanguageGateway {
    val selections = mutableListOf<AppLanguage?>()

    override fun current(): LanguageState = state

    override fun select(language: AppLanguage?) {
        selections += language
        state = state.copy(selected = language)
    }
}
