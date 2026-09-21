package fr.sygix.sygixos.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.ui.home.HomeScreen
import fr.sygix.sygixos.ui.home.HomeViewModel

class MainActivity : ComponentActivity() {

    private companion object {
        /** Permission runtime du framework, absente de android.Manifest.permission dans le SDK public. */
        const val READ_TV_LISTINGS = "android.permission.READ_TV_LISTINGS"
    }

    private lateinit var viewModel: HomeViewModel

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshHero() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, HomeViewModel.Factory(applicationContext))[HomeViewModel::class.java]
        setContent {
            SygixOsTheme {
                HomeScreen(viewModel)
            }
        }
        requestTvListingsPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        viewModel.refresh()
    }

    /** Lecture des programmes publiés par les autres apps : permission runtime, demandée une fois. */
    private fun requestTvListingsPermissionIfNeeded() {
        val permission = READ_TV_LISTINGS
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }
}
