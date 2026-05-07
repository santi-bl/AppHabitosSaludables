/**
 * @author Santiago Barandiarán Lasheras
 * @description Pantalla de nutrición e hidratación. Permite el seguimiento del consumo
 * de agua mediante un vaso animado y el registro detallado de alimentos. Incluye
 * un listado de comidas del día con opción de edición y borrado.
 */
package com.example.apphabitossaludables.ui.user

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import com.example.apphabitossaludables.data.model.Comida
import com.example.apphabitossaludables.data.model.RegistroNutricional
import com.example.apphabitossaludables.data.model.TipoRegistro
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val nutricion by viewModel.nutricion.collectAsState()
    val fechaSeleccionada by viewModel.fechaSeleccionada.collectAsState()
    val objetivoLitros = 2.5f
    val progresoAgua = (nutricion.hidratacionLitros.toFloat() / objetivoLitros).coerceIn(0f, 1f)
    
    var showFoodDialog by remember { mutableStateOf(false) }
    var editingComida by remember { mutableStateOf<Comida?>(null) }
    
    // Formateador que usa punto como decimal y muestra hasta 2 decimales solo si existen
    val df = remember { 
        DecimalFormat("0.##", DecimalFormatSymbols(Locale.US)) 
    }

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
                    onClick = { 
                        editingComida = null
                        showFoodDialog = true 
                    },
                    icon = { Icon(Icons.Default.Restaurant, contentDescription = null) },
                    text = { Text("Añadir Comida") },
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Resumen de Macronutrientes con precisión decimal
                Card(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Resumen Diario", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            NutrientItem("Calorías", "${nutricion.caloriasConsumidas.toInt()}", "kcal")
                            NutrientItem("Proteínas", df.format(nutricion.proteinasGramos), "g")
                            NutrientItem("Carbo", df.format(nutricion.carbohidratosGramos), "g")
                            NutrientItem("Grasas", df.format(nutricion.grasasGramos), "g")
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }

            item {
                // Vaso de agua animado
                WaterGlass(progresoAgua)

                Spacer(modifier = Modifier.height(24.dp))

                // Cantidad exacta de agua
                Text(
                    text = "${df.format(nutricion.hidratacionLitros)} L",
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
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        WaterButton("250ml", "+") { viewModel.agregarAgua(0.25) }
                        WaterButton("500ml", "++") { viewModel.agregarAgua(0.5) }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
                
                Text(
                    "Comidas del día",
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            if (nutricion.registros.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        Text("No hay registros hoy", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(nutricion.registros) { registro ->
                    UnifiedNutritionRow(
                        registro = registro,
                        onEdit = {
                            if (registro.tipo == TipoRegistro.COMIDA) {
                                // Buscamos el objeto Comida original para editar
                                val comidaOriginal = nutricion.comidas.find { it.id == registro.id }
                                editingComida = comidaOriginal
                                showFoodDialog = true
                            }
                        },
                        onDelete = {
                            if (registro.tipo == TipoRegistro.COMIDA) {
                                viewModel.eliminarComida(registro.id)
                            } else {
                                viewModel.eliminarAgua(registro.id)
                            }
                        }
                    )
                }
            }
        }

        if (showFoodDialog) {
            AddEditFoodDialog(
                comida = editingComida,
                onDismiss = { showFoodDialog = false },
                onConfirm = { cal, prot, carb, fat, nombre ->
                    if (editingComida != null) {
                        viewModel.eliminarComida(editingComida!!.id)
                    }
                    viewModel.agregarComida(cal, prot, carb, fat, nombre)
                    showFoodDialog = false
                }
            )
        }
    }
}

@Composable
fun UnifiedNutritionRow(registro: RegistroNutricional, onEdit: () -> Unit, onDelete: () -> Unit) {
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault())
    val esAgua = registro.tipo == TipoRegistro.AGUA
    
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icono dinámico según tipo
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        if (esAgua) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (esAgua) Icons.Default.LocalDrink else Icons.Default.Restaurant,
                    contentDescription = null,
                    tint = if (esAgua) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(registro.nombre, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                Text(
                    "${registro.valor} • ${timeFormatter.format(registro.momento)}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Row {
                if (!esAgua) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar", tint = MaterialTheme.colorScheme.primary)
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = MaterialTheme.colorScheme.error)
                }
            }
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
fun AddEditFoodDialog(comida: Comida?, onDismiss: () -> Unit, onConfirm: (Double, Double, Double, Double, String) -> Unit) {
    var nombre by remember { mutableStateOf(comida?.nombre ?: "") }
    var calorias by remember { mutableStateOf(comida?.calorias?.toString() ?: "") }
    var proteinas by remember { mutableStateOf(comida?.proteinas?.toString() ?: "") }
    var carbohidratos by remember { mutableStateOf(comida?.carbohidratos?.toString() ?: "") }
    var grasas by remember { mutableStateOf(comida?.grasas?.toString() ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (comida == null) "Registrar Alimento" else "Editar Alimento") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = nombre,
                    onValueChange = { nombre = it },
                    label = { Text("Nombre (ej. Desayuno)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = calorias,
                    onValueChange = { calorias = it },
                    label = { Text("Calorías (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = proteinas,
                    onValueChange = { proteinas = it },
                    label = { Text("Proteínas (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = carbohidratos,
                    onValueChange = { carbohidratos = it },
                    label = { Text("Carbohidratos (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = grasas,
                    onValueChange = { grasas = it },
                    label = { Text("Grasas (g)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val cal = calorias.replace(',', '.').toDoubleOrNull() ?: 0.0
                val prot = proteinas.replace(',', '.').toDoubleOrNull() ?: 0.0
                val carb = carbohidratos.replace(',', '.').toDoubleOrNull() ?: 0.0
                val fat = grasas.replace(',', '.').toDoubleOrNull() ?: 0.0
                if (cal > 0 || nombre.isNotEmpty()) onConfirm(cal, prot, carb, fat, nombre.ifEmpty { "Comida" })
            }) { Text(if (comida == null) "Añadir" else "Guardar") }
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
