/**
 * @author Santiago Barandiarán Lasheras
 * @description ViewModel principal que coordina la lógica de negocio, la interacción con Health Connect
 * y la sincronización en tiempo real con Firebase Firestore.
 * Gestiona el perfil del usuario, metas de salud y preferencias de interfaz.
 */
package com.example.apphabitossaludables.viewmodel

import android.util.Log
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
import kotlinx.coroutines.flow.map
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

    // Configuración persistente (Boolean? para permitir seguir el sistema por defecto)
    val isDarkMode: StateFlow<Boolean?> = preferencesRepository.isDarkModeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

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

    private val _userProfile = MutableStateFlow<Usuario?>(null)
    val userProfile: StateFlow<Usuario?> = _userProfile.asStateFlow()

    // Propiedades reactivas derivadas del perfil del usuario
    val userName: StateFlow<String> = userProfile
        .map { usuario ->
            usuario?.nombre?.ifBlank { null } ?: auth.currentUser?.displayName?.split(" ")?.firstOrNull() ?: "Usuario"
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    val userFullName: StateFlow<String> = userProfile
        .map { usuario ->
            val completo = if (usuario != null) "${usuario.nombre} ${usuario.apellidos}".trim() else ""
            completo.ifBlank { auth.currentUser?.displayName ?: "Usuario" }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "...")

    private val _isProfileLoading = MutableStateFlow(true)
    val isProfileLoading: StateFlow<Boolean> = _isProfileLoading.asStateFlow()

    private val _pesoActual = MutableStateFlow(0.0)
    val pesoActual: StateFlow<Double> = _pesoActual

    private val _historialPeso = MutableStateFlow<List<Pair<java.time.Instant, Double>>>(emptyList())
    val historialPeso: StateFlow<List<Pair<java.time.Instant, Double>>> = _historialPeso.asStateFlow()

    // Listener para detectar cambios en la autenticación y disparar la carga del perfil
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        if (firebaseAuth.currentUser != null) {
            fetchUserProfile()
        }
    }

    init {
        // Registramos el listener de sesión inmediatamente para capturar la sesión al abrir
        auth.addAuthStateListener(authStateListener)
        
        viewModelScope.launch {
            _pesoActual.value = repository.obtenerPesoActual()
            _historialPeso.value = repository.obtenerHistorialPeso()
            cargarDatosDelDiaInternal()
        }
    }

    override fun onCleared() {
        super.onCleared()
        auth.removeAuthStateListener(authStateListener)
    }

    fun cambiarFecha(dias: Long) {
        val nuevaFecha = _fechaSeleccionada.value.plusDays(dias)
        if (!nuevaFecha.isAfter(LocalDate.now())) {
            _fechaSeleccionada.value = nuevaFecha
            cargarDatosDelDia()
        }
    }

    fun seleccionarFecha(fecha: LocalDate) {
        if (!fecha.isAfter(LocalDate.now())) {
            _fechaSeleccionada.value = fecha
            cargarDatosDelDia()
        }
    }

    fun updateProfile(usuario: Usuario) {
        val email = auth.currentUser?.email?.lowercase()?.trim() ?: return
        viewModelScope.launch {
            try {
                // Sincronizamos usando el EMAIL como clave según la estructura vista
                db.collection("perfiles_detallados").document(email).set(usuario.copy(correo = email)).await()
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
                cargarDatosDelDiaInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarAgua(id: String) {
        viewModelScope.launch {
            try {
                repository.eliminarHidratacion(id)
                cargarDatosDelDiaInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun agregarComida(calorias: Double, proteinas: Double, carbohidratos: Double, grasas: Double, nombre: String = "Comida") {
        viewModelScope.launch {
            try {
                repository.agregarComida(calorias, proteinas, carbohidratos, grasas, nombre)
                cargarDatosDelDiaInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarComida(id: String) {
        viewModelScope.launch {
            try {
                repository.eliminarComida(id)
                cargarDatosDelDiaInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun eliminarUltimaAgua() {
        viewModelScope.launch {
            try {
                repository.eliminarUltimaHidratacion()
                cargarDatosDelDiaInternal()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun guardarPeso(kg: Double) {
        viewModelScope.launch {
            try {
                repository.agregarPeso(kg)
                _pesoActual.value = kg
                _historialPeso.value = repository.obtenerHistorialPeso()
                val email = auth.currentUser?.email?.lowercase()?.trim() ?: return@launch
                db.collection("perfiles_detallados").document(email).update("pesoKg", kg)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Recupera el perfil del usuario buscando el documento por su EMAIL.
     * Mantiene la información actual en pantalla y solo la sobreescribe
     * si los datos obtenidos de Firebase son válidos y no están vacíos.
     */
    fun fetchUserProfile() {
        val currentUser = auth.currentUser ?: return
        val email = currentUser.email?.lowercase()?.trim() ?: return
        
        viewModelScope.launch {
            _isProfileLoading.value = true
            
            try {
                // 1. Intentamos obtener el documento directamente por ID (Email)
                var doc = db.collection("perfiles_detallados").document(email).get().await()
                
                // 2. Si no existe por ID, buscamos por el campo 'correo' (Fallback)
                if (!doc.exists()) {
                    val query = db.collection("perfiles_detallados")
                        .whereEqualTo("correo", email)
                        .get()
                        .await()
                    if (!query.isEmpty) doc = query.documents[0]
                }

                if (doc.exists()) {
                    val usuario = doc.toObject(Usuario::class.java)
                    if (usuario != null) {
                        usuario.id = doc.id
                        _userProfile.value = usuario
                    }
                }
            } catch (e: Exception) {
                Log.e("AppHabitusViewModel", "Error fetching user profile", e)
            } finally {
                _isProfileLoading.value = false
            }
        }
    }

    fun cargarDatosDelDia() {
        viewModelScope.launch {
            cargarDatosDelDiaInternal()
        }
    }

    private suspend fun cargarDatosDelDiaInternal() {
        val fecha = _fechaSeleccionada.value
        val nuevaActividad = repository.obtenerActividadFisica(fecha)
        val nuevaNutricion = repository.obtenerNutricion(fecha)
        val nuevosSignos = repository.obtenerSignosVitales(fecha)
        val nuevoSueno = repository.leerSuenoDelDia(fecha)
        val nuevoHistorial = repository.obtenerVitalidadSemanal(fecha)

        _actividad.value = nuevaActividad
        _nutricion.value = nuevaNutricion
        _signosVitales.value = nuevosSignos
        _sueño.value = nuevoSueno
        _historialVitalidad.value = nuevoHistorial

        sincronizarDatosConFirebase(nuevaActividad, nuevaNutricion, nuevosSignos, nuevoSueno, fecha)
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
