/**
 * @author Santiago Barandiarán Lasheras
 * @description Modelo de datos que encapsula la actividad física diaria del usuario,
 * incluyendo pasos, calorías, distancia y detalle de sesiones de ejercicio.
 */
package com.example.apphabitossaludables.data.model

import java.time.Instant

data class ActividadFisica(
    val pasos: Long = 0,
    val caloriasQuemadas: Double = 0.0,
    val distanciaMetros: Double = 0.0,
    val minutosActivos: Long = 0,
    val sesionesEjercicio: List<SesionEjercicio> = emptyList(),
    val ultimaActualizacion: Instant = Instant.now()
)

data class SesionEjercicio(
    val tipo: String,
    val duracionMinutos: Long,
    val calorias: Double? = null,
    val inicio: Instant
)
