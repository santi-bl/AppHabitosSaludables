package com.example.apphabitossaludables.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyScreen(onBack: () -> Unit) {
    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Privacidad", fontWeight = FontWeight.Bold) },
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
                .verticalScroll(scrollState)
                .padding(24.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Security,
                    contentDescription = null,
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(32.dp))

            PrivacySection(
                title = "¿Qué datos recogemos?",
                content = "Recogemos datos sobre tu actividad física (pasos, calorías, distancia), nutrición (hidratación), signos vitales (frecuencia cardíaca), sueño, medidas corporales (peso y altura) y tu perfil de usuario (nombre y correo)."
            )

            PrivacySection(
                title = "¿De dónde vienen los datos?",
                content = "La mayoría de los datos de salud provienen de Health Connect, sincronizados desde tus dispositivos y otras aplicaciones de salud. Los datos de perfil y peso inicial son proporcionados directamente por ti."
            )

            PrivacySection(
                title = "¿Dónde se almacenan?",
                content = "Tus datos se almacenan de forma segura en dos lugares: localmente en tu dispositivo a través de Health Connect y en la nube utilizando Firebase Firestore para que puedas acceder a ellos desde cualquier lugar."
            )

            PrivacySection(
                title = "¿Para qué se usan?",
                content = "Utilizamos estos datos para calcular tu Índice de Vitalidad diario, ofrecerte un seguimiento detallado de tu progreso y personalizar las recomendaciones de salud dentro de la aplicación."
            )

            PrivacySection(
                title = "¿Qué NO hacemos con tus datos?",
                content = "Tu privacidad es nuestra prioridad. NO vendemos tus datos a terceros, NO los utilizamos para fines publicitarios ni compartimos tu información personal con anunciantes."
            )

            Spacer(Modifier.height(32.dp))
        }
    }
}

@Composable
fun PrivacySection(title: String, content: String) {
    Column(modifier = Modifier.padding(bottom = 24.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = content,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            lineHeight = 22.sp
        )
        HorizontalDivider(
            modifier = Modifier.padding(top = 16.dp),
            thickness = 0.5.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}
