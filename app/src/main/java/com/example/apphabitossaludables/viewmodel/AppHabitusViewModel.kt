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
        val email = auth.currentUser?.email?.lowercase() ?: return
        val uid = auth.currentUser?.uid ?: ""
        viewModelScope.launch {
            try {
                db.collection("perfiles_detallados").document(email).set(usuario.copy(id = uid, correo = email)).await()
                fetchUserProfile()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun agregarAgua(litros: Double) {
        viewModelScope.launch {
            repository.agregarHidratacion(litros)
            if (_fechaSeleccionada.value == LocalDate.now()) {
                val nutricionActual = _nutricion.value
                val nuevaNutricion = nutricionActual.copy(
                    hidratacionLitros = nutricionActual.hidratacionLitros + litros
                )
                _nutricion.value = nuevaNutricion
                sincronizarDatosConFirebase(_actividad.value, nuevaNutricion, _signosVitales.value, _sueño.value, LocalDate.now())
            }
            cargarDatosDelDia()
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
            repository.agregarPeso(kg)
            _pesoActual.value = kg
            _historialPeso.value = repository.obtenerHistorialPeso()
            val email = auth.currentUser?.email?.lowercase() ?: return@launch
            db.collection("perfiles_detallados").document(email).update("pesoKg", kg)
        }
    }

    fun fetchUserProfile() {
        val email = auth.currentUser?.email?.lowercase() ?: return
        viewModelScope.launch {
            try {
                val document = db.collection("perfiles_detallados").document(email).get().await()
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

            // Sincronizamos siempre con la colección de historial por fecha
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
        val email = auth.currentUser?.email?.lowercase() ?: ""
        
        val datosMap = hashMapOf(
            "userId" to uid,
            "email" to email,
            "fecha" to fecha.toString(),
            "pasos" to act.pasos,
            "calorias" to act.caloriasQuemadas,
            "distancia" to act.distanciaMetros,
            "hidratacion" to nut.hidratacionLitros,
            "frecuenciaCardiacaMedia" to sig.frecuenciaCardiacaMedia,
            "minutosSueno" to (sue?.duracionTotalMinutos ?: 0),
            "ultimaActualizacion" to com.google.firebase.Timestamp.now()
        )
        
        try {
            // Guardamos en la subcolección 'historial' dentro del documento del usuario (email)
            // de esta forma tenemos un registro por cada día
            db.collection("datos_salud")
                .document(email)
                .collection("historial")
                .document(fecha.toString())
                .set(datosMap)
                .await()
                
            // También mantenemos un resumen en el documento principal para acceso rápido al "estado actual"
            if (fecha == LocalDate.now()) {
                db.collection("datos_salud").document(email).set(datosMap).await()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
