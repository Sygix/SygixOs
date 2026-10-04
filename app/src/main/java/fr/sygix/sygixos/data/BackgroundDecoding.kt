/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.os.Process
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher

val BackgroundDecoding: CoroutineDispatcher = Executors.newSingleThreadExecutor { task ->
    Thread({
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        task.run()
    }, "ArtworkDecoder").apply { isDaemon = true }
}.asCoroutineDispatcher()
