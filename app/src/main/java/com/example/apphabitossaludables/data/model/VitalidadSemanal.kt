/**
 * @author Santiago Barandiarán Lasheras
 * @description Representa la puntuación de vitalidad alcanzada en un día específico.
 * Se utiliza para construir el histórico de actividad y gráficos de progreso.
 */
package com.example.apphabitossaludables.data.model

import java.time.LocalDate

data class VitalidadSemanal(
    val fecha: LocalDate,
    val puntuacion: Int
)
