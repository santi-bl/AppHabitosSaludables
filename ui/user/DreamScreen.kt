package com.example.apphabitossaludables.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DreamScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val sueño by viewModel.sueño.collectAsState()
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Sueño y Descanso", fontWeight = FontWeight.Bold)
                        val subTexto = when (fechaSeleccionada) {
                            LocalDate.now() -> "Hoy"
                            LocalDate.now().minusDays(1) -> "Ayer"
                            else -> fechaSeleccionada.format(DateTimeFormatter.ofPattern("d MMM, yyyy", Locale("es", "ES")))
                        }
                        Text(subTexto, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            sueño?.let { datos ->
                // Formateo de tiempo total
                val totalHoras = datos.duracionTotalMinutos / 60
                val totalMinutos = datos.duracionTotalMinutos % 60

                // Card principal de resumen
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Bedtime, contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text("Tiempo Total", style = MaterialTheme.typography.labelLarge)
                            Text(
                                text = buildString {
                                    if (totalHoras > 0) append("${totalHoras}h ")
                                    append("${totalMinutos}min")
                                },
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }

                Text(
                    text = "Detalles de las Etapas",
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Listado de etapas formateadas
                datos.etapas.forEach { (nombre, mins) ->
                    val h = mins / 60
                    val m = mins % 60
                    
                    val colorEtapa = when (nombre) {
                        "Sueño profundo" -> Color(0xFF1A237E)
                        "Sueño ligero" -> Color(0xFF3F51B5)
                        "REM" -> Color(0xFF9C27B0)
                        "Despierto" -> Color(0xFFFF9800)
                        else -> MaterialTheme.colorScheme.outline
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp).fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(modifier = Modifier.size(12.dp).background(colorEtapa, CircleShape))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(nombre, fontWeight = FontWeight.Medium)
                            }
                            Text(
                                text = buildString {
                                    if (h > 0) append("${h}h ")
                                    append("${m}min")
                                },
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Info adicional
                Card(
                    modifier = Modifier.padding(top = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            if (fechaSeleccionada == LocalDate.now()) 
                                "Los datos muestran la sesión de sueño más reciente detectada en las últimas 24 horas."
                            else 
                                "Mostrando registros de sueño guardados para esta fecha.",
                            fontSize = 12.sp
                        )
                    }
                }

            } ?: Column(
                modifier = Modifier.fillMaxSize().padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.Bedtime, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                Text(
                    "No hay datos de sueño para esta fecha",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        }
    }
}
