/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KeystoreTrackingSession(context: Context, private val keys: TrackingSessionKeys = AndroidTrackingSessionKeys(),
    private val dispatcher: kotlinx.coroutines.CoroutineDispatcher = Dispatchers.IO,
) : TrackingSessionStore {
    private val file = android.util.AtomicFile(context.noBackupFilesDir.resolve("tracking-session"))

    override suspend fun read(): String? = withContext(dispatcher) {
        if (!file.baseFile.exists()) return@withContext null
        try {
            val data = file.openRead().use { it.readNBytes(8193) }
            if (data.size !in 29..8192) throw TrackingException(TrackingFailure.STORAGE)
            val key = keys.read() ?: run { file.delete(); return@withContext null }
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, data.copyOfRange(0, 12)))
            cipher.doFinal(data.copyOfRange(12, data.size)).toString(Charsets.UTF_8)
        } catch (error: Exception) {
            if (error is javax.crypto.AEADBadTagException || error is android.security.keystore.KeyPermanentlyInvalidatedException) {
                if (error is android.security.keystore.KeyPermanentlyInvalidatedException) {
                    try { keys.delete() }
                    catch (_: Exception) { throw TrackingException(TrackingFailure.STORAGE) }
                }
                file.delete()
                return@withContext null
            }
            throw TrackingException(TrackingFailure.STORAGE)
        }
    }

    override suspend fun write(token: String?) = withContext(dispatcher) {
        if (token == null) {
            try { keys.delete() }
            finally { file.delete() }
            if (file.baseFile.exists()) throw TrackingException(TrackingFailure.STORAGE)
            return@withContext
        }
        var output: java.io.FileOutputStream? = null
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, keys.read() ?: keys.create())
            val encrypted = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            output = file.startWrite()
            output.write(encrypted)
            file.finishWrite(output)
        } catch (_: Exception) { file.failWrite(output); throw TrackingException(TrackingFailure.STORAGE) }
    }
}
