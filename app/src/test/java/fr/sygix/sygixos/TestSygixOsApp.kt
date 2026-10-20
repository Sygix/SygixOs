/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import fr.sygix.sygixos.data.HomeStateReader
import fr.sygix.sygixos.domain.LauncherSystemStore
import fr.sygix.sygixos.model.HomeScreenSnapshot

class TestSygixOsApp : SygixOsApp() {
    var boot: Int? = null
    var launcherSystemStore: LauncherSystemStore? = null
    override val prefetchesMascot: Boolean get() = false
    override val checksAccessibility: Boolean get() = false
    override val applicationDispatcher get() = kotlinx.coroutines.Dispatchers.Unconfined
    override fun bootCount() = boot
    override fun createHomeStateReader(): HomeStateReader = HomeStateReader { Result.success(HomeScreenSnapshot()) }
    override fun createLauncherSystemStore() = launcherSystemStore ?: super.createLauncherSystemStore()
    override fun createHeroVideo(onEnded: () -> Unit, onError: (String) -> Unit) = fr.sygix.sygixos.ui.hero.FakeVideo()
}
