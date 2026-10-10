/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.model.SystemControl
import fr.sygix.sygixos.model.SystemControlState
import fr.sygix.sygixos.model.SystemRowDetail
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherSystemTextsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `titles details and states use the shared french strings`() {
        assertEquals(
            listOf("Remplacer le launcher", "Démarrer à l’allumage", "Retour à l’accueil"),
            SystemControl.entries.map { context.getString(it.titleRes()) },
        )
        assertEquals(
            listOf(
                "SygixOs devient l’écran d’accueil de la TV",
                "Cette TV ne permet pas de changer l’écran d’accueil",
                "Ouvrir SygixOs quand la TV s’allume",
                "SygixOs ne s’est pas ouvert automatiquement à ce démarrage",
                "Cette TV ne permet pas d’autoriser l’ouverture au démarrage",
                "Revenir à SygixOs quand la touche Home ouvre l’accueil du constructeur",
                "Les réglages d’accessibilité ne peuvent pas s’ouvrir sur cette TV",
            ),
            SystemRowDetail.entries.map { context.getString(it.textRes()) },
        )
        assertEquals(
            listOf("Actif", "Inactif", "Indisponible", "En attente…"),
            SystemControlState.entries.map { context.getString(it.labelRes()) },
        )
    }

    @Test
    fun `only active and pending states carry a dot`() {
        assertEquals(SygixColors.SwitchOn, SystemControlState.ACTIVE.dotColor())
        assertEquals(SygixColors.Badge, SystemControlState.PENDING.dotColor())
        assertNull(SystemControlState.INACTIVE.dotColor())
        assertNull(SystemControlState.UNAVAILABLE.dotColor())
    }

    @Test
    fun `unavailable title is dimmed only without focus`() {
        assertEquals(SygixColors.OnDarkSecondary, preferenceLabelColor(focused = false, dimmed = true))
        assertEquals(preferenceLabelColor(focused = true), preferenceLabelColor(focused = true, dimmed = true))
        assertEquals(SygixColors.OnDark, preferenceLabelColor(focused = false))
    }
}
