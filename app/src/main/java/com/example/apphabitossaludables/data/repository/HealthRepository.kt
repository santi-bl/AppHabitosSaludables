/**
 * @author Santiago Barandiarán Lasheras
 * @description Repositorio encargado de la comunicación directa con la API de Health Connect.
 * Implementa la lectura y escritura de pasos, peso, hidratación, sueño y nutrición.
 * Soporta la gestión detallada de comidas e hidratación para su visualización y borrado.
 */
package com.example.apphabitossaludables.data.repository

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Energy
import androidx.health.connect.client.units.Mass
import androidx.health.connect.client.units.Volume
import com.example.apphabitossaludables.data.model.*
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

class HealthRepository(private val healthConnectClient: HealthConnectClient) {

    suspend fun obtenerActividadFisica(fecha: LocalDate = LocalDate.now()): ActividadFisica {
        val startOfDay = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

        val pasos = try {
            healthConnectClient.readRecords(
                ReadRecordsRequest(StepsRecord::class, timeRange)
            ).records.sumOf { it.count }
        } catch (e: Exception) { 0L }

        val calorias = try {
            healthConnectClient.readRecords(
                ReadRecordsRequest(ActiveCaloriesBurnedRecord::class, timeRange)
            ).records.sumOf { it.energy.inKilocalories }
        } catch (e: Exception) { 0.0 }

        val distancia = try {
            healthConnectClient.readRecords(
                ReadRecordsRequest(DistanceRecord::class, timeRange)
            ).records.sumOf { it.distance.inMeters }
        } catch (e: Exception) { 0.0 }

        val listaSesiones = try {
            val ejercicioResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(ExerciseSessionRecord::class, timeRange)
            )
            ejercicioResponse.records.map { registro ->
                SesionEjercicio(
                    tipo = mapearTipoEjercicio(registro.exerciseType),
                    duracionMinutos = Duration.between(registro.startTime, registro.endTime).toMinutes(),
                    inicio = registro.startTime
                )
            }
        } catch (e: Exception) { emptyList() }

        val minutosActivos = listaSesiones.sumOf { it.duracionMinutos }

        return ActividadFisica(
            pasos = pasos,
            caloriasQuemadas = calorias,
            distanciaMetros = distancia,
            minutosActivos = minutosActivos,
            sesionesEjercicio = listaSesiones,
            ultimaActualizacion = Instant.now()
        )
    }

    suspend fun escribirPasos(cantidad: Long, inicio: Instant, fin: Instant) {
        val record = StepsRecord(
            startTime = inicio,
            endTime = fin,
            count = cantidad,
            startZoneOffset = ZoneId.systemDefault().rules.getOffset(inicio),
            endZoneOffset = ZoneId.systemDefault().rules.getOffset(fin)
        )
        try {
            healthConnectClient.insertRecords(listOf(record))
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun obtenerVitalidadSemanal(fechaReferencia: LocalDate = LocalDate.now()): List<VitalidadSemanal> {
        val lista = mutableListOf<VitalidadSemanal>()
        for (i in 6 downTo 0) {
            val fecha = fechaReferencia.minusDays(i.toLong())
            val startOfDay = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endOfDay = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

            try {
                val pasos = healthConnectClient.readRecords(ReadRecordsRequest(StepsRecord::class, timeRange)).records.sumOf { it.count }
                val agua = healthConnectClient.readRecords(ReadRecordsRequest(HydrationRecord::class, timeRange)).records.sumOf { it.volume.inLiters }
                val sueno = healthConnectClient.readRecords(ReadRecordsRequest(SleepSessionRecord::class, timeRange)).records.lastOrNull()
                val minSueno = sueno?.let { Duration.between(it.startTime, it.endTime).toMinutes() } ?: 0L

                val scorePasos = (pasos / 10000f * 40).coerceAtMost(40f)
                val scoreSueno = (minSueno / 480f * 40).coerceAtMost(40f)
                val scoreAgua = (agua / 2.5 * 20).coerceAtMost(20.0)
                
                lista.add(VitalidadSemanal(fecha, (scorePasos + scoreSueno + scoreAgua.toFloat()).toInt()))
            } catch (e: Exception) {
                lista.add(VitalidadSemanal(fecha, 0))
            }
        }
        return lista
    }

    private fun mapearTipoEjercicio(tipoInt: Int): String {
        return when (tipoInt) {
            ExerciseSessionRecord.EXERCISE_TYPE_RUNNING -> "Correr"
            ExerciseSessionRecord.EXERCISE_TYPE_WALKING -> "Caminar"
            ExerciseSessionRecord.EXERCISE_TYPE_BIKING -> "Ciclismo"
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER,
            ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL -> "Natación"
            ExerciseSessionRecord.EXERCISE_TYPE_STRENGTH_TRAINING -> "Pesas"
            else -> "Ejercicio"
        }
    }

    suspend fun obtenerNutricion(fecha: LocalDate = LocalDate.now()): Nutricion {
        val startOfDay = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

        return try {
            val hidratacionRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(HydrationRecord::class, timeRange)
            ).records
            
            val nutricionRecords = healthConnectClient.readRecords(
                ReadRecordsRequest(NutritionRecord::class, timeRange)
            ).records

            val listaComidas = nutricionRecords.map { record ->
                val nombreComida = record.name ?: when(record.mealType) {
                    1 -> "Desayuno"
                    2 -> "Almuerzo"
                    3 -> "Cena"
                    4 -> "Snack"
                    else -> "Comida"
                }
                Comida(
                    id = record.metadata.id,
                    nombre = nombreComida,
                    calorias = record.energy?.inKilocalories ?: 0.0,
                    proteinas = record.protein?.inGrams ?: 0.0,
                    carbohidratos = record.totalCarbohydrate?.inGrams ?: 0.0,
                    grasas = record.totalFat?.inGrams ?: 0.0,
                    momento = record.startTime
                )
            }.sortedByDescending { it.momento }

            val listaAgua = hidratacionRecords.map { record ->
                RegistroAgua(
                    id = record.metadata.id,
                    cantidadLitros = record.volume.inLiters,
                    momento = record.startTime
                )
            }.sortedByDescending { it.momento }

            Nutricion(
                hidratacionLitros = hidratacionRecords.sumOf { it.volume.inLiters },
                caloriasConsumidas = nutricionRecords.sumOf { it.energy?.inKilocalories ?: 0.0 },
                proteinasGramos = nutricionRecords.sumOf { it.protein?.inGrams ?: 0.0 },
                carbohidratosGramos = nutricionRecords.sumOf { it.totalCarbohydrate?.inGrams ?: 0.0 },
                grasasGramos = nutricionRecords.sumOf { it.totalFat?.inGrams ?: 0.0 },
                comidas = listaComidas,
                registrosAgua = listaAgua,
                fecha = Instant.now()
            )
        } catch (e: Exception) {
            println("Error leyendo nutricion: ${e.message}")
            Nutricion()
        }
    }

    suspend fun agregarComida(calorias: Double, proteinas: Double, carbohidratos: Double, grasas: Double, nombre: String = "Comida") {
        val ahora = Instant.now()
        val tipoComida = when (nombre.lowercase()) {
            "desayuno" -> 1
            "almuerzo", "comida" -> 2
            "cena" -> 3
            "snack" -> 4
            else -> 0
        }

        val record = NutritionRecord(
            startTime = ahora.minusSeconds(1),
            endTime = ahora,
            energy = Energy.kilocalories(calorias),
            protein = Mass.grams(proteinas),
            totalCarbohydrate = Mass.grams(carbohidratos),
            totalFat = Mass.grams(grasas),
            mealType = tipoComida,
            name = nombre,
            startZoneOffset = ZoneId.systemDefault().rules.getOffset(ahora),
            endZoneOffset = ZoneId.systemDefault().rules.getOffset(ahora)
        )
        healthConnectClient.insertRecords(listOf(record))
    }

    suspend fun eliminarComida(id: String) {
        try {
            healthConnectClient.deleteRecords(
                recordType = NutritionRecord::class,
                recordIdsList = listOf(id),
                clientRecordIdsList = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun obtenerSignosVitales(fecha: LocalDate = LocalDate.now()): SignosVitales {
        val startOfDay = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        val timeRange = TimeRangeFilter.between(startOfDay, endOfDay)

        return try {
            val pulsacionesResponse = healthConnectClient.readRecords(
                ReadRecordsRequest(HeartRateRecord::class, timeRange)
            )

            var sumaPulsacionesTotal = 0L
            var totalMuestrasTotal = 0
            val porHora = mutableMapOf<Int, MutableList<Long>>()

            for (registro in pulsacionesResponse.records) {
                val hora = registro.startTime.atZone(ZoneId.systemDefault()).hour
                if (!porHora.containsKey(hora)) porHora[hora] = mutableListOf()
                for (muestra in registro.samples) {
                    sumaPulsacionesTotal += muestra.beatsPerMinute
                    totalMuestrasTotal++
                    porHora[hora]?.add(muestra.beatsPerMinute)
                }
            }

            val mediaTotal = if (totalMuestrasTotal > 0) sumaPulsacionesTotal / totalMuestrasTotal else 0L
            val maximo = if (totalMuestrasTotal > 0) porHora.values.flatten().maxOrNull() ?: 0L else 0L
            val minimo = if (totalMuestrasTotal > 0) porHora.values.flatten().minOrNull() ?: 0L else 0L
            val mediasPorHora = porHora.mapValues { (_, lista) -> lista.average().toLong() }

            SignosVitales(
                frecuenciaCardiacaMedia = mediaTotal,
                frecuenciaCardiacaMaxima = maximo,
                frecuenciaCardiacaMinima = minimo,
                pulsacionesPorHora = mediasPorHora,
                fecha = Instant.now()
            )
        } catch (e: Exception) {
            SignosVitales()
        }
    }

    suspend fun leerSuenoDelDia(fecha: LocalDate = LocalDate.now()): Suenio? {
        val startOfDay = fecha.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = fecha.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(startOfDay, endOfDay))
            )
            if (response.records.isEmpty()) return null
            val sesion = response.records.last()
            val etapas = mutableMapOf<String, Long>()
            sesion.stages.forEach { etapa ->
                val nombreEtapa = when (etapa.stage) {
                    SleepSessionRecord.STAGE_TYPE_AWAKE -> "Despierto"
                    SleepSessionRecord.STAGE_TYPE_LIGHT -> "Sueño ligero"
                    SleepSessionRecord.STAGE_TYPE_DEEP -> "Sueño profundo"
                    SleepSessionRecord.STAGE_TYPE_REM -> "REM"
                    else -> "Otros"
                }
                val min = ChronoUnit.MINUTES.between(etapa.startTime, etapa.endTime)
                etapas[nombreEtapa] = (etapas[nombreEtapa] ?: 0L) + min
            }
            Suenio(
                duracionTotalMinutos = ChronoUnit.MINUTES.between(sesion.startTime, sesion.endTime),
                horaInicio = sesion.startTime,
                horaFin = sesion.endTime,
                etapas = etapas
            )
        } catch (e: Exception) { null }
    }

    suspend fun agregarHidratacion(litros: Double) {
        val ahora = Instant.now()
        val zoneOffset = ZoneId.systemDefault().rules.getOffset(ahora)
        val record = HydrationRecord(
            startTime = ahora.minusSeconds(10),
            endTime = ahora,
            volume = Volume.liters(litros),
            startZoneOffset = zoneOffset,
            endZoneOffset = zoneOffset
        )
        healthConnectClient.insertRecords(listOf(record))
    }

    suspend fun eliminarHidratacion(id: String) {
        try {
            healthConnectClient.deleteRecords(
                recordType = HydrationRecord::class,
                recordIdsList = listOf(id),
                clientRecordIdsList = emptyList()
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun eliminarUltimaHidratacion() {
        val hoy = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant()
        try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(HydrationRecord::class, TimeRangeFilter.after(hoy))
            )
            if (response.records.isNotEmpty()) {
                healthConnectClient.deleteRecords(HydrationRecord::class, listOf(response.records.last().metadata.id), emptyList())
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    suspend fun obtenerPesoActual(): Double = obtenerHistorialPeso().lastOrNull()?.second ?: 0.0

    suspend fun obtenerHistorialPeso(): List<Pair<Instant, Double>> {
        val inicio = Instant.now().minus(90, ChronoUnit.DAYS)
        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest(WeightRecord::class, TimeRangeFilter.after(inicio))
            )
            response.records.map { it.time to it.weight.inKilograms }
        } catch (e: Exception) { emptyList() }
    }

    suspend fun agregarPeso(kg: Double) {
        val ahora = Instant.now()
        val record = WeightRecord(
            time = ahora,
            weight = Mass.kilograms(kg),
            zoneOffset = ZoneId.systemDefault().rules.getOffset(ahora)
        )
        healthConnectClient.insertRecords(listOf(record))
    }
}
