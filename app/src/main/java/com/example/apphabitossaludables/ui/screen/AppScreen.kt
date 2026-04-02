package com.example.apphabitossaludables.ui.screen

import android.content.Intent
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.apphabitossaludables.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter

@Composable
fun AppScreen(){
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLogin = { username ->
                    nav.navigate("user") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { nav.navigate("register") })
        }
        composable("register"){
            RegiesterScreen(
                onRegister_Login = { username ->
                    nav.navigate("user/$username"){
                        popUpTo("register"){inclusive = true}
                    }
                }
            )
        }
        composable("user"){
            UserScreen(
                onLogout = {->
                    nav.navigate("login") {
                        popUpTo("user") { inclusive = true }
                    }
                },
                onFoodScreen = { ->
                    nav.navigate("food"){
                        popUpTo("user"){ inclusive = true}
                    }
                },onExerciseScreen = { ->
                    nav.navigate("exercise"){
                        popUpTo("user"){inclusive = true}
                    }
                },onDreamScreen ={ ->
                    nav.navigate("dream"){
                        popUpTo("user"){inclusive = true}
                    }
                }
            )
        }
    }
}

@Composable
fun LoginScreen(onLogin:(String) ->Unit,
                onNavigateToRegister:() -> Unit) {
    var email by remember { mutableStateOf("")}
    var password by remember { mutableStateOf("")}
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center)
    {
        Image(
            painter = painterResource(id= R.drawable.app),
            contentDescription = "Imagen de android",
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
        )
        Text(text = "INICIAR SESION",style = MaterialTheme.typography.headlineMedium,textDecoration = TextDecoration.Underline)
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("EMAIL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("CONTRASEÑA") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            modifier = Modifier.fillMaxWidth(), onClick =
                {    onLogin("user")    })
        { Text("Acceder") }
        Spacer(modifier = Modifier.height(20.dp))

        //LINEA SEPARADORA

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick =  onNavigateToRegister) {
            Text(
                text = "¿No tienes una cuenta? Regístrate",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
    }
}
@Composable
fun RegiesterScreen(onRegister_Login:(String) -> Unit){

    var alias by remember{mutableStateOf("")}
    var name by remember{mutableStateOf("")}
    var lastname by remember{mutableStateOf("")}
    var email by remember{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    var borndate by remember{mutableStateOf(null)}

    Column(Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top)
    {
        Image(
            painter = painterResource(id= R.drawable.isotipo),
            contentDescription = "Imagen de android",
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
        )
        Text(text = "Empieza a usar Habitus",style = MaterialTheme.typography.headlineMedium, fontSize = 24.sp)
        Text(text = "Regístrate para llevar mejores habitos y mejorar día a día junto a nosotros",style = MaterialTheme.typography.headlineMedium, fontSize = 16.sp)
        Text(text = "Nombre",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("NOMBRE") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Apellidos",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = lastname,
            onValueChange = { lastname = it },
            label = { Text("APELLIDOS") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Email",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("EMAIL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Contraseña",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("CONTRASEÑA") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

    }
}
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

suspend fun leerPasosDelDia(healthConnectClient: HealthConnectClient): Long {
    // Definimos el inicio del día (00:00 de hoy) en la zona horaria local
    val startOfDay = ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
    val endOfDay = Instant.now()

    return try {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest<StepsRecord>(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
        )

        // Sumamos todos los registros y devolvemos el total
        val totalPasos = response.records.sumOf { it.count }
        println("Total de pasos recogidos: $totalPasos")

        totalPasos
    } catch (e: Exception) {
        // Manejar error (por ejemplo, si el usuario no dio permisos)
        println("Error leyendo pasos: ${e.message}")
        0L // Devolvemos 0 si falla
    }
}

suspend fun leerPulsacionesDelDia(healthConnectClient: HealthConnectClient): Long {
    // Definimos el inicio del día (00:00 de hoy)
    val startOfDay = ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
    val endOfDay = Instant.now()

    return try {
        val response = healthConnectClient.readRecords(
            ReadRecordsRequest<HeartRateRecord>(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
            )
        )

        var sumaPulsaciones = 0L
        var totalMuestras = 0

        // Recorremos todos los registros y sus muestras internas
        for (registro in response.records) {
            for (muestra in registro.samples) {
                sumaPulsaciones += muestra.beatsPerMinute
                totalMuestras++
            }
        }

        // Calculamos y devolvemos la media
        if (totalMuestras > 0) {
            val mediaPulsaciones = sumaPulsaciones / totalMuestras
            println("Pulsaciones medias recogidas: $mediaPulsaciones (de $totalMuestras muestras)")
            mediaPulsaciones
        } else {
            println("No se encontraron datos de pulsaciones hoy")
            0L
        }

    } catch (e: Exception) {
        println("Error leyendo pulsaciones: ${e.message}")
        0L // Devolvemos 0 si falla
    }
}