/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppArtworkSourceTest {
    private val pm = ApplicationProvider.getApplicationContext<Context>().packageManager

    private fun install(packageName: String) {
        shadowOf(pm).installPackage(PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        })
        shadowOf(pm).setApplicationIcon(packageName, ShapeDrawable(RectShape()).apply {
            intrinsicWidth = 16
            intrinsicHeight = 16
        })
    }

    @Test fun unavailableArtworkCanBeLoadedAfterThePackageBecomesAvailable() {
        val source = AppArtworkSource(pm)
        val app = TvApp("com.artwork.late", "Late")
        assertNull(source.load(app))
        install(app.packageName)
        assertNotNull(source.load(app))
        assertNotNull(source.cached(app))
    }

    @Test fun evictingAnArtworkDoesNotRecycleTheBitmapStillUsedByTheUi() {
        val source = AppArtworkSource(pm, maxBytes = 16 * 16 * 4)
        val first = TvApp("com.artwork.first", "First")
        val second = TvApp("com.artwork.second", "Second")
        install(first.packageName)
        install(second.packageName)
        val displayed = requireNotNull(source.load(first))
        assertSame(displayed, source.load(first))
        val latest = requireNotNull(source.load(second))
        assertNull(source.cached(first))
        assertSame(latest, source.cached(second))
        assertFalse(displayed.bitmap.isRecycled)
    }
}
