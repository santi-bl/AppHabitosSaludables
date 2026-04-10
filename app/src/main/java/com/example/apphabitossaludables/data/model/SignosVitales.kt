package com.example.apphabitossaludables.data.model

import java.time.Instant

data class SignosVitales(
    val frecuenciaCardiacaMedia: Long = 0,
    val frecuenciaCardiacaMinima: Long = 0,
    val frecuenciaCardiacaMaxima: Long = 0,
    val frecuenciaCardiacaReposo: Long = 0,
    val pulsacionesPorHora: Map<Int, Long> = emptyMap(), // Hora (0-23) -> Pulsaciones media en esa hora
    val saturacionOxigeno: Double = 0.0,
    val presionSistolica: Double = 0.0,
    val presionDiastolica: Double = 0.0,
    val temperaturaCorporal: Double = 0.0,
    val fecha: Instant = Instant.now()
)