/**
 * @author Santiago Barandiarán Lasheras
 * @description Servicio en primer plano (Foreground Service) que utiliza el sensor de podómetro
 * del dispositivo para contar pasos en tiempo real, incluso cuando la app está cerrada,
 * y los registra en Health Connect.
 */
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
import com.example.apphabitossaludables.data.repository.UserPreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.time.Instant
import java.time.LocalDate

class StepCounterService : Service(), SensorEventListener {

    private lateinit var sensorManager: SensorManager
    private var stepSensor: Sensor? = null
    private var initialSteps = -1
    private var stepsInThisSession = 0
    private var totalStepsToday = 0
    
    private var milestone3k = false
    private var milestone6k = false
    private var milestone10k = false

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private lateinit var healthRepository: HealthRepository
    private lateinit var prefsRepository: UserPreferencesRepository

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        stepSensor = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        
        val healthConnectClient = HealthConnectClient.getOrCreate(this)
        healthRepository = HealthRepository(healthConnectClient)
        prefsRepository = UserPreferencesRepository(this)

        createNotificationChannel()
        startForeground(1, createNotification("Iniciando contador de pasos..."))

        // Inicializar pasos del día desde Health Connect
        serviceScope.launch {
            val actividad = healthRepository.obtenerActividadFisica(LocalDate.now())
            totalStepsToday = actividad.pasos.toInt()
            updateMilestonesFromSteps(totalStepsToday)
            updateNotification("Hoy llevas $totalStepsToday pasos. ¡A por más!")
        }

        stepSensor?.let {
            sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }
    }

    private fun updateMilestonesFromSteps(steps: Int) {
        if (steps >= 3000) milestone3k = true
        if (steps >= 6000) milestone6k = true
        if (steps >= 10000) milestone10k = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "step_channel",
                "Seguimiento de Actividad",
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
            .setContentTitle("Habitus")
            .setContentText(content)
            .setSmallIcon(R.drawable.logo)
            .setLargeIcon(logoBitmap)
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
            
            val newStepsInSession = totalStepsSinceBoot - initialSteps
            if (newStepsInSession > stepsInThisSession) {
                val diff = (newStepsInSession - stepsInThisSession).toLong()
                stepsInThisSession = newStepsInSession
                totalStepsToday += diff.toInt()
                
                // Guardar en Health Connect
                serviceScope.launch {
                    healthRepository.escribirPasos(diff, Instant.now().minusSeconds(1), Instant.now())
                }
                
                checkMotivationalMilestones(totalStepsToday)
            }
        }
    }

    private fun checkMotivationalMilestones(steps: Int) {
        serviceScope.launch {
            // Verificar si las notificaciones están habilitadas en ajustes
            val enabled = prefsRepository.notificationsEnabledFlow.first()
            if (!enabled) {
                updateNotification("Llevas $steps pasos hoy")
                return@launch
            }

            val message = when {
                steps >= 10000 && !milestone10k -> {
                    milestone10k = true
                    "¡OBJETIVO CUMPLIDO! 10.000 pasos. ¡Tu cuerpo te lo agradece, eres una máquina!"
                }
                steps >= 6000 && !milestone6k -> {
                    milestone6k = true
                    "¡Se nota de qué estás hecho! 6000 pasos superados. ¡Ese es el ritmo de un campeón!"
                }
                steps >= 3000 && !milestone3k -> {
                    milestone3k = true
                    "¡Vamos, tú puedes! Ya llevas 3000 pasos. ¡Un pequeño esfuerzo más y llegamos a la meta!"
                }
                else -> null
            }

            if (message != null) {
                sendMotivationalNotification(message)
            } else {
                updateNotification("Llevas $steps pasos hoy")
            }
        }
    }

    private fun sendMotivationalNotification(message: String) {
        val notification = NotificationCompat.Builder(this, "step_channel")
            .setContentTitle("¡Meta alcanzada!")
            .setContentText(message)
            .setSmallIcon(R.drawable.logo)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()
            
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(2, notification) // ID 2 para no pisar la notificación persistente
        
        // También actualizamos la principal
        updateNotification(message)
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
