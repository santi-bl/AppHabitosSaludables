package com.example.apphabitossaludables.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.health.connect.client.HealthConnectClient
import com.example.apphabitossaludables.MainActivity
import com.example.apphabitossaludables.R
import com.example.apphabitossaludables.data.repository.HealthRepository
import kotlinx.coroutines.*
import java.time.Instant

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = -1
    private var currentSteps = 0
    
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var repository: HealthRepository

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        repository = HealthRepository(healthConnectClient)

        createNotificationChannel()
        startForeground(1, createNotification("Contando tus pasos..."))

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "step_channel",
                "Contador de Pasos",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(content: String): Notification {
        val pendingIntent = Intent(this, MainActivity::class.java).let {
            PendingIntent.getActivity(this, 0, it, PendingIntent.FLAG_IMMUTABLE)
        }

        val logoBitmap = BitmapFactory.decodeResource(resources, R.drawable.logo)

        return NotificationCompat.Builder(this, "step_channel")
            .setContentTitle("Habitus en marcha")
            .setContentText(content)
            .setSmallIcon(R.drawable.logo) // Icono de la barra de estado
            .setLargeIcon(logoBitmap)     // Imagen grande en la notificación
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_STEP_COUNTER) {
            val totalStepsSinceBoot = event.values[0].toInt()
            
            if (initialSteps == -1) {
                initialSteps = totalStepsSinceBoot
            }
            
            val newSteps = totalStepsSinceBoot - initialSteps
            if (newSteps > currentSteps) {
                val difference = (newSteps - currentSteps).toLong()
                currentSteps = newSteps
                
                // Guardar en Health Connect periódicamente o tras cada paso
                serviceScope.launch {
                    repository.escribirPasos(
                        cantidad = difference,
                        inicio = Instant.now().minusSeconds(1),
                        fin = Instant.now()
                    )
                }
                
                updateNotification("Has dado $currentSteps pasos nuevos")
            }
        }
    }

    private fun updateNotification(content: String) {
        val notification = createNotification(content)
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(1, notification)
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        sensorManager.unregisterListener(this)
        serviceScope.cancel()
    }
}
