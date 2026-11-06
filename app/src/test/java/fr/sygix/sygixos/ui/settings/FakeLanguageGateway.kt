/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import fr.sygix.sygixos.data.AppLanguageGateway
import fr.sygix.sygixos.domain.AppLanguage
import fr.sygix.sygixos.domain.LanguageState

internal class FakeLanguageGateway(
    var state: LanguageState = LanguageState(system = AppLanguage.FRENCH),
    private val inBackground: () -> Boolean = { false },
) : AppLanguageGateway {
    val selections = mutableListOf<AppLanguage?>()
    val reads = mutableListOf<Boolean>()
    val writes = mutableListOf<Boolean>()

    override fun current(): LanguageState {
        reads += inBackground()
        return state
    }

    override fun select(language: AppLanguage?) {
        writes += inBackground()
        selections += language
        state = state.copy(selected = language)
    }
}
