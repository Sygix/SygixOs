/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import fr.sygix.sygixos.data.HomeStateReader
import fr.sygix.sygixos.data.AndroidLauncherSystemGateway
import fr.sygix.sygixos.domain.LauncherSystemGateway
import fr.sygix.sygixos.model.HomeScreenSnapshot

class TestSygixOsApp : SygixOsApp() {
    var bootCount: Int? = null
    override val prefetchesMascot: Boolean get() = false
    override val checksAccessibility: Boolean get() = false
    override fun createHomeStateReader(): HomeStateReader = HomeStateReader { Result.success(HomeScreenSnapshot()) }
    override fun createLauncherSystemGateway(): LauncherSystemGateway =
        AndroidLauncherSystemGateway(this, readBootCount = { bootCount }, check = accessibilityCheck)
}
