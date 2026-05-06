/**
 * @author Santiago Barandiarán Lasheras
 * @description Pantalla de edición de perfil. Permite al usuario actualizar sus datos
 * personales y físicos, sincronizando los cambios con Firebase Firestore.
 */
package com.example.apphabitossaludables.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.data.model.Usuario
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import com.google.firebase.auth.FirebaseAuth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(viewModel: AppHabitusViewModel, onBack: () -> Unit) {
    val userProfile by viewModel.userProfile.collectAsState()
    val scrollState = rememberScrollState()
    
    var nombre by remember { mutableStateOf("") }
    var apellidos by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var edad by remember { mutableStateOf("") }
    var peso by remember { mutableStateOf("") }
    var altura by remember { mutableStateOf("") }
    var genero by remember { mutableStateOf("Hombre") }
    var nivelActividad by remember { mutableStateOf("Moderado") }
    var objetivoPasos by remember { mutableStateOf("10000") }

    LaunchedEffect(userProfile) {
        userProfile?.let {
            nombre = it.nombre
            apellidos = it.apellidos
            email = it.correo
            edad = if (it.edad > 0) it.edad.toString() else ""
            peso = if (it.pesoKg > 0) it.pesoKg.toString() else ""
            altura = if (it.alturaCm > 0) it.alturaCm.toString() else ""
            genero = if (it.genero.isNotEmpty()) it.genero else "Hombre"
            nivelActividad = if (it.nivelActividad.isNotEmpty()) it.nivelActividad else "Moderado"
            objetivoPasos = it.objetivoPasos.toString()
        } ?: run {
            email = FirebaseAuth.getInstance().currentUser?.email ?: ""
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mi Perfil de Salud", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Volver")
                    }
                }
            )
        },
        bottomBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 8.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Box(modifier = Modifier.padding(24.dp)) {
                    Button(
                        onClick = {
                            val edadValue = edad.toIntOrNull() ?: 0
                            val pesoValue = peso.replace(',', '.').toDoubleOrNull() ?: 0.0
                            val alturaValue = altura.toIntOrNull() ?: 0
                            val pasosValue = objetivoPasos.toIntOrNull() ?: 10000
                            
                            val updatedUser = userProfile?.copy(
                                nombre = nombre,
                                apellidos = apellidos,
                                edad = edadValue,
                                pesoKg = pesoValue,
                                alturaCm = alturaValue,
                                genero = genero,
                                nivelActividad = nivelActividad,
                                objetivoPasos = pasosValue
                            ) ?: Usuario(
                                id = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                                nombre = nombre,
                                apellidos = apellidos,
                                correo = email.ifEmpty { FirebaseAuth.getInstance().currentUser?.email ?: "" },
                                edad = edadValue,
                                pesoKg = pesoValue,
                                alturaCm = alturaValue,
                                genero = genero,
                                nivelActividad = nivelActividad,
                                objetivoPasos = pasosValue
                            )
                            
                            viewModel.updateProfile(updatedUser)
                            
                            if (pesoValue > 0 && (userProfile == null || pesoValue != userProfile?.pesoKg)) {
                                viewModel.guardarPeso(pesoValue)
                            }
                            
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("GUARDAR CAMBIOS", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = null,
                modifier = Modifier.size(100.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            
            Spacer(Modifier.height(24.dp))

            EditSectionTitle("Datos Personales")
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = apellidos,
                onValueChange = { apellidos = it },
                label = { Text("Apellidos") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Person, null) },
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = email,
                onValueChange = { },
                label = { Text("Correo Electrónico") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.Default.Email, null) },
                readOnly = true,
                enabled = false,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            EditSectionTitle("Perfil Físico")
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = edad,
                    onValueChange = { if (it.all { char -> char.isDigit() }) edad = it },
                    label = { Text("Edad") },
                    modifier = Modifier.weight(0.7f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = peso,
                    onValueChange = { if (it.all { char -> char.isDigit() || char == '.' || char == ',' }) peso = it },
                    label = { Text("Peso (kg)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp)
                )
                OutlinedTextField(
                    value = altura,
                    onValueChange = { if (it.all { char -> char.isDigit() }) altura = it },
                    label = { Text("Altura (cm)") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp)
                )
            }
            
            Spacer(Modifier.height(16.dp))
            
            Text("Género", modifier = Modifier.fillMaxWidth(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Hombre", "Mujer", "Otro").forEach { option ->
                    FilterChip(
                        selected = genero == option,
                        onClick = { genero = option },
                        label = { Text(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(24.dp))

            EditSectionTitle("Configuración de Objetivos")
            OutlinedTextField(
                value = objetivoPasos,
                onValueChange = { if (it.all { char -> char.isDigit() }) objetivoPasos = it },
                label = { Text("Objetivo de pasos diarios") },
                modifier = Modifier.fillMaxWidth(),
                leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(Modifier.height(24.dp))

            EditSectionTitle("Nivel de Actividad")
            val niveles = listOf("Sedentario", "Ligero", "Moderado", "Intenso")
            var expanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = nivelActividad,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Nivel de Actividad") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    niveles.forEach { nivel ->
                        DropdownMenuItem(
                            text = { Text(nivel) },
                            onClick = {
                                nivelActividad = nivel
                                expanded = false
                            }
                        )
                    }
                }
            }
            
            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun EditSectionTitle(title: String) {
    Text(
        text = title,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.primary,
        fontWeight = FontWeight.Bold
    )
}
