/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import java.io.File
import java.nio.file.Files
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.test.TestScope

fun testDataStore(scope: CoroutineScope): DataStore<Preferences> {
    val directory = Files.createTempDirectory("datastore").toFile().apply { deleteOnExit() }
    return PreferenceDataStoreFactory.create(scope = scope) { File(directory, "launcher.preferences_pb") }
}

fun TestScope.testDataStore(): DataStore<Preferences> = testDataStore(backgroundScope)

fun inlineDataStore(): DataStore<Preferences> = testDataStore(CoroutineScope(SupervisorJob() + Dispatchers.Unconfined))
