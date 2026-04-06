package com.example.apphabitossaludables.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphabitossaludables.data.repository.HealthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AppHabitusViewModel(
    private val repository: HealthRepository
) : ViewModel() {

    // StateFlow para cada dato — la UI los observa
    private val _pasos = MutableStateFlow(0L)
    val pasos: StateFlow<Long> = _pasos

    private val _pulsaciones = MutableStateFlow(0L)
    val pulsaciones: StateFlow<Long> = _pulsaciones

    private val _sueño = MutableStateFlow(0L)
    val sueño: StateFlow<Long> = _sueño

    private val _cargando = MutableStateFlow(false)
    val cargando: StateFlow<Boolean> = _cargando

    fun cargarDatosDelDia() {
        viewModelScope.launch {  // scope que sobrevive recomposiciones
            _cargando.value = true
            _pasos.value = repository.leerPasosDelDia()
            _pulsaciones.value = repository.leerPulsacionesDelDia()
            _sueño.value = repository.leerSuenoDelDia()
            _cargando.value = false
        }
    }
}