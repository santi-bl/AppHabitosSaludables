package com.example.apphabitossaludables.data.model

import com.google.firebase.firestore.Exclude
import java.util.Date

data class Usuario(
    @get:Exclude var id: String ="",
    var nombre: String ="",
    var apellidos: String ="",
    var correo:String="",
    var contraseña:String="",
    var fechaNacimiento: Date,


)