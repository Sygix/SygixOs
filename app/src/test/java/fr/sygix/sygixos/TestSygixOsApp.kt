/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import fr.sygix.sygixos.data.HomeStateReader
import fr.sygix.sygixos.model.HomeScreenSnapshot

class TestSygixOsApp : SygixOsApp() {
    override val prefetchesMascot: Boolean get() = false
    override val checksAccessibility: Boolean get() = false
    override fun createHomeStateReader(): HomeStateReader = HomeStateReader { Result.success(HomeScreenSnapshot()) }
}
