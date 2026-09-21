package fr.sygix.sygixos.ui

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import fr.sygix.sygixos.core.designsystem.SygixOsTheme
import fr.sygix.sygixos.ui.home.HomeScreen

class MainActivity : ComponentActivity() {

    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestTvProviderPermissionIfNeeded()
        setContent {
            SygixOsTheme {
                HomeScreen()
            }
        }
    }

    /**
     * Lecture du TV Provider (programmes publiés par les apps installées).
     * Si la permission est de type runtime, la modale système s'affiche au
     * premier lancement ; sinon l'appel est sans effet.
     */
    private fun requestTvProviderPermissionIfNeeded() {
        val permission = "com.android.providers.tv.permission.READ_EPG_DATA"
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionLauncher.launch(permission)
        }
    }
}
