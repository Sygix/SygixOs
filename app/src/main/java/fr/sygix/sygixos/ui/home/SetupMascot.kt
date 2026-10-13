/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui.home

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import fr.sygix.sygixos.R
import fr.sygix.sygixos.core.designsystem.Dimens
import fr.sygix.sygixos.domain.ImageBounds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun SetupMascot() {
    val resources = LocalContext.current.resources
    val target = with(LocalDensity.current) { Dimens.OnboardingMascot.roundToPx() }
    val bitmap by produceState<Bitmap?>(null, resources, target) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                resources.openRawResource(R.raw.splash_mascot).use { BitmapFactory.decodeStream(it, null, bounds) }
                val sample = ImageBounds.sampleSize(bounds.outWidth, target)
                resources.openRawResource(R.raw.splash_mascot).use {
                    BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
                }
            }.getOrNull()
        }
    }
    Box(Modifier.size(Dimens.OnboardingMascot).testTag("setup-mascot")) {
        bitmap?.let { Image(it.asImageBitmap(), contentDescription = null, modifier = Modifier.fillMaxSize()) }
    }
}
