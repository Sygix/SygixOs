/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.text.format.DateFormat
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Date
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

interface ClockSource {
    fun current(): String
    fun time(): Flow<String>
}

class SystemClockSource(
    private val context: Context,
    private val now: () -> Long = System::currentTimeMillis,
) : ClockSource {

    override fun current(): String = DateFormat.getTimeFormat(context).format(Date(now()))

    override fun time(): Flow<String> = callbackFlow {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                trySend(current())
            }
        }
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }
        val registered = runCatching {
            ContextCompat.registerReceiver(context, receiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        }.onFailure { Log.w(TAG, "horloge sans mise à jour", it) }.isSuccess
        trySend(current())
        awaitClose {
            if (registered) runCatching { context.unregisterReceiver(receiver) }
        }
    }

    private companion object {
        const val TAG = "SystemClockSource"
    }
}
