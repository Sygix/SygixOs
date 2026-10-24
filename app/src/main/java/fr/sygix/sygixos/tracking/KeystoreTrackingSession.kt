/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KeystoreTrackingSession(context: Context) : TrackingSessionStore {
    private val file = android.util.AtomicFile(context.noBackupFilesDir.resolve("tracking-session"))
    private val alias = "sygixos.tracking.session"

    private fun key(create: Boolean = true): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        if (!create) throw java.security.KeyStoreException()
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }

    override suspend fun read(): String? = withContext(Dispatchers.IO) {
        if (!file.baseFile.exists()) return@withContext null
        try {
            val data = file.openRead().use { it.readNBytes(8193) }
            if (data.size !in 29..8192) throw TrackingException(TrackingFailure.STORAGE)
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, key(create = false), GCMParameterSpec(128, data.copyOfRange(0, 12)))
            cipher.doFinal(data.copyOfRange(12, data.size)).toString(Charsets.UTF_8)
        } catch (error: Exception) {
            if (error is javax.crypto.AEADBadTagException || error is java.security.UnrecoverableKeyException) file.delete()
            throw TrackingException(TrackingFailure.STORAGE)
        }
    }

    override suspend fun write(token: String?) = withContext(Dispatchers.IO) {
        if (token == null) {
            try { KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) } }
            finally { file.delete() }
            if (file.baseFile.exists()) throw TrackingException(TrackingFailure.STORAGE)
            return@withContext
        }
        var output: java.io.FileOutputStream? = null
        try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, key())
            val encrypted = cipher.iv + cipher.doFinal(token.toByteArray(Charsets.UTF_8))
            output = file.startWrite()
            output.write(encrypted)
            file.finishWrite(output)
        } catch (_: Exception) { file.failWrite(output); throw TrackingException(TrackingFailure.STORAGE) }
    }
}
