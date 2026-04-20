package com.example.apphabitossaludables.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.apphabitossaludables.viewmodel.AuthViewModel

@Composable
fun LoginScreen(
    onLogin: (String) -> Unit, 
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit
) {
    val authViewModel: AuthViewModel = viewModel()
    val authState by authViewModel.authState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    LaunchedEffect(authState) {
        if (authState is AuthViewModel.AuthState.Success) {
            onLogin((authState as AuthViewModel.AuthState.Success).userId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.app),
            contentDescription = "Logo",
            modifier = Modifier.height(150.dp).fillMaxWidth()
        )
        
        Text(
            text = "BIENVENIDO",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 24.dp)
        )

        if (authState is AuthViewModel.AuthState.Error) {
            Text(
                text = (authState as AuthViewModel.AuthState.Error).message,
                color = Color.Red,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Contraseña") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp)
        )
        
        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            TextButton(onClick = onNavigateToForgotPassword) {
                Text("¿Olvidaste tu contraseña?", fontSize = 14.sp)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (authState is AuthViewModel.AuthState.Loading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = { authViewModel.login(email, password) },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Iniciar Sesión")
            }
            
            Spacer(Modifier.height(16.dp))
            
            // BOTÓN DE GOOGLE
            OutlinedButton(
                onClick = { /* Lógica de Google */ },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Black)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.google), 
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text("Continuar con Google")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        TextButton(onClick = onNavigateToRegister) {
            Text(
                text = "¿No tienes cuenta? Regístrate",
                textDecoration = TextDecoration.Underline,
                fontSize = 16.sp
            )
        }
    }
}
