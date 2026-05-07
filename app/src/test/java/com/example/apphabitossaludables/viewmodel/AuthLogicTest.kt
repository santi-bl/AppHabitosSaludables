package com.example.apphabitossaludables.viewmodel

import com.example.apphabitossaludables.data.model.Usuario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AuthLogicTest {

    @Test
    fun `registration validation fails with weak password`() {
        val passwordCorta = "123"
        val esValida = passwordCorta.length >= 6
        assertFalse("La contraseña de 3 caracteres debería ser inválida", esValida)
    }

    @Test
    fun `user profile validation detects invalid numbers`() {
        val usuarioConPesoCero = Usuario(pesoKg = 0.0, alturaCm = 170)
        val esValido = usuarioConPesoCero.pesoKg > 0 && usuarioConPesoCero.alturaCm > 0
        assertFalse("Un usuario con peso 0 no debe ser válido", esValido)
    }

    @Test
    fun `email cleaning removes spaces and converts to lowercase`() {
        val emailEntrada = "  Santi.Dev@Gmail.com  "
        val emailProcesado = emailEntrada.lowercase().trim()
        assertEquals("santi.dev@gmail.com", emailProcesado)
    }
}
