/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.perf

import android.app.Application
import androidx.core.app.CoreComponentFactory

class ProfileComponentFactory : CoreComponentFactory() {
    override fun instantiateApplication(cl: ClassLoader, className: String): Application =
        if (Application.getProcessName().endsWith(":gridprofile")) Application() else super.instantiateApplication(cl, className)
}
