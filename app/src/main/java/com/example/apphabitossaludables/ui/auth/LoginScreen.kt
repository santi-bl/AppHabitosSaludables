package com.example.apphabitossaludables.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.R

@Composable
fun LoginScreen(onLogin:(String) ->Unit,
                onNavigateToRegister:() -> Unit) {
    var email by remember { mutableStateOf("")}
    var password by remember { mutableStateOf("")}
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center)
    {
        Image(
            painter = painterResource(id= R.drawable.app),
            contentDescription = "Imagen de android",
            modifier = Modifier
                .height(250.dp)
                .fillMaxWidth()
        )
        Text(text = "INICIAR SESION",style = MaterialTheme.typography.headlineMedium,textDecoration = TextDecoration.Underline)
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("EMAIL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("CONTRASEÑA") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            modifier = Modifier.fillMaxWidth(), onClick =
                {    onLogin("user")    })
        { Text("Acceder") }
        Spacer(modifier = Modifier.height(20.dp))

        //LINEA SEPARADORA

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))
        TextButton(onClick =  onNavigateToRegister) {
            Text(
                text = "¿No tienes una cuenta? Regístrate",
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 18.sp,
                    textDecoration = TextDecoration.Underline
                )
            )
        }
    }
}