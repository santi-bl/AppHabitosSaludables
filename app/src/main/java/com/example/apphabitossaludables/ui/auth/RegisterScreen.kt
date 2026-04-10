package com.example.apphabitossaludables.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.apphabitossaludables.R

@Composable
fun RegiesterScreen(onRegister_Login:(String) -> Unit){

    var alias by remember{mutableStateOf("")}
    var name by remember{mutableStateOf("")}
    var lastname by remember{mutableStateOf("")}
    var email by remember{mutableStateOf("")}
    var password by remember{mutableStateOf("")}
    var borndate by remember{mutableStateOf(null)}

    Column(Modifier
        .fillMaxSize()
        .padding(20.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Top)
    {
        Image(
            painter = painterResource(id= R.drawable.isotipo),
            contentDescription = "Imagen de android",
            modifier = Modifier
                .height(200.dp)
                .fillMaxWidth()
        )
        Text(text = "Empieza a usar Habitus",style = MaterialTheme.typography.headlineMedium, fontSize = 24.sp)
        Text(text = "Regístrate para llevar mejores habitos y mejorar día a día junto a nosotros",style = MaterialTheme.typography.headlineMedium, fontSize = 16.sp)
        Text(text = "Nombre",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("NOMBRE") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Apellidos",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = lastname,
            onValueChange = { lastname = it },
            label = { Text("APELLIDOS") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Email",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("EMAIL") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))
        Text(text = "Contraseña",style = MaterialTheme.typography.headlineMedium)
        TextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("CONTRASEÑA") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(10.dp))

    }
}