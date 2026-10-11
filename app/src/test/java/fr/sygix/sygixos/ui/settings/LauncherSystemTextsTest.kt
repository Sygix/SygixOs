/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.SygixColors
import fr.sygix.sygixos.domain.LauncherGuide
import fr.sygix.sygixos.model.GuideStep
import fr.sygix.sygixos.model.StepDetail
import fr.sygix.sygixos.model.StepStatus
import fr.sygix.sygixos.model.SummaryDetail
import fr.sygix.sygixos.model.SummaryStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherSystemTextsTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun `steps details states and summaries use the french strings of the guided setup`() {
        assertEquals(
            listOf(
                "Revenir à SygixOs avec la touche Home",
                "Ouvrir SygixOs à l’allumage",
                "Autoriser l’ouverture au démarrage",
                "Définir SygixOs par défaut",
            ),
            GuideStep.entries.map { context.getString(it.titleRes()) },
        )
        assertEquals(
            "La TV a bloqué ce service au démarrage. Désactivez-le puis réactivez-le dans Accessibilité ; si la TV le propose, autorisez aussi le démarrage automatique de SygixOs.",
            context.getString(StepDetail.ACCESSIBILITY_BLOCKED.textRes()),
        )
        assertEquals("Cette TV ne permet pas d’autoriser l’ouverture au démarrage", context.getString(StepDetail.BOOT_UNAVAILABLE.textRes()))
        assertEquals(
            listOf("À faire", "En attente…", "Fait", "Bloqué", "Indisponible", "Facultatif"),
            StepStatus.entries.map { context.getString(it.labelRes()) },
        )
        assertEquals(
            listOf("Terminé", "Partiel", "À faire", "Bloqué", "Indisponible"),
            SummaryStatus.entries.map { context.getString(it.labelRes()) },
        )
        assertEquals(
            listOf(
                "Toutes les étapes sont faites",
                "Touche Home à terminer",
                "Démarrage à l’allumage à terminer",
                "Touche Home et démarrage à l’allumage à terminer",
                "Touche Home bloquée par la TV",
                "Cette TV ne permet pas ces réglages",
            ),
            SummaryDetail.entries.map { context.getString(it.textRes()) },
        )
    }

    @Test
    fun `progress agrees with the number of finished steps`() {
        val res = context.resources
        assertEquals("0 étape sur 2 terminée", res.getQuantityString(R.plurals.launcher_guide_progress, LauncherGuide.progressQuantity(0), 0, 2))
        assertEquals("1 étape sur 3 terminée", res.getQuantityString(R.plurals.launcher_guide_progress, LauncherGuide.progressQuantity(1), 1, 3))
        assertEquals("3 étapes sur 3 terminées", res.getQuantityString(R.plurals.launcher_guide_progress, LauncherGuide.progressQuantity(3), 3, 3))
    }

    @Test
    fun `only done and pending states carry a dot`() {
        assertEquals(SygixColors.SwitchOn, StepStatus.DONE.dotColor())
        assertEquals(SygixColors.Badge, StepStatus.PENDING.dotColor())
        listOf(StepStatus.TODO, StepStatus.BLOCKED, StepStatus.UNAVAILABLE, StepStatus.OPTIONAL).forEach { assertNull(it.dotColor()) }
        assertEquals(SygixColors.SwitchOn, SummaryStatus.DONE.dotColor())
        listOf(SummaryStatus.PARTIAL, SummaryStatus.TODO, SummaryStatus.BLOCKED, SummaryStatus.UNAVAILABLE).forEach { assertNull(it.dotColor()) }
    }

    @Test
    fun `unavailable title is dimmed only without focus`() {
        assertEquals(SygixColors.OnDarkSecondary, preferenceLabelColor(focused = false, dimmed = true))
        assertEquals(preferenceLabelColor(focused = true), preferenceLabelColor(focused = true, dimmed = true))
        assertEquals(SygixColors.OnDark, preferenceLabelColor(focused = false))
    }
}
