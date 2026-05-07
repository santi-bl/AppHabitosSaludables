/**
 * @author Santiago Barandiarán Lasheras
 * @description Pantalla principal de usuario (Dashboard). Muestra un resumen visual
 * del estado de salud diario, incluyendo puntuación de vitalidad, historial semanal
 * interactivo y accesos directos a las diferentes métricas de salud.
 */
package com.example.apphabitossaludables.ui.user

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import java.time.LocalDate
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserScreen(
    viewModel: AppHabitusViewModel,
    onLogout: () -> Unit,
    onFoodScreen: () -> Unit,
    onExerciseScreen: () -> Unit,
    onDreamScreen: () -> Unit,
    onVitalsScreen: () -> Unit,
    onWeightScreen: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
    val df = remember { DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)) }

    val permisosRequeridos = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getWritePermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getWritePermission(HydrationRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getWritePermission(NutritionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class)
    )

    val requestPermissionLauncher = rememberLauncherForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { permisosConcedidos ->
        if (permisosConcedidos.containsAll(permisosRequeridos)) {
            viewModel.cargarDatosDelDia()
        } else {
            Toast.makeText(context, "Faltan algunos permisos de salud", Toast.LENGTH_SHORT).show()
        }
    }

    // Efecto para actualizar datos siempre que la pantalla vuelva a estar activa (OnResume)
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                // Al volver de otra pantalla o segundo plano, refrescamos datos
                viewModel.cargarDatosDelDia()
                viewModel.fetchUserProfile()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val pesoActual by viewModel.pesoActual.collectAsState()
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()

    LaunchedEffect(Unit) {
        val otorgados = healthConnectClient.permissionController.getGrantedPermissions()
        if (otorgados.containsAll(permisosRequeridos)) {
            viewModel.cargarDatosDelDia()
        } else {
            requestPermissionLauncher.launch(permisosRequeridos)
        }
    }

    val userProfile by viewModel.userProfile.collectAsState()
    val actividad by viewModel.actividad.collectAsState()
    val nutricion by viewModel.nutricion.collectAsState()
    val signosVitales by viewModel.signosVitales.collectAsState()
    val sueño by viewModel.sueño.collectAsState()
    val historial by viewModel.historialVitalidad.collectAsState()

    val score = remember(actividad, nutricion, sueño, userProfile) {
        val metaPasos = userProfile?.objetivoPasos?.toFloat() ?: 10000f
        val actScore = (actividad.pasos / metaPasos).coerceIn(0f, 1f) * 40
        val sleepScore = ((sueño?.duracionTotalMinutos ?: 0) / 450f).coerceIn(0f, 1f) * 40
        val nutScore = (nutricion.hidratacionLitros / 2.0).coerceIn(0.0, 1.0) * 20
        (actScore + sleepScore + nutScore).toInt()
    }

    val estadoMensaje = when {
        score > 80 -> "¡Día Excelente! Estás en equilibrio."
        score > 50 -> "Buen ritmo. Sigue así."
        else -> "Día de recuperación. ¡Ánimo!"
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val userName by viewModel.userFullName.collectAsState()

        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Hola $userName",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { viewModel.cambiarFecha(-1) }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Día anterior", tint = MaterialTheme.colorScheme.primary)
            }
            
            var showDatePicker by remember { mutableStateOf(false) }
            
            if (showDatePicker) {
                val datePickerState = rememberDatePickerState(
                    initialSelectedDateMillis = fechaSeleccionada.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                    selectableDates = object : SelectableDates {
                        override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                            // No permitir fechas futuras
                            return utcTimeMillis <= System.currentTimeMillis()
                        }
                    }
                )

                DatePickerDialog(
                    onDismissRequest = { showDatePicker = false },
                    confirmButton = {
                        TextButton(onClick = {
                            datePickerState.selectedDateMillis?.let {
                                val selectedDate = Instant.ofEpochMilli(it)
                                    .atZone(ZoneId.systemDefault())
                                    .toLocalDate()
                                viewModel.seleccionarFecha(selectedDate)
                            }
                            showDatePicker = false
                        }) { Text("Aceptar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
                    }
                ) {
                    DatePicker(state = datePickerState)
                }
            }

            val fechaTexto = when (fechaSeleccionada) {
                LocalDate.now() -> "Hoy"
                LocalDate.now().minusDays(1) -> "Ayer"
                else -> fechaSeleccionada.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es", "ES")))
            }
            
            Text(
                text = fechaTexto,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { showDatePicker = true },
                color = MaterialTheme.colorScheme.onBackground
            )

            IconButton(
                onClick = { viewModel.cambiarFecha(1) },
                enabled = fechaSeleccionada.isBefore(LocalDate.now())
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.KeyboardArrowRight, 
                    contentDescription = "Día siguiente",
                    tint = if (fechaSeleccionada.isBefore(LocalDate.now())) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 12.dp,
            )
            CircularProgressIndicator(
                progress = { score / 100f },
                modifier = Modifier.fillMaxSize(),
                color = if (score > 70) Color(0xFF4CAF50) else if (score > 40) Color(0xFFFFC107) else Color(0xFFFF5252),
                strokeWidth = 12.dp,
                strokeCap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "$score", 
                    style = MaterialTheme.typography.displayLarge, 
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Puntos", 
                    style = MaterialTheme.typography.titleMedium, 
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = estadoMensaje,
            modifier = Modifier.padding(top = 16.dp, bottom = 24.dp),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (historial.isNotEmpty()) {
            Text(
                "Historial de Vitalidad (7 días)",
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    historial.forEach { item ->
                        val isSelected = item.fecha == fechaSeleccionada
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .clickable { viewModel.seleccionarFecha(item.fecha) }
                                .padding(4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(16.dp)
                                    .height((item.puntuacion.toFloat().coerceAtLeast(5f)).dp)
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                                if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                    )
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = item.fecha.dayOfWeek.getDisplayName(java.time.format.TextStyle.NARROW, Locale("es", "ES")),
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Actividad",
                    subtitle = "${actividad.pasos} pasos",
                    icon = Icons.AutoMirrored.Filled.DirectionsRun,
                    color = Color(0xFF42A5F5),
                    modifier = Modifier.weight(1f),
                    onClick = onExerciseScreen
                )
                DashboardCard(
                    title = "Sueño",
                    subtitle = sueño?.let { "${it.duracionTotalMinutos / 60}h ${it.duracionTotalMinutos % 60}m" } ?: "Sin datos",
                    icon = Icons.Default.Bedtime,
                    color = Color(0xFF7E57C2),
                    modifier = Modifier.weight(1f),
                    onClick = onDreamScreen
                )
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                DashboardCard(
                    title = "Nutrición",
                    subtitle = "${df.format(nutricion.hidratacionLitros)}L agua",
                    icon = Icons.Default.LocalDrink,
                    color = Color(0xFF26C6DA),
                    modifier = Modifier.weight(1f),
                    onClick = onFoodScreen
                )
                DashboardCard(
                    title = "Vitals",
                    subtitle = "${signosVitales.frecuenciaCardiacaMedia} ppm",
                    icon = Icons.Default.Favorite,
                    color = Color(0xFFEF5350),
                    modifier = Modifier.weight(1f),
                    onClick = onVitalsScreen
                )
            }
            DashboardCard(
                title = "Peso corporal",
                subtitle = if (pesoActual > 0) "${df.format(pesoActual)} kg" else "Registrar peso",
                icon = Icons.Default.MonitorWeight,
                color = Color(0xFF4DB6AC),
                modifier = Modifier.fillMaxWidth(),
                onClick = onWeightScreen,
                isCentered = true
            )
        }

        Spacer(modifier = Modifier.height(30.dp))
    }
}

@Composable
fun DashboardCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier,
    isCentered: Boolean = false,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier
            .height(110.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start
        ) {
            Icon(icon, contentDescription = null, tint = color)
            Column(horizontalAlignment = if (isCentered) Alignment.CenterHorizontally else Alignment.Start) {
                Text(text = title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = subtitle, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
