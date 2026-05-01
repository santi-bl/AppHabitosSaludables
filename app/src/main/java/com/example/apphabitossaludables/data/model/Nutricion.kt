/**
 * @author Santiago Barandiarán Lasheras
 * @description Modelo de datos para el seguimiento nutricional. Incluye el resumen diario
 * de macronutrientes, hidratación y las listas de comidas y registros de agua.
 */
package com.example.apphabitossaludables.data.model

import java.time.Instant

data class Nutricion(
    val hidratacionLitros: Double = 0.0,
    val caloriasConsumidas: Double = 0.0,
    val proteinasGramos: Double = 0.0,
    val carbohidratosGramos: Double = 0.0,
    val grasasGramos: Double = 0.0,
    val comidas: List<Comida> = emptyList(),
    val registrosAgua: List<RegistroAgua> = emptyList(),
    val registros: List<RegistroNutricional> = emptyList(),
    val fecha: Instant = Instant.now()
)

data class RegistroNutricional(
    val id: String,
    val nombre: String,
    val valor: String,
    val tipo: TipoRegistro,
    val momento: Instant,
    val kcal: Double = 0.0,
    val prot: Double = 0.0,
    val carb: Double = 0.0,
    val fat: Double = 0.0,
    val litros: Double = 0.0
)

enum class TipoRegistro { COMIDA, AGUA }

data class Comida(
    val id: String = "",
    val nombre: String = "Comida",
    val calorias: Double = 0.0,
    val proteinas: Double = 0.0,
    val carbohidratos: Double = 0.0,
    val grasas: Double = 0.0,
    val momento: Instant = Instant.now()
)

data class RegistroAgua(
    val id: String = "",
    val cantidadLitros: Double = 0.0,
    val momento: Instant = Instant.now()
)
