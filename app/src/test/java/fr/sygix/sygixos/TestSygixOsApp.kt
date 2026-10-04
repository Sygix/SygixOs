/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

class TestSygixOsApp : SygixOsApp() {
    override val prefetchesMascot: Boolean get() = false
}
