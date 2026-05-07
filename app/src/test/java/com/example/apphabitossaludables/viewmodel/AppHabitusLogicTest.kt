package com.example.apphabitossaludables.viewmodel

import com.example.apphabitossaludables.data.model.ActividadFisica
import com.example.apphabitossaludables.data.model.Nutricion
import com.example.apphabitossaludables.data.model.Suenio
import com.example.apphabitossaludables.data.model.Usuario
import com.example.apphabitossaludables.data.model.SesionEjercicio
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class AppHabitusLogicTest {

    @Test
    fun `score calculation returns 100 when all goals are met`() {
        val actividad = ActividadFisica(pasos = 10000)
        val nutricion = Nutricion(hidratacionLitros = 2.0)
        val suenio = Suenio(duracionTotalMinutos = 450)
        val usuario = Usuario(objetivoPasos = 10000)

        val score = calculateScore(actividad, nutricion, suenio, usuario)
        assertEquals(100, score)
    }

    @Test
    fun `calories burned calculation uses correct MET for running`() {
        val peso = 70.0
        val duracion = 30L
        // Añadimos Instant.now() para corregir el error de parámetro faltante
        val sesiones = listOf(SesionEjercicio(tipo = "Correr", duracionMinutos = duracion, inicio = Instant.now()))
        val actividad = ActividadFisica(sesionesEjercicio = sesiones)
        
        val calorias = calcularCaloriasMET(actividad, peso)
        // kcal = (8.0 * 3.5 * 70 / 200) * 30 = 294 kcal
        assertEquals(294.0, calorias, 0.5)
    }

    @Test
    fun `distance estimation logic is accurate based on height`() {
        val pasos = 1000L
        val usuarioHombre = Usuario(alturaCm = 180, genero = "Hombre")
        val distancia = calcularDistanciaEstimada(pasos, usuarioHombre)
        // 180 * 0.415 = 74.7cm/paso -> 1000 pasos = 747m
        assertEquals(747.0, distancia, 0.1)
    }

    // Funciones espejo de la lógica del ViewModel para testing unitario
    private fun calculateScore(act: ActividadFisica, nut: Nutricion, sue: Suenio?, profile: Usuario?): Int {
        val objPasos = profile?.objetivoPasos?.toFloat() ?: 8000f
        val actScore = (act.pasos / objPasos).coerceIn(0f, 1f) * 40
        val sleepScore = ((sue?.duracionTotalMinutos ?: 0) / 450f).coerceIn(0f, 1f) * 40
        val nutScore = (nut.hidratacionLitros / 2.0).coerceIn(0.0, 1.0) * 20
        return (actScore + sleepScore + nutScore).toInt()
    }

    private fun calcularCaloriasMET(act: ActividadFisica, peso: Double): Double {
        var totalKcal = 0.0
        if (act.sesionesEjercicio.isNotEmpty()) {
            totalKcal = act.sesionesEjercicio.sumOf { sesion ->
                val met = when (sesion.tipo) {
                    "Correr" -> 8.0
                    "Caminar" -> 3.5
                    "Ciclismo" -> 7.5
                    "Natación" -> 7.0
                    "Pesas" -> 5.0
                    else -> 4.5
                }
                (met * 3.5 * peso / 200.0) * sesion.duracionMinutos
            }
        }
        return totalKcal
    }

    private fun calcularDistanciaEstimada(pasos: Long, usuario: Usuario?): Double {
        val altura = if (usuario != null && usuario.alturaCm > 0) usuario.alturaCm.toDouble() else 170.0
        val factor = if (usuario?.genero?.lowercase()?.contains("mujer") == true) 0.413 else 0.415
        return pasos * ((altura * factor) / 100.0)
    }
}
