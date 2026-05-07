/**
 * @author Santiago Barandiarán Lasheras
 * @description Modelo de datos que representa el perfil completo de un usuario,
 * incluyendo sus datos físicos, objetivos y configuración de salud.
 */
package com.example.apphabitossaludables.data.model

import com.google.firebase.firestore.Exclude
import java.util.Date

data class Usuario(
    @get:Exclude var id: String ="",
    var nombre: String ="",
    var apellidos: String ="",
    var correo:String="",
    var edad: Int = 0,
    var pesoKg: Double = 0.0,
    var alturaCm: Int = 0,
    var genero: String = "",
    var objetivoPasos: Int = 10000,
    var nivelActividad: String = "Moderado"
)
