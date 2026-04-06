package com.example.apphabitossaludables.ui.screen.user

import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apphabitossaludables.data.repository.HealthRepository
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModelFactory


@Composable
fun UserScreen(
    onLogout: () -> Unit,
    onFoodScreen: () -> Unit,
    onExerciseScreen: () -> Unit,
    onDreamScreen: () -> Unit
) {
    val context = LocalContext.current

    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
    val repository = remember { HealthRepository(healthConnectClient) }
    val viewModel: AppHabitusViewModel = viewModel(
        factory = AppHabitusViewModelFactory(repository)
    )

    // Observar los datos como estado de Compose
    val pasos by viewModel.pasos.collectAsState()
    val pulsaciones by viewModel.pulsaciones.collectAsState()
    val horasDeSueño by viewModel.sueño.collectAsState()
    val cargando by viewModel.cargando.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.cargarDatosDelDia()
    }

    Column(Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top)
    {

            Text(text = "Pasos de hoy: $pasos")
            Text(text = "Pulsaciones medias: $pulsaciones ppm")
            Text(text = "Horas de sueño: $horasDeSueño")
        Text(text = "Cargando datos...:$cargando")
    }
}