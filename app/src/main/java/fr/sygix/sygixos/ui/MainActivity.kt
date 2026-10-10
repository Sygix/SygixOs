/*
 * Copyright (C) 2026 Sygix
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package fr.sygix.sygixos.ui

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.data.LauncherSystemIntents
import fr.sygix.sygixos.ui.home.HomeScreen
import fr.sygix.sygixos.ui.home.HomeViewModel
import fr.sygix.sygixos.ui.home.LauncherSystemViewModel

class MainActivity : ComponentActivity() {

    private companion object {
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
        const val EXTRA_NO_GLASS = "noglass"
    }

    private lateinit var viewModel: HomeViewModel
    private lateinit var launcher: LauncherSystemViewModel
    private var shownSinceStop = false

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            viewModel.refreshHero()
            viewModel.refreshUpNext()
            launcher.onPermissionFinished()
        }

    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        launcher.onHomeRoleResult(it.resultCode == Activity.RESULT_OK)
    }

    private val launchRoleRequest: (Intent) -> Unit = { roleRequest.launch(it) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        splashScreen.setOnExitAnimationListener { it.remove() }
        viewModel = ViewModelProvider(this, HomeViewModel.Factory(applicationContext))[HomeViewModel::class.java]
        launcher = ViewModelProvider(this, LauncherSystemViewModel.Factory(applicationContext))[LauncherSystemViewModel::class.java]
        launcher.bindRoleRequest(launchRoleRequest)
        if (LauncherSystemIntents.isBoot(intent)) launcher.onBootObserved()
        val glassBlur = !intent.getBooleanExtra(EXTRA_NO_GLASS, false)
        setContent {
            SygixOsTheme {
                HomeScreen(viewModel, glassBlur = glassBlur, launcher = launcher)
            }
        }
        window.setBackgroundDrawable(null)
        requestTvListingsPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        shownSinceStop = true
        launcher.onResume()
        viewModel.refresh()
        viewModel.onForeground()
    }

    override fun onStop() {
        shownSinceStop = false
        super.onStop()
    }

    override fun onDestroy() {
        launcher.unbindRoleRequest(launchRoleRequest)
        super.onDestroy()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (LauncherSystemIntents.isBoot(intent)) launcher.onBootObserved()
        val homeKey = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        if ((homeKey && shownSinceStop) || LauncherSystemIntents.isForegroundHome(intent)) launcher.onHome()
    }

    private fun requestTvListingsPermissionIfNeeded() {
        val permission = READ_TV_LISTINGS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        } else {
            launcher.onPermissionFinished()
        }
    }
}
