package com.example.apphabitossaludables.ui.user

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val pesoActual by viewModel.pesoActual.collectAsState()
    val historial by viewModel.historialPeso.collectAsState()
    var showDialog by remember { mutableStateOf(false) }
    var weightInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Control de Peso", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { 
                    weightInput = if (pesoActual > 0) pesoActual.toString() else ""
                    showDialog = true 
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Registrar Peso")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF8F9FA))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Card de Peso Actual
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Default.MonitorWeight,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Peso Actual", color = Color.Gray)
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = if (pesoActual > 0) String.format("%.1f", pesoActual) else "--",
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Black
                        )
                        Text(" kg", fontSize = 20.sp, color = Color.Gray, modifier = Modifier.padding(bottom = 8.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Gráfica de progreso
            Text(
                "Progreso (Últimos registros)",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .padding(vertical = 12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                if (historial.size >= 2) {
                    WeightChart(historial)
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Registra al menos 2 pesos para ver la gráfica", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de Historial
            Text(
                "Historial Reciente",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )
            
            Spacer(modifier = Modifier.height(8.dp))

            historial.reversed().take(10).forEach { (fecha, peso) ->
                val formatter = DateTimeFormatter.ofPattern("dd MMM, yyyy")
                    .withZone(ZoneId.systemDefault())
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(formatter.format(fecha), fontWeight = FontWeight.Medium)
                        Text("${String.format("%.1f", peso)} kg", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Registrar Peso") },
                text = {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("Peso en kg") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        val peso = weightInput.toDoubleOrNull()
                        if (peso != null) {
                            viewModel.guardarPeso(peso)
                            showDialog = false
                        }
                    }) { Text("Guardar") }
                },
                dismissButton = {
                    TextButton(onClick = { showDialog = false }) { Text("Cancelar") }
                }
            )
        }
    }
}

@Composable
fun WeightChart(datos: List<Pair<java.time.Instant, Double>>) {
    val pesos = datos.map { it.second.toFloat() }
    val maxPeso = pesos.maxOrNull() ?: 100f
    val minPeso = pesos.minOrNull() ?: 0f
    val range = (maxPeso - minPeso).coerceAtLeast(1f)
    
    val primaryColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 32.dp, bottom = 16.dp, start = 16.dp, end = 16.dp)
    ) {
        val width = size.width
        val height = size.height
        val spacing = width / (pesos.size - 1).coerceAtLeast(1)

        val points = pesos.mapIndexed { index, peso ->
            val x = index * spacing
            val normalizedY = (peso - minPeso) / range
            val y = height - (normalizedY * height)
            Offset(x, y)
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                points.forEach { lineTo(it.x, it.y) }
            }
        }

        drawPath(
            path = path,
            color = primaryColor,
            style = Stroke(width = 3.dp.toPx())
        )

        points.forEach { point ->
            drawCircle(
                color = primaryColor,
                radius = 5.dp.toPx(),
                center = point
            )
            drawCircle(
                color = Color.White,
                radius = 3.dp.toPx(),
                center = point
            )
        }
    }
}
