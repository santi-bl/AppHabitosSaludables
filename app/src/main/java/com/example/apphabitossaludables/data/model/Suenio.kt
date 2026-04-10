package com.example.apphabitossaludables.data.model

import com.google.firebase.firestore.Exclude
import java.time.Instant

data class Suenio(
    @get:Exclude var id: String ="",
    var duracionTotalMinutos: Long=0,
    var horaInicio: Instant?= null,
    var horaFin: Instant?=null,
    var etapas: Map<String, Long> =emptyMap()
)