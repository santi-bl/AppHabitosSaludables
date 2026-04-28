/**
 * @author Santiago Barandiarán Lasheras
 * @description Pantalla de nutrición e hidratación. Permite el seguimiento del consumo
 * de agua mediante un vaso animado y el registro de alimentos (calorías y macronutrientes).
 */
package com.example.apphabitossaludables.ui.user

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val nutricion by viewModel.nutricion.collectAsState()
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val objetivoLitros = 2.5f
    val progresoAgua = (nutricion.hidratacionLitros.toFloat() / objetivoLitros).coerceIn(0f, 1f)
    
    var showFoodDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Nutrición e Hidratación", fontWeight = FontWeight.Bold)
                        val subTexto = when (fechaSeleccionada) {
                            LocalDate.now() -> "Hoy"
                            LocalDate.now().minusDays(1) -> "Ayer"
                            else -> fechaSeleccionada.format(DateTimeFormatter.ofPattern("d MMM, yyyy", Locale("es", "ES")))
                        }
                        Text(subTexto, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                },
                actions = {
                    if (fechaSeleccionada == LocalDate.now()) {
                        IconButton(onClick = { viewModel.eliminarUltimaAgua() }) {
                            Icon(
                                painter = painterResource(id = android.R.drawable.ic_menu_revert),
                                contentDescription = "Deshacer último agua",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            if (fechaSeleccionada == LocalDate.now()) {
                ExtendedFloatingActionButton(
                    onClick = { showFoodDialog = true },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    text = { Text("Añadir Comida") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
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
            Spacer(modifier = Modifier.height(24.dp))

            // Resumen de Calorías si hay datos
            if (nutricion.caloriasConsumidas > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Resumen Nutricional", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            NutrientItem("Calorías", "${nutricion.caloriasConsumidas.toInt()}", "kcal")
                            NutrientItem("Proteínas", "${nutricion.proteinasGramos.toInt()}", "g")
                            NutrientItem("Carbo", "${nutricion.carbohidratosGramos.toInt()}", "g")
                            NutrientItem("Grasas", "${nutricion.grasasGramos.toInt()}", "g")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Vaso de agua animado
            WaterGlass(progresoAgua)

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = String.format("%.1f L", nutricion.hidratacionLitros),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Objetivo de agua: $objetivoLitros L",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (fechaSeleccionada == LocalDate.now()) {
                Spacer(modifier = Modifier.height(32.dp))
                // Botones rápidos de agua
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    WaterButton("250ml", "+") { viewModel.agregarAgua(0.25) }
                    WaterButton("500ml", "++") { viewModel.agregarAgua(0.5) }
                }
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            Card(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.LocalDrink, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        "Beber agua mejora tu concentración y energía diaria.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            Spacer(modifier = Modifier.height(80.dp)) // Espacio para el FAB
        }

        if (showFoodDialog) {
            AddFoodDialog(
                onDismiss = { showFoodDialog = false },
                onConfirm = { cal, prot, carb, fat ->
                    viewModel.agregarComida(cal, prot, carb, fat)
                    showFoodDialog = false
                }
            )
        }
    }
}

@Composable
fun NutrientItem(label: String, value: String, unit: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
        Text(unit, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(label, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun AddFoodDialog(onDismiss: () -> Unit, onConfirm: (Double, Double, Double, Double) -> Unit) {
    var calorias by remember { mutableStateOf("") }
    var proteinas by remember { mutableStateOf("") }
    var carbohidratos by remember { mutableStateOf("") }
    var grasas by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar Alimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = calorias,
                    onValueChange = { calorias = it },
                    label = { Text("Calorías (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proteinas,
                    onValueChange = { proteinas = it },
                    label = { Text("Proteínas (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbohidratos,
                    onValueChange = { carbohidratos = it },
                    label = { Text("Carbohidratos (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grasas,
                    onValueChange = { grasas = it },
                    label = { Text("Grasas (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val cal = calorias.toDoubleOrNull() ?: 0.0
                val prot = proteinas.toDoubleOrNull() ?: 0.0
                val carb = carbohidratos.toDoubleOrNull() ?: 0.0
                val fat = grasas.toDoubleOrNull() ?: 0.0
                if (cal > 0) onConfirm(cal, prot, carb, fat)
            }) { Text("Añadir") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
fun WaterGlass(progreso: Float) {
    val infiniteTransition = rememberInfiniteTransition()
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2 * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    val glassBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
    val waterColorPrimary = MaterialTheme.colorScheme.primary
    val waterColorSecondary = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)

    Box(
        modifier = Modifier
            .size(width = 140.dp, height = 200.dp)
            .background(glassBorderColor, RoundedCornerShape(bottomStart = 30.dp, bottomEnd = 30.dp, topStart = 10.dp, topEnd = 10.dp))
            .padding(8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val waterHeight = height * progreso
            
            val path = Path().apply {
                moveTo(0f, height)
                lineTo(width, height)
                lineTo(width, (height - waterHeight).coerceAtLeast(0f))
                
                // Efecto de onda
                if (progreso > 0) {
                    for (x in width.toInt() downTo 0) {
                        val baseLine = height - waterHeight
                        val y = baseLine + (Math.sin((x / width * 2 * Math.PI) + waveOffset) * 8).toFloat()
                        lineTo(x.toFloat(), y)
                    }
                }
                close()
            }

            clipPath(Path().apply { 
                addRoundRect(androidx.compose.ui.geometry.RoundRect(0f, 0f, width, height, androidx.compose.ui.geometry.CornerRadius(40f, 40f))) 
            }) {
                drawPath(
                    path = path,
                    brush = Brush.verticalGradient(
                        colors = listOf(waterColorSecondary, waterColorPrimary)
                    )
                )
            }
        }
    }
}

@Composable
fun WaterButton(label: String, iconSuffix: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        LargeFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shape = CircleShape,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = null)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(label, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onBackground)
    }
}
