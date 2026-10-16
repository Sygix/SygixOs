/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.shapes.RectShape
import android.net.Uri
import androidx.test.core.app.ApplicationProvider
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.domain.ImageBounds
import fr.sygix.sygixos.domain.PixelSize
import fr.sygix.sygixos.model.TvApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class AppArtworkSourceTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val pm = context.packageManager

    private fun install(packageName: String, side: Int = ICON_SIDE) {
        shadowOf(pm).installPackage(PackageInfo().apply {
            this.packageName = packageName
            applicationInfo = ApplicationInfo().apply { this.packageName = packageName }
        })
        shadowOf(pm).setApplicationIcon(packageName, ShapeDrawable(RectShape()).apply {
            intrinsicWidth = side
            intrinsicHeight = side
        })
    }

    private fun apps(count: Int): List<TvApp> = List(count) { TvApp("com.artwork.$it", "App $it").also { app -> install(app.packageName) } }

    @Test fun unavailableArtworkIsNotRememberedAsMissing() {
        val source = AppArtworkSource(pm)
        val app = TvApp("com.artwork.late", "Late")
        assertNull(source.load(app))
        install(app.packageName)
        assertNotNull(source.load(app))
        assertNotNull(source.cached(app))
    }

    @Test fun artworkIsDecodedWithinTheTileBounds() {
        val source = AppArtworkSource(pm, bounds = PixelSize(12, 7))
        val app = apps(1).single()
        val artwork = requireNotNull(source.load(app))
        assertEquals(7, artwork.bitmap.width)
        assertEquals(7, artwork.bitmap.height)
    }

    @Test fun preloadFillsTheBudgetInOrderWithoutEvicting() {
        val source = AppArtworkSource(pm, maxBytes = ImageBounds.MAX_ARTWORK_WIDTH * ImageBounds.MAX_ARTWORK_HEIGHT * 4 + 2 * ICON_BYTES)
        val catalog = apps(5)
        source.preload(catalog)
        val kept = catalog.map(source::cached)
        kept.take(3).forEach { assertNotNull(it) }
        kept.drop(3).forEach { assertNull(it) }
        source.preload(catalog)
        assertEquals(kept, catalog.map(source::cached))
    }

    @Test fun preloadStopsAfterTheFirstRows() {
        val source = AppArtworkSource(pm)
        val catalog = apps(AppArtworkSource.PRELOAD_LIMIT + 1)
        source.preload(catalog)
        assertNotNull(source.cached(catalog[AppArtworkSource.PRELOAD_LIMIT - 1]))
        assertNull(source.cached(catalog.last()))
    }

    @Test fun onDemandLoadsBeyondTheBudgetEvictTheOldestWithoutRecyclingIt() {
        val source = AppArtworkSource(pm, maxBytes = ICON_BYTES)
        val (first, second) = apps(2)
        val displayed = requireNotNull(source.load(first))
        assertSame(displayed, source.load(first))
        val latest = requireNotNull(source.load(second))
        assertNull(source.cached(first))
        assertSame(latest, source.cached(second))
        assertFalse(displayed.bitmap.isRecycled)
    }

    @Test fun invalidatingAPackageDecodesItAgainAndNotifies() {
        val source = AppArtworkSource(pm)
        val app = apps(1).single()
        val stale = requireNotNull(source.load(app))
        install(app.packageName, side = ICON_SIDE * 2)
        source.invalidate(app.packageName)
        assertNull(source.cached(app))
        assertEquals(1, source.changes.value)
        val fresh = requireNotNull(source.load(app))
        assertNotSame(stale, fresh)
        assertEquals(ICON_SIDE * 2, fresh.bitmap.width)
    }

    @Test fun packageBroadcastsInvalidateTheSharedSource() {
        val source = (context as SygixOsApp).appArtwork
        val app = apps(1).single()
        assertNotNull(source.load(app))
        context.sendBroadcast(Intent(Intent.ACTION_PACKAGE_REPLACED, Uri.parse("package:" + app.packageName)))
        shadowOf(context.mainLooper).idle()
        assertNull(source.cached(app))
        assertEquals(1, source.changes.value)
    }

    private companion object {
        const val ICON_SIDE = 16
        const val ICON_BYTES = ICON_SIDE * ICON_SIDE * 4
    }
}
