/**
 * @author Santiago Barandiarán Lasheras
 * @description ViewModel principal que coordina la lógica de negocio, la interacción con Health Connect
 * y la sincronización en tiempo real con Firebase Firestore.
 */
package com.example.apphabitossaludables.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphabitossaludables.data.model.ActividadFisica
import com.example.apphabitossaludables.data.model.Nutricion
import com.example.apphabitossaludables.data.model.SignosVitales
import com.example.apphabitossaludables.data.model.Suenio
import com.example.apphabitossaludables.data.model.VitalidadSemanal
import com.example.apphabitossaludables.data.model.Usuario
import com.example.apphabitossaludables.data.repository.HealthRepository
import com.example.apphabitossaludables.data.repository.UserPreferencesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.time.LocalDate

class AppHabitusViewModel(
    private val repository: HealthRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // Configuración persistente
    val isDarkMode: StateFlow<Boolean> = preferencesRepository.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val notificationsEnabled: StateFlow<Boolean> = preferencesRepository.notificationsEnabledFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun toggleTheme(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDarkMode(enabled)
        }
    }

    fun toggleNotifications(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateNotificationsEnabled(enabled)
        }
    }

    private val _fechaSeleccionada = MutableStateFlow(LocalDate.now())
    val fechaSeleccionada: StateFlow<LocalDate> = _fechaSeleccionada.asStateFlow()

    private val _actividad = MutableStateFlow(ActividadFisica())
    val actividad: StateFlow<ActividadFisica> = _actividad

    private val _nutricion = MutableStateFlow(Nutricion())
    val nutricion: StateFlow<Nutricion> = _nutricion

    private val _signosVitales = MutableStateFlow(SignosVitales())
    val signosVitales: StateFlow<SignosVitales> = _signosVitales

    private val _sueño = MutableStateFlow<Suenio?>(null)
    val sueño: StateFlow<Suenio?> = _sueño

    private val _historialVitalidad = MutableStateFlow<List<VitalidadSemanal>>(emptyList())
    val historialVitalidad: StateFlow<List<VitalidadSemanal>> = _historialVitalidad

    private val _userName = MutableStateFlow("Usuario")
    val userName: StateFlow<String> = _userName.asStateFlow()

    private val _userProfile = MutableStateFlow<Usuario?>(null)
    val userProfile: StateFlow<Usuario?> = _userProfile.asStateFlow()

    private val _pesoActual = MutableStateFlow(0.0)
    val pesoActual: StateFlow<Double> = _pesoActual

    private val _historialPeso = MutableStateFlow<List<Pair<java.time.Instant, Double>>>(emptyList())
    val historialPeso: StateFlow<List<Pair<java.time.Instant, Double>>> = _historialPeso.asStateFlow()

    init {
        fetchUserProfile()
        viewModelScope.launch {
            _pesoActual.value = repository.obtenerPesoActual()
            _historialPeso.value = repository.obtenerHistorialPeso()
            cargarDatosDelDia()
        }
    }

    fun cambiarFecha(dias: Long) {
        val nuevaFecha = _fechaSeleccionada.value.plusDays(dias)
        if (!nuevaFecha.isAfter(LocalDate.now())) {
            _fechaSeleccionada.value = nuevaFecha
            cargarDatosDelDia()
        }
    }

    fun updateProfile(usuario: Usuario) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                db.collection("perfiles_detallados").document(uid).set(usuario.copy(id = uid)).await()
                fetchUserProfile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun agregarAgua(litros: Double) {
        viewModelScope.launch {
            try {
                repository.agregarHidratacion(litros)
                cargarDatosDelDia()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun agregarComida(calorias: Double, proteinas: Double, carbohidratos: Double, grasas: Double) {
        viewModelScope.launch {
            try {
                repository.agregarComida(calorias, proteinas, carbohidratos, grasas)
                cargarDatosDelDia()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarUltimaAgua() {
        viewModelScope.launch {
            repository.eliminarUltimaHidratacion()
            cargarDatosDelDia()
        }
    }

    fun guardarPeso(kg: Double) {
        viewModelScope.launch {
            try {
                repository.agregarPeso(kg)
                _pesoActual.value = kg
                _historialPeso.value = repository.obtenerHistorialPeso()
                val uid = auth.currentUser?.uid ?: return@launch
                db.collection("perfiles_detallados").document(uid).update("pesoKg", kg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun fetchUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                val document = db.collection("perfiles_detallados").document(uid).get().await()
                val usuario = document.toObject(Usuario::class.java)
                usuario?.let {
                    _userName.value = it.nombre
                    _userProfile.value = it
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cargarDatosDelDia() {
        val fecha = _fechaSeleccionada.value
        viewModelScope.launch {
            val nuevaActividad = repository.obtenerActividadFisica(fecha)
            val nuevaNutricion = repository.obtenerNutricion(fecha)
            val nuevosSignos = repository.obtenerSignosVitales(fecha)
            val nuevoSueno = repository.leerSuenoDelDia(fecha)
            val nuevoHistorial = repository.obtenerVitalidadSemanal()

            _actividad.value = nuevaActividad
            _nutricion.value = nuevaNutricion
            _signosVitales.value = nuevosSignos
            _sueño.value = nuevoSueno
            _historialVitalidad.value = nuevoHistorial

            sincronizarDatosConFirebase(nuevaActividad, nuevaNutricion, nuevosSignos, nuevoSueno, fecha)
        }
    }

    private suspend fun sincronizarDatosConFirebase(
        act: ActividadFisica,
        nut: Nutricion,
        sig: SignosVitales,
        sue: Suenio?,
        fecha: LocalDate
    ) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: ""
        
        // Cálculo del score de vitalidad para sincronizar
        val objPasos = _userProfile.value?.objetivoPasos?.toFloat() ?: 10000f
        val actScore = (act.pasos / objPasos).coerceIn(0f, 1f) * 40
        val sleepScore = ((sue?.duracionTotalMinutos ?: 0) / 450f).coerceIn(0f, 1f) * 40
        val nutScore = (nut.hidratacionLitros / 2.0).coerceIn(0.0, 1.0) * 20
        val totalScore = (actScore + sleepScore + nutScore).toInt()

        val datosMap = hashMapOf(
            "userId" to uid,
            "email" to email,
            "fecha" to fecha.toString(),
            "pasos" to act.pasos,
            "minutosActivos" to act.minutosActivos,
            "caloriasQuemadas" to act.caloriasQuemadas,
            "distanciaMetros" to act.distanciaMetros,
            "hidratacionLitros" to nut.hidratacionLitros,
            "caloriasConsumidas" to nut.caloriasConsumidas,
            "proteinasGramos" to nut.proteinasGramos,
            "carbohidratosGramos" to nut.carbohidratosGramos,
            "grasasGramos" to nut.grasasGramos,
            "frecuenciaCardiacaMedia" to sig.frecuenciaCardiacaMedia,
            "minutosSueno" to (sue?.duracionTotalMinutos ?: 0),
            "puntuacionVitalidad" to totalScore,
            "ultimaActualizacion" to com.google.firebase.Timestamp.now()
        )
        
        try {
            db.collection("datos_salud")
                .document(uid)
                .collection("historial")
                .document(fecha.toString())
                .set(datosMap)
                .await()
                
            if (fecha == LocalDate.now()) {
                db.collection("datos_salud").document(uid).set(datosMap).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
