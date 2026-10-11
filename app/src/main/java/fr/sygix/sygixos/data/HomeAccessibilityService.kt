/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import fr.sygix.sygixos.SygixOsApp
import fr.sygix.sygixos.domain.HomeKeyEvent
import fr.sygix.sygixos.domain.HomeKeyFilter

class HomeAccessibilityService : AccessibilityService() {
    private val filter by lazy {
        HomeKeyFilter(
            foreground = { (application as SygixOsApp).foregroundTracker.isForeground },
            openHome = { wasForeground -> runCatching { startActivity(LauncherSystemIntents.home(this, wasForeground)) } },
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        AccessibilityConnection.update(true)
        (application as? SygixOsApp)?.onAccessibilityConnected()
    }

    override fun onKeyEvent(event: KeyEvent): Boolean = filter.filter(
        HomeKeyEvent(
            home = event.keyCode == KeyEvent.KEYCODE_HOME,
            down = event.action == KeyEvent.ACTION_DOWN,
            repeatCount = event.repeatCount,
            canceled = event.isCanceled,
        ),
        AccessibilityConnection.connected.value,
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) = Unit

    override fun onInterrupt() = filter.reset()

    override fun onUnbind(intent: Intent?): Boolean {
        disconnect()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        disconnect()
        super.onDestroy()
    }

    private fun disconnect() {
        AccessibilityConnection.update(false)
        filter.reset()
    }
}
