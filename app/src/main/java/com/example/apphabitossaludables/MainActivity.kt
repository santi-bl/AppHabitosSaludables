package com.example.apphabitossaludables

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apphabitossaludables.data.repository.HealthRepository
import com.example.apphabitossaludables.data.repository.UserPreferencesRepository
import com.example.apphabitossaludables.service.StepCounterService
import com.example.apphabitossaludables.ui.navigation.AppNavigation
import com.example.apphabitossaludables.ui.theme.AppHabitosTheme
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModelFactory

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACTIVITY_RECOGNITION] == true) {
            startStepCounterService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Solicitar permisos necesarios para el contador de pasos
        val permissionsToRequest = mutableListOf(Manifest.permission.ACTIVITY_RECOGNITION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())

        setContent {
            val healthConnectClient = HealthConnectClient.getOrCreate(this)
            val healthRepository = HealthRepository(healthConnectClient)
            val preferencesRepository = UserPreferencesRepository(this)
            
            val viewModel: AppHabitusViewModel = viewModel(
                factory = AppHabitusViewModelFactory(healthRepository, preferencesRepository)
            )
            
            val isDarkMode by viewModel.isDarkMode.collectAsState()

            AppHabitosTheme(darkTheme = isDarkMode) {
                AppNavigation()
            }
        }
    }

    private fun startStepCounterService() {
        val intent = Intent(this, StepCounterService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }
}
