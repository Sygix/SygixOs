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
        /** Diagnostic : `am start --ez noglass true` désactive le flou d'arrière-plan. */
        const val EXTRA_NO_GLASS = "noglass"
    }

    private lateinit var viewModel: HomeViewModel

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { viewModel.refreshHero() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this, HomeViewModel.Factory(applicationContext))[HomeViewModel::class.java]
        val glassBlur = !intent.getBooleanExtra(EXTRA_NO_GLASS, false)
        setContent {
            SygixOsTheme {
                HomeScreen(viewModel, glassBlur = glassBlur)
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
