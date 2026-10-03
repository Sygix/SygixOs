/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.app.Activity
import android.app.Application
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

interface ForegroundState {
    val isForeground: Boolean
}

class ForegroundTracker : ForegroundState, Application.ActivityLifecycleCallbacks {

    private val started = AtomicInteger(0)

    override val isForeground: Boolean get() = started.get() > 0

    override fun onActivityStarted(activity: Activity) {
        started.incrementAndGet()
    }

    override fun onActivityStopped(activity: Activity) {
        started.updateAndGet { (it - 1).coerceAtLeast(0) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) = Unit
    override fun onActivityResumed(activity: Activity) = Unit
    override fun onActivityPaused(activity: Activity) = Unit
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit
    override fun onActivityDestroyed(activity: Activity) = Unit
}
