package com.example.apphabitossaludables.data.repository

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

class HealthRepository(private val healthConnectClient: HealthConnectClient)
{

    suspend fun leerPasosDelDia(): Long {
        // Definimos el inicio del día (00:00 de hoy) en la zona horaria local
        val startOfDay = ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = Instant.now()

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest<StepsRecord>(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            // Sumamos todos los registros y devolvemos el total
            val totalPasos = response.records.sumOf { it.count }
            println("Total de pasos recogidos: $totalPasos")

            totalPasos
        } catch (e: Exception) {
            // Manejar error (por ejemplo, si el usuario no dio permisos)
            println("Error leyendo pasos: ${e.message}")
            0L // Devolvemos 0 si falla
        }
    }

    suspend fun leerPulsacionesDelDia(): Long {
        // Definimos el inicio del día (00:00 de hoy)
        val startOfDay = ZonedDateTime.now().toLocalDate().atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = Instant.now()

        return try {
            val response = healthConnectClient.readRecords(
                ReadRecordsRequest<HeartRateRecord>(
                    recordType = HeartRateRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(startOfDay, endOfDay)
                )
            )

            var sumaPulsaciones = 0L
            var totalMuestras = 0

            // Recorremos todos los registros y sus muestras internas
            for (registro in response.records) {
                for (muestra in registro.samples) {
                    sumaPulsaciones += muestra.beatsPerMinute
                    totalMuestras++
                }
            }

            // Calculamos y devolvemos la media
            if (totalMuestras > 0) {
                val mediaPulsaciones = sumaPulsaciones / totalMuestras
                println("Pulsaciones medias recogidas: $mediaPulsaciones (de $totalMuestras muestras)")
                mediaPulsaciones
            } else {
                println("No se encontraron datos de pulsaciones hoy")
                0L
            }

        } catch (e: Exception) {
            println("Error leyendo pulsaciones: ${e.message}")
            0L // Devolvemos 0 si falla
        }
    }
    suspend fun leerSuenoDelDia(): Long {
        // aquí irá la lógica de sueño
        return 0L
    }
}