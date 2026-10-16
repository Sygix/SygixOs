/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.perf

import android.graphics.Bitmap
import android.os.Bundle
import android.os.Debug
import android.os.Process
import android.os.SystemClock
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.lifecycle.lifecycleScope
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.data.AppArtwork
import fr.sygix.sygixos.data.AppArtworkSource
import fr.sygix.sygixos.data.Catalog
import fr.sygix.sygixos.model.TvApp
import fr.sygix.sygixos.ui.home.HeroState
import fr.sygix.sygixos.ui.home.LauncherHome
import fr.sygix.sygixos.ui.home.LocalAppArtwork
import fr.sygix.sygixos.ui.home.Zone
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.json.JSONObject

class GridProfileActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val size = intent.getIntExtra("apps", 200).takeIf { it in Sizes } ?: 200
        val measure = intent.getBooleanExtra("measure", false)
        val catalog = Catalog(emptyList(), (0 until size).map { TvApp("fixture.app$it", "Fixture $it") })
        var sample by mutableStateOf<ProfileSample?>(null)
        setContent {
            SygixOsTheme {
                val current = sample
                if (current != null) {
                    CompositionLocalProvider(LocalAppArtwork provides current.artworks) {
                        Box(Modifier.fillMaxSize().semantics { testTagsAsResourceId = true }.drawWithContent {
                            drawContent()
                            current.drawn.complete(Unit)
                        }) {
                            LauncherHome(catalog, HeroState(emptyList(), false), {}, initialZone = Zone.GRID, glassBlur = false)
                        }
                    }
                }
            }
        }
        lifecycleScope.launch {
            repeat(if (measure) 7 else 1) { index ->
                sample = null
                delay(500)
                val current = ProfileSample(FixtureArtworks(this@GridProfileActivity))
                val cpu = Process.getElapsedCpuTime()
                val allocated = allocatedBytes()
                val start = SystemClock.elapsedRealtimeNanos()
                sample = current
                current.drawn.await()
                val firstDraw = SystemClock.elapsedRealtimeNanos() - start
                delay(1_000)
                val memory = Debug.MemoryInfo().also(Debug::getMemoryInfo)
                Log.i("GridProfile", JSONObject().apply {
                    put("apps", size)
                    put("sample", index - 2)
                    put("warmup", index < 2)
                    put("firstDrawNanos", firstDraw)
                    put("cpuMillis", Process.getElapsedCpuTime() - cpu)
                    put("allocatedBytes", allocatedBytes() - allocated)
                    put("totalPssKiB", memory.totalPss)
                    put("nativePssKiB", memory.nativePss)
                    put("tilesShown", current.artworks.shown.size)
                }.toString())
            }
        }
    }

    private fun allocatedBytes(): Long = Debug.getRuntimeStat("art.gc.bytes-allocated")?.toLongOrNull() ?: 0L

    private companion object {
        val Sizes = setOf(20, 60, 200, 2_000)
    }
}

private class ProfileSample(val artworks: FixtureArtworks) {
    val drawn = CompletableDeferred<Unit>()
}

private class FixtureArtworks(activity: ComponentActivity) : AppArtworkSource(activity.packageManager) {
    val shown: MutableSet<String> = ConcurrentHashMap.newKeySet()
    private val artwork = AppArtwork(Bitmap.createBitmap(16, 9, Bitmap.Config.ARGB_8888).apply { eraseColor(0xff405060.toInt()) }, true)
    override fun cached(app: TvApp): AppArtwork {
        shown += app.packageName
        return artwork
    }
}
