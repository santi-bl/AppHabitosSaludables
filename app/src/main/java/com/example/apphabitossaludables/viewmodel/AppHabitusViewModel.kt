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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AppHabitusViewModel(
    private val repository: HealthRepository
) : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    // StateFlow para cada categoría de datos
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

    fun agregarAgua(litros: Double) {
        viewModelScope.launch {
            repository.agregarHidratacion(litros)
            
            // Actualizar el estado local inmediatamente para feedback visual
            val nutricionActual = _nutricion.value
            val nuevaNutricion = nutricionActual.copy(
                hidratacionLitros = nutricionActual.hidratacionLitros + litros
            )
            _nutricion.value = nuevaNutricion

            // Sincronizar con Firebase
            sincronizarDatosConFirebase(
                _actividad.value,
                nuevaNutricion,
                _signosVitales.value,
                _sueño.value
            )
            
            // Recargar todo por si acaso
            cargarDatosDelDia()
        }
    }

    fun eliminarUltimaAgua() {
        viewModelScope.launch {
            repository.eliminarUltimaHidratacion()
            
            // Recargar datos para reflejar el borrado
            val nuevaNutricion = repository.obtenerNutricion()
            _nutricion.value = nuevaNutricion

            // Sincronizar el borrado con Firebase
            sincronizarDatosConFirebase(
                _actividad.value,
                nuevaNutricion,
                _signosVitales.value,
                _sueño.value
            )

            cargarDatosDelDia()
        }
    }

    fun guardarPeso(kg: Double) {
        viewModelScope.launch {
            repository.agregarPeso(kg)
            _pesoActual.value = kg
            _historialPeso.value = repository.obtenerHistorialPeso()
            val email = auth.currentUser?.email ?: return@launch
            db.collection("perfiles_detallados").document(email).update("pesoKg", kg)
        }
    }

    fun fetchUserProfile() {
        val email = auth.currentUser?.email ?: return
        viewModelScope.launch {
            try {
                // Ahora buscamos en "perfiles_detallados" usando el email como clave
                val document = db.collection("perfiles_detallados").document(email).get().await()
                val usuario = document.toObject(Usuario::class.java)
                usuario?.let {
                    _userName.value = it.nombre
                }
            } catch (e: Exception) {
                // Manejar error
            }
        }
    }

    fun cargarDatosDelDia() {
        // Aseguramos que tenemos el perfil del usuario (nombre, etc) al cargar datos
        fetchUserProfile()

        viewModelScope.launch {
            val nuevaActividad = repository.obtenerActividadFisica()
            val nuevaNutricion = repository.obtenerNutricion()
            val nuevosSignos = repository.obtenerSignosVitales()
            val nuevoSueno = repository.leerSuenoDelDia()
            val nuevoHistorial = repository.obtenerVitalidadSemanal()

            _actividad.value = nuevaActividad
            _nutricion.value = nuevaNutricion
            _signosVitales.value = nuevosSignos
            _sueño.value = nuevoSueno
            _historialVitalidad.value = nuevoHistorial

            // Sincronizamos con Firebase
            sincronizarDatosConFirebase(nuevaActividad, nuevaNutricion, nuevosSignos, nuevoSueno)
        }
    }

    private suspend fun sincronizarDatosConFirebase(
        act: ActividadFisica,
        nut: Nutricion,
        sig: SignosVitales,
        sue: Suenio?
    ) {
        val email = auth.currentUser?.email ?: return
        val datosMap = hashMapOf(
            "email" to email,
            "pasos" to act.pasos,
            "calorias" to act.caloriasQuemadas,
            "distancia" to act.distanciaMetros,
            "hidratacion" to nut.hidratacionLitros,
            "frecuenciaCardiacaMedia" to sig.frecuenciaCardiacaMedia,
            "minutosSueno" to (sue?.duracionTotalMinutos ?: 0),
            "ultimaActualizacion" to com.google.firebase.Timestamp.now()
        )

        try {
            // Guardamos en la colección "datos_salud" usando el email como clave
            db.collection("datos_salud").document(email).set(datosMap).await()
        } catch (e: Exception) {
            // Error al sincronizar
        }
    }
}