package com.example.apphabitossaludables.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apphabitossaludables.R
import com.example.apphabitossaludables.viewmodel.AuthViewModel

@Composable
fun ForgotPasswordScreen(onBack: () -> Unit) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()
    var email by remember { mutableStateOf("") }
    var emailSent by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.isotipo),
            contentDescription = "Logo",
            modifier = Modifier.size(100.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Recuperar Contraseña",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Te enviaremos un enlace para restablecer tu cuenta",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        if (authState is AuthViewModel.AuthState.Error) {
            Text(text = (authState as AuthViewModel.AuthState.Error).message, color = Color.Red)
        }

        if (emailSent) {
            Text(
                text = "¡Email enviado! Revisa tu bandeja de entrada.",
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(Modifier.height(24.dp))

        if (authState is AuthViewModel.AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { 
                    authViewModel.resetPassword(email)
                    emailSent = true
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Enviar Enlace")
            }
        }

        TextButton(onClick = onBack) {
            Text("Volver al inicio de sesión")
        }
    }
}
