/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import fr.sygix.sygixos.domain.AppLanguage
import fr.sygix.sygixos.domain.LanguageState

interface AppLanguageGateway {
    fun current(): LanguageState
    fun select(language: AppLanguage?)
}

class AndroidAppLanguageGateway(context: Context) : AppLanguageGateway {
    private val manager = context.getSystemService(LocaleManager::class.java)

    override fun current(): LanguageState = LanguageState(
        selected = manager.applicationLocales.toList().firstOrNull()?.let(AppLanguage::of),
        system = AppLanguage.resolve(manager.systemLocales.toList()),
    )

    override fun select(language: AppLanguage?) {
        manager.applicationLocales = language?.let { LocaleList.forLanguageTags(it.tag) } ?: LocaleList.getEmptyLocaleList()
    }

    private fun LocaleList.toList() = List(size()) { get(it) }
}
