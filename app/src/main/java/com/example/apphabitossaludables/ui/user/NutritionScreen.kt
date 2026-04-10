package com.example.apphabitossaludables.ui.user

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val nutricion by viewModel.nutricion.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nutrición e Hidratación") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Hidratación", style = MaterialTheme.typography.titleMedium)
                    Text(String.format(Locale.getDefault(), "%.2f Litros", nutricion.hidratacionLitros), style = MaterialTheme.typography.headlineLarge)
                }
            }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Calorías Consumidas", style = MaterialTheme.typography.titleMedium)
                    Text("${nutricion.caloriasConsumidas} kcal", style = MaterialTheme.typography.headlineSmall)
                }
            }

            Text("Macronutrientes", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(top = 8.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Proteínas")
                        Text("${nutricion.proteinasGramos}g", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Carbohidratos")
                        Text("${nutricion.carbohidratosGramos}g", style = MaterialTheme.typography.titleMedium)
                    }
                }
                Card(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Grasas")
                        Text("${nutricion.grasasGramos}g", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
