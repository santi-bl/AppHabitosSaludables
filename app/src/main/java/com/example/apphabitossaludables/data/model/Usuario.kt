package com.example.apphabitossaludables.data.model

import com.google.firebase.firestore.Exclude
import java.util.Date

data class Usuario(
    @get:Exclude var id: String ="",
    var nombre: String ="",
    var apellidos: String ="",
    var correo:String="",
    var contraseña:String="",
    var fechaNacimiento: Date? = null,
    var pesoKg: Double = 0.0,
    var alturaCm: Int = 0,
    var genero: String = "",
    var objetivoPasos: Int = 10000,
    var nivelActividad: String = "Moderado"
)