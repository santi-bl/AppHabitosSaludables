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
import kotlinx.coroutines.flow.combine
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

    val score: StateFlow<Int> = combine(
        _actividad, 
        _nutricion, 
        _sueño, 
        _userProfile
    ) { act, nut, sue, profile ->
        val objPasos = profile?.objetivoPasos?.toFloat() ?: 8000f
        val actScore = (act.pasos / objPasos).coerceIn(0f, 1f) * 40
        val sleepScore = ((sue?.duracionTotalMinutos ?: 0) / 450f).coerceIn(0f, 1f) * 40
        val nutScore = (nut.hidratacionLitros / 2.0).coerceIn(0.0, 1.0) * 20
        (actScore + sleepScore + nutScore).toInt()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

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

    /**
     * Listener para detectar cambios en la autenticación.
     * Centraliza la carga y limpieza de datos para asegurar que no se realicen
     * consultas (Firestore/Health Connect) sin una sesión activa.
     */
    private val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        val user = firebaseAuth.currentUser
        if (user != null) {
            // Usuario autenticado: Iniciamos carga secuencial de datos
            inicializarDatosUsuario()
        } else {
            // Usuario desconectado: Limpiamos todos los estados reactivos
            limpiarDatosUsuario()
        }
    }

    private fun inicializarDatosUsuario() {
        viewModelScope.launch {
            // 1. Cargamos el perfil (esto disparará a su vez la carga de datos de salud una vez obtenido)
            fetchUserProfile()
            
            // 2. Cargamos datos físicos básicos del repositorio local/Health Connect
            try {
                _pesoActual.value = repository.obtenerPesoActual()
                _historialPeso.value = repository.obtenerHistorialPeso()
                // Nota: cargarDatosDelDiaInternal() ya es llamado dentro de fetchUserProfile() 
                // cuando se recibe el objeto Usuario, pero lo llamamos aquí como fallback 
                // por si el perfil de Firestore tarda o no existe todavía.
                cargarDatosDelDiaInternal()
            } catch (e: Exception) {
                Log.e("AppHabitusViewModel", "Error en inicialización de salud", e)
            }
        }
    }

    private fun limpiarDatosUsuario() {
        _userProfile.value = null
        _actividad.value = ActividadFisica()
        _nutricion.value = Nutricion()
        _signosVitales.value = SignosVitales()
        _sueño.value = null
        _pesoActual.value = 0.0
        _historialPeso.value = emptyList()
        _historialVitalidad.value = emptyList()
    }

    init {
        // Registramos el listener de sesión inmediatamente.
        // Ninguna consulta a datos_salud o perfil ocurre antes de que el listener detecte al usuario.
        auth.addAuthStateListener(authStateListener)
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
        val currentUser = auth.currentUser ?: return
        val email = currentUser.email?.lowercase()?.trim() ?: return
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
        if (auth.currentUser == null) return
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
        if (auth.currentUser == null) return
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
        if (auth.currentUser == null) return
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
        if (auth.currentUser == null) return
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
        if (auth.currentUser == null) return
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
        val currentUser = auth.currentUser ?: return
        val email = currentUser.email?.lowercase()?.trim() ?: return
        viewModelScope.launch {
            try {
                repository.agregarPeso(kg)
                _pesoActual.value = kg
                _historialPeso.value = repository.obtenerHistorialPeso()
                
                db.collection("perfiles_detallados").document(email).update("pesoKg", kg)
                
                // Actualizamos las métricas del día inmediatamente ya que el peso influye en las calorías
                cargarDatosDelDiaInternal()
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
                        // Recalculamos datos del día por si el perfil (altura/objetivos) ha cambiado
                        cargarDatosDelDiaInternal()
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
        if (auth.currentUser == null) return
        viewModelScope.launch {
            cargarDatosDelDiaInternal()
        }
    }

    private suspend fun cargarDatosDelDiaInternal() {
        if (auth.currentUser == null) return
        val fecha = _fechaSeleccionada.value
        val nuevaActividad = repository.obtenerActividadFisica(fecha)
        val nuevaNutricion = repository.obtenerNutricion(fecha)
        val nuevosSignos = repository.obtenerSignosVitales(fecha)
        val nuevoSueno = repository.leerSuenoDelDia(fecha)
        val nuevoHistorial = repository.obtenerVitalidadSemanal(fecha)

        // Buscamos el peso más cercano a la fecha seleccionada en el historial (sin pasarnos de fecha)
        val pesoParaFecha = _historialPeso.value
            .filter { !it.first.atZone(java.time.ZoneId.systemDefault()).toLocalDate().isAfter(fecha) }
            .maxByOrNull { it.first }?.second
            ?: _pesoActual.value.takeIf { it > 0 }
            ?: _userProfile.value?.pesoKg
            ?: 0.0

        // Calculamos las calorías y distancia personalizadas
        val distanciaEstimada = calcularDistanciaEstimada(nuevaActividad.pasos, _userProfile.value)
        val actividadCalculada = nuevaActividad.copy(
            caloriasQuemadas = if (pesoParaFecha > 0) calcularCaloriasMET(nuevaActividad, pesoParaFecha) else nuevaActividad.caloriasQuemadas,
            // Si Health Connect devuelve 0 o una distancia sospechosamente baja, usamos la estimada por pasos y altura
            distanciaMetros = if (nuevaActividad.distanciaMetros < (distanciaEstimada * 0.1)) distanciaEstimada else nuevaActividad.distanciaMetros
        )

        _actividad.value = actividadCalculada
        _nutricion.value = nuevaNutricion
        _signosVitales.value = nuevosSignos
        _sueño.value = nuevoSueno
        _historialVitalidad.value = nuevoHistorial

        sincronizarDatosConFirebase(actividadCalculada, nuevaNutricion, nuevosSignos, nuevoSueno, fecha)
    }

    private fun calcularCaloriasMET(act: ActividadFisica, peso: Double): Double {
        // kcal/min = MET * 3.5 * Peso (kg) / 200
        var totalKcal = 0.0
        
        if (act.sesionesEjercicio.isNotEmpty()) {
            totalKcal = act.sesionesEjercicio.sumOf { sesion ->
                val met = when (sesion.tipo) {
                    "Correr" -> 8.0
                    "Caminar" -> 3.5
                    "Ciclismo" -> 7.5
                    "Natación" -> 7.0
                    "Pesas" -> 5.0
                    else -> 4.5
                }
                (met * 3.5 * peso / 200.0) * sesion.duracionMinutos
            }
        } else if (act.pasos > 0) {
            // Si no hay sesiones pero hay pasos, estimamos 100 pasos/minuto a un MET de 3.5 (caminar)
            val minutosEstimados = act.pasos / 100.0
            totalKcal = (3.5 * 3.5 * peso / 200.0) * minutosEstimados
        }
        
        return if (totalKcal > 0) totalKcal else act.caloriasQuemadas
    }

    private fun calcularDistanciaEstimada(pasos: Long, usuario: Usuario?): Double {
        if (pasos <= 0) return 0.0
        // Estimación de zancada: Altura * factor (0.415 para hombres, 0.413 para mujeres)
        val altura = if (usuario != null && usuario.alturaCm > 0) usuario.alturaCm.toDouble() else 170.0
        val factor = if (usuario?.genero?.lowercase()?.contains("mujer") == true || 
            usuario?.genero?.lowercase()?.contains("femenino") == true) 0.413 else 0.415
        
        val longitudZancadaMetros = (altura * factor) / 100.0
        return pasos * longitudZancadaMetros
    }

    private suspend fun sincronizarDatosConFirebase(
        act: ActividadFisica,
        nut: Nutricion,
        sig: SignosVitales,
        sue: Suenio?,
        fecha: LocalDate
    ) {
        val currentUser = auth.currentUser ?: return
        val uid = currentUser.uid
        val email = currentUser.email ?: ""
        
        // Usamos el objetivo personalizado del usuario o 8000 por defecto (consistente con UI)
        val objPasos = _userProfile.value?.objetivoPasos?.toFloat() ?: 8000f
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
