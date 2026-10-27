/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.tracking

import android.content.Context
import android.content.ContextWrapper
import android.security.keystore.KeyPermanentlyInvalidatedException
import androidx.test.core.app.ApplicationProvider
import java.security.UnrecoverableKeyException
import javax.crypto.SecretKey
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class KeystoreTrackingSessionTest {
    @get:Rule val folder = TemporaryFolder()

    @Test fun temporaryOrUnknownKeyFailuresKeepEncryptedSessionUntilRecovery() = runTest {
        for (failure in listOf(UnrecoverableKeyException("Fixture unavailable"),
            UnrecoverableKeyException("Fixture unavailable").apply { initCause(java.security.KeyStoreException("Fixture service failure")) },
            IllegalStateException("Fixture unavailable"))) {
            val keys = Keys()
            val context = context()
            val session = session(context, keys)
            session.write("fixture-token")
            val file = context.noBackupFilesDir.resolve("tracking-session")
            val encrypted = file.readBytes()
            keys.failure = failure
            try { session.read(); fail() }
            catch (error: TrackingException) { assertEquals(TrackingFailure.STORAGE, error.failure) }
            assertTrue(file.exists())
            assertArrayEquals(encrypted, file.readBytes())
            keys.failure = null
            assertEquals("fixture-token", session(context, keys).read())
            assertArrayEquals(encrypted, file.readBytes())
            assertEquals(0, keys.created)
            assertEquals(0, keys.deleted)
        }
    }

    @Test fun confirmedMissingKeyEndsRestorationAndDeletesOnlyUnreadableSession() = runTest {
        val keys = Keys()
        val context = context()
        val session = session(context, keys)
        session.write("fixture-token")
        keys.value = null
        assertNull(session.read())
        assertFalse(context.noBackupFilesDir.resolve("tracking-session").exists())
        assertNull(session.read())
        assertEquals(0, keys.created)
    }

    @Test fun confirmedPermanentKeyInvalidationAllowsSavingAndReadingANewSession() = runTest {
        val keys = Keys()
        val context = context()
        val session = session(context, keys)
        session.write("fixture-token")
        keys.failure = KeyPermanentlyInvalidatedException()
        assertNull(session.read())
        assertFalse(context.noBackupFilesDir.resolve("tracking-session").exists())
        assertNull(session.read())
        session.write("fixture-replacement")
        assertEquals("fixture-replacement", session(context, keys).read())
        assertEquals(1, keys.deleted)
        assertEquals(1, keys.created)
    }

    @Test fun failedInvalidKeyCleanupKeepsSessionRetryableUntilANewKeyCanBeCreated() = runTest {
        val keys = Keys()
        val context = context()
        val session = session(context, keys)
        session.write("fixture-token")
        val file = context.noBackupFilesDir.resolve("tracking-session")
        val encrypted = file.readBytes()
        keys.failure = KeyPermanentlyInvalidatedException()
        keys.deleteFailure = java.security.KeyStoreException("Fixture service unavailable")
        try { session.read(); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.STORAGE, error.failure) }
        assertArrayEquals(encrypted, file.readBytes())
        assertEquals(0, keys.deleted)
        assertEquals(0, keys.created)
        keys.deleteFailure = null
        assertNull(session.read())
        assertFalse(file.exists())
        session.write("fixture-replacement")
        assertEquals("fixture-replacement", session(context, keys).read())
        assertEquals(1, keys.deleted)
        assertEquals(1, keys.created)
    }

    @Test fun failedGcmAuthenticationEndsRestorationWithoutAnEndlessStorageError() = runTest {
        val keys = Keys()
        val context = context()
        val session = session(context, keys)
        session.write("fixture-token")
        val file = context.noBackupFilesDir.resolve("tracking-session")
        val bytes = file.readBytes()
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        file.writeBytes(bytes)
        assertNull(session.read())
        assertFalse(file.exists())
        assertNull(session.read())
    }

    @Test fun failedReplacementDoesNotOverwriteTheSavedSession() = runTest {
        val keys = Keys()
        val context = context()
        val session = session(context, keys)
        session.write("fixture-token")
        val file = context.noBackupFilesDir.resolve("tracking-session")
        val encrypted = file.readBytes()
        keys.failure = UnrecoverableKeyException("Fixture unavailable")
        try { session.write("fixture-replacement"); fail() }
        catch (error: TrackingException) { assertEquals(TrackingFailure.STORAGE, error.failure) }
        assertArrayEquals(encrypted, file.readBytes())
        keys.failure = null
        assertEquals("fixture-token", session.read())
    }

    @Test fun explicitDisconnectDeletesKeyAndSession() = runTest {
        val keys = Keys()
        val context = context()
        val session = session(context, keys)
        session.write("fixture-token")
        session.write(null)
        assertEquals(1, keys.deleted)
        assertFalse(context.noBackupFilesDir.resolve("tracking-session").exists())
        assertNull(session.read())
    }

    private fun context(): Context {
        val directory = folder.newFolder()
        return object : ContextWrapper(ApplicationProvider.getApplicationContext()) {
            override fun getNoBackupFilesDir() = directory
        }
    }

    private fun TestScope.session(context: Context, keys: Keys) = KeystoreTrackingSession(context, keys, StandardTestDispatcher(testScheduler))

    private class Keys : TrackingSessionKeys {
        var value: SecretKey? = SecretKeySpec(ByteArray(16) { 7 }, "AES")
        var failure: Exception? = null
        var deleteFailure: Exception? = null
        var created = 0
        var deleted = 0
        override fun read(): SecretKey? { failure?.let { throw it }; return value }
        override fun create(): SecretKey { ++created; return SecretKeySpec(ByteArray(16) { 9 }, "AES").also { value = it } }
        override fun delete() { deleteFailure?.let { throw it }; ++deleted; value = null; failure = null }
    }
}
