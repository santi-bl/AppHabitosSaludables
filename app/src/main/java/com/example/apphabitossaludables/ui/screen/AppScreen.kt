package com.example.apphabitossaludables.ui.screen

import android.media.Image
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.Role.Companion.Image
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.apphabitossaludables.R

@Composable
fun AppScreen(){
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLogin = { username ->
                    nav.navigate("welcome/$username") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { nav.navigate("register") })
        }
    }
}

@Composable
fun LoginScreen(onLogin:(String) ->Unit,
                onNavigateToRegister:() -> Unit) {
    var correo by remember { mutableStateOf("")}
    var contrasenia by remember { mutableStateOf("")}
    var error by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center)
    {
        Image(
            painter = painterResource(id= R.drawable.ic_launcher_foreground),
            contentDescription = "Imagen de android",
            modifier = Modifier.size(200.dp)
        )
        Text(text = "INICIAR SESION",style = MaterialTheme.typography.headlineMedium,textDecoration = TextDecoration.Underline)
        Spacer(Modifier.height(10.dp))
        TextField(
            value = correo,
            onValueChange = { correo = it },
            label = { Text("EMAIL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        TextField(
            value = contrasenia,
            onValueChange = { contrasenia = it },
            label = { Text("CONTRASEÑA") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(20.dp))
        Button(
            modifier = Modifier.fillMaxWidth(), onClick =
                {        })
        { Text("Acceder") }
        Spacer(modifier = Modifier.height(20.dp))

        //LINEA SEPARADORA

        HorizontalDivider(
            modifier = Modifier.padding(vertical = 8.dp),
            thickness = 1.dp,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(20.dp))
    }
}