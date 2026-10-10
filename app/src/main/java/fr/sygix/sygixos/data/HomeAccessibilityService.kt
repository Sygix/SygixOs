/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityEvent
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.domain.HomeKeyEvent
import fr.sygix.sygixos.domain.HomeKeyFilter

class HomeAccessibilityService : AccessibilityService() {
    private var connected = false
    private val handler by lazy { Handler(Looper.getMainLooper()) }
    private val filter by lazy {
        HomeKeyFilter(
            longPressMs = ViewConfiguration.getLongPressTimeout().toLong(),
            foreground = { (application as SygixOsApp).foregroundTracker.isForeground },
            openHome = { wasForeground ->
                handler.postDelayed({
                    runCatching { startActivity(LauncherSystemIntents.home(this, wasForeground)) }
                }, HOME_RETURN_DELAY_MS)
            },
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        connected = true
        running = true
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        if (event.keyCode != KeyEvent.KEYCODE_HOME || !connected) return false
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false
        return filter.filter(
            HomeKeyEvent(
                down = event.action == KeyEvent.ACTION_DOWN,
                repeatCount = event.repeatCount,
                longPress = event.isLongPress,
                canceled = event.isCanceled,
                eventTime = event.eventTime,
            ),
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit
    override fun onInterrupt() { filter.reset() }
    override fun onDestroy() {
        connected = false
        running = false
        filter.reset()
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object {
        private const val HOME_RETURN_DELAY_MS = 300L

        @Volatile var running: Boolean = false
            private set
    }
}
