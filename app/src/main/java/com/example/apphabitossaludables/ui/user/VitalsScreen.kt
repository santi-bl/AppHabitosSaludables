/**
 * @author Santiago Barandiarán Lasheras
 * @description Pantalla de monitorización de signos vitales. Muestra gráficamente
 * las pulsaciones por hora y estadísticas de ritmo cardíaco.
 * Totalmente adaptada para modo oscuro y claro.
 */
package com.example.apphabitossaludables.ui.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val vitals by viewModel.signosVitales.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ritmo Cardíaco", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Valor Central Grande
            Column(
                modifier = Modifier.padding(vertical = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${vitals.frecuenciaCardiacaMedia}",
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = " PPM",
                        modifier = Modifier.padding(bottom = 12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text("Promedio diario", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
            }

            // Área del Gráfico
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(horizontal = 16.dp)
            ) {
                // Líneas de referencia horizontales
                Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceBetween) {
                    repeat(3) { index ->
                        val valor = when(index) { 0 -> "150"; 1 -> "75"; else -> "0" }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                            Text("  $valor", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Barras con gradiente
                Row(
                    modifier = Modifier.fillMaxSize().padding(top = 8.dp, bottom = 8.dp, end = 35.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    for (hora in 0..23) {
                        val media = vitals.pulsacionesPorHora[hora] ?: 0L
                        val alturaProporcional = (media.toFloat() / 180f).coerceIn(0.05f, 1f)
                        
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(alturaProporcional)
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = listOf(Color(0xFFFF5252), Color(0xFFFF5252).copy(alpha = 0.3f))
                                    ),
                                    shape = RoundedCornerShape(2.dp)
                                )
                        )
                    }
                }
            }
            
            // Eje X (Horas)
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("00:00", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("23:59", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tarjeta de Estadísticas
            Card(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Estadísticas diarias", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
                    
                    StatRow("Ritmo cardíaco promedio", "${vitals.frecuenciaCardiacaMedia}")
                    StatRow("Ritmo cardíaco máximo", "${vitals.frecuenciaCardiacaMaxima}")
                    StatRow("Ritmo cardíaco mínimo", "${vitals.frecuenciaCardiacaMinima}")
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = MaterialTheme.colorScheme.onSurface)
            Text(" ppm", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
