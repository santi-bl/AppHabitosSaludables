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
import com.example.apphabitossaludables.ui.screen.navigation.leerPasosDelDia
import com.example.apphabitossaludables.ui.screen.navigation.leerPulsacionesDelDia


@Composable
fun UserScreen(
    onLogout: () -> Unit,
    onFoodScreen: () -> Unit,
    onExerciseScreen: () -> Unit,
    onDreamScreen: () -> Unit
) {
    val context = LocalContext.current

    val healthConnectClient = remember {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }

    val PERMISSIONS = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class)
    )

    var pasosDeHoy by remember { mutableLongStateOf(0L) }
    var pulsacionesDeHoy by remember { mutableLongStateOf(0L) }

    // 2. Estado para forzar la lectura cuando ya tenemos permisos
    var permisosConcedidos by remember { mutableStateOf(false) }

    // 1. Creamos el lanzador que mostrará la pantalla de permisos
    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->
        // MODIFICACIÓN: Comprobamos si nos han dado AL MENOS uno de los permisos importantes
        if (grantedPermissions.isNotEmpty()) {
            println("Permisos concedidos: $grantedPermissions")
            permisosConcedidos = true
        } else {
            println("El usuario no marcó ninguna casilla o cerró la ventana")
            Toast.makeText(context, "No se concedieron permisos. Por favor, marca las casillas.", Toast.LENGTH_LONG).show()
        }
    }

    // 3. Comprobamos los permisos al entrar a la pantalla (SOLO COMPROBAR, NO PEDIR)
    LaunchedEffect(healthConnectClient) {
        if (healthConnectClient != null) {
            val granted = healthConnectClient.permissionController.getGrantedPermissions()
            if (granted.containsAll(PERMISSIONS)) {
                permisosConcedidos = true
            }
            // ¡HEMOS QUITADO EL LANZAMIENTO AUTOMÁTICO DE AQUÍ!
        } else {
            Toast.makeText(context, "Health Connect no está instalado", Toast.LENGTH_SHORT).show()
        }
    }

    // 4. Leemos los datos SOLO cuando los permisos están concedidos
    LaunchedEffect(permisosConcedidos) {
        if (permisosConcedidos && healthConnectClient != null) {
            pasosDeHoy = leerPasosDelDia(healthConnectClient)
            pulsacionesDeHoy = leerPulsacionesDelDia(healthConnectClient)
        }
    }

    Column(Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top)
    {
        if (permisosConcedidos) {
            Text(text = "Pasos de hoy: $pasosDeHoy")
            Text(text = "Pulsaciones medias: $pulsacionesDeHoy ppm")
        } else {
            Text(text = "Faltan permisos para leer los datos de salud.")
            Spacer(modifier = Modifier.height(16.dp))

            // NUEVO BOTÓN: Pedir permisos manualmente (Aceptado por Android 14)
            Button(onClick = {
                permissionsLauncher.launch(PERMISSIONS)
            }) {
                Text(text = "Pedir Permisos")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(onClick = {
                // Esto abre directamente la pantalla de Health Connect de tu Samsung
                val intent = Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS)
                context.startActivity(intent)
            }) {
                Text(text = "Abrir Ajustes de Health Connect")
            }
        }
    }
}