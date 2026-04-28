/**
 * @author Santiago Barandiarán Lasheras
 * @description Modelo de datos para el seguimiento nutricional, incluyendo
 * hidratación, calorías y desglose de macronutrientes (proteínas, carbohidratos, grasas).
 */
package com.example.apphabitossaludables.data.model

import java.time.Instant

data class Nutricion(
    val hidratacionLitros: Double = 0.0,
    val caloriasConsumidas: Double = 0.0,
    val proteinasGramos: Double = 0.0,
    val carbohidratosGramos: Double = 0.0,
    val grasasGramos: Double = 0.0,
    val fecha: Instant = Instant.now()
)
