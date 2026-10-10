/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.ResolveInfo
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import fr.sygix.sygixos.R
import fr.sygix.sygixos.model.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowToast

@RunWith(AndroidJUnit4::class)
class UpNextLauncherTest {
    private val base = ApplicationProvider.getApplicationContext<Context>()
    private val source = UpNextSourceEntry("com.source", "Source", intentUri = "https://example.org/play")
    private val item = UpNextItem(1, source, UpNextContentType.MOVIE, "Movie")
    private val started = mutableListOf<Intent>()
    private fun context(failure: RuntimeException? = null): Context = object : ContextWrapper(base) {
        override fun startActivity(intent: Intent) {
            started += intent
            if (intent.data != null && failure != null) throw failure
        }
    }
    private fun fallback() {
        val info = ResolveInfo().apply { activityInfo = ActivityInfo().apply {
            packageName = source.packageName
            name = "com.source.MainActivity"
            applicationInfo = ApplicationInfo().apply { packageName = source.packageName; enabled = true }
            enabled = true
        } }
        shadowOf(base.packageManager).addResolveInfoForIntent(Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LEANBACK_LAUNCHER).setPackage(source.packageName), info)
    }
    @Test fun `published intent opens first constrained to selected package`() {
        AppLauncher.open(context(), item)
        assertEquals(1, started.size)
        assertEquals(source.packageName, started.single().`package`)
        assertEquals("https://example.org/play", started.single().data.toString())
        assertTrue(started.single().flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
        assertEquals(0, ShadowToast.shownToastCount())
    }
    @Test fun `absent published intent uses leanback fallback without toast`() {
        fallback()
        AppLauncher.open(context(), UpNextItem(1, source.copy(intentUri = null), UpNextContentType.MOVIE, "Movie"))
        assertEquals(source.packageName, started.single().component?.packageName)
        assertEquals(0, ShadowToast.shownToastCount())
    }
    @Test fun `activity not found on a card falls back silently without crashing`() {
        fallback()
        AppLauncher.open(context(ActivityNotFoundException()), item)
        assertEquals(2, started.size)
        assertEquals(source.packageName, started.last().component?.packageName)
        assertEquals(0, ShadowToast.shownToastCount())
    }
    @Test fun `security exception on a card falls back silently without crashing`() {
        fallback()
        AppLauncher.open(context(SecurityException()), item)
        assertEquals(2, started.size)
        assertEquals(source.packageName, started.last().component?.packageName)
        assertEquals(0, ShadowToast.shownToastCount())
    }
    @Test fun `failed published intent and missing package show toast`() {
        AppLauncher.open(context(SecurityException()), item)
        assertEquals(base.getString(R.string.upnext_open_error, item.displayTitle), ShadowToast.getTextOfLatestToast())
    }
    @Test fun `failed intent chosen from the menu launches the source app and shows a toast`() {
        fallback()
        AppLauncher.openWith(context(ActivityNotFoundException()), item, source)
        assertEquals(2, started.size)
        assertEquals(source.packageName, started.last().component?.packageName)
        assertEquals(base.getString(R.string.upnext_open_fallback, source.label), ShadowToast.getTextOfLatestToast())
    }
    @Test fun `menu entry without intent launches the source app without toast`() {
        fallback()
        AppLauncher.openWith(context(), item, source.copy(intentUri = null))
        assertEquals(source.packageName, started.single().component?.packageName)
        assertEquals(0, ShadowToast.shownToastCount())
    }
    @Test fun `selected merged source uses its own published intent`() {
        val other = UpNextSourceEntry("com.other", "Other", intentUri = "https://example.org/other")
        AppLauncher.openWith(context(), item.copy(sources = listOf(source, other)), other)
        assertEquals(other.packageName, started.single().`package`)
        assertEquals(other.intentUri, started.single().data.toString())
        assertEquals(0, ShadowToast.shownToastCount())
    }
}
