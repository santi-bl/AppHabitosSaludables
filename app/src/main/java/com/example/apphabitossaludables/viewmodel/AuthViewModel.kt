/**
 * @author Santiago Barandiarán Lasheras
 * @description ViewModel responsable de la lógica de autenticación. Gestiona el registro,
 * inicio de sesión y recuperación de contraseña utilizando Firebase Auth y Firestore.
 */
package com.example.apphabitossaludables.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphabitossaludables.data.model.Usuario
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState

    sealed class AuthState {
        object Idle : AuthState()
        object Loading : AuthState()
        data class Success(val userId: String) : AuthState()
        data class Error(val message: String) : AuthState()
    }

    fun login(email: String, pass: String) {
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthState.Error("Por favor, rellena todos los campos")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val result = auth.signInWithEmailAndPassword(email.lowercase().trim(), pass).await()
                _authState.value = AuthState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(handleAuthError(e))
            }
        }
    }

    fun register(usuario: Usuario, pass: String) {
        if (usuario.nombre.isBlank() || usuario.apellidos.isBlank() || usuario.correo.isBlank() ||
            pass.isBlank() || usuario.pesoKg <= 0 || usuario.alturaCm <= 0 || usuario.edad <= 0) {
            _authState.value = AuthState.Error("Todos los campos marcados con * son obligatorios")
            return
        }

        if (pass.length < 6) {
            _authState.value = AuthState.Error("La contraseña debe tener al menos 6 caracteres")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val userEmail = usuario.correo.lowercase().trim()

                // 1. Creamos la cuenta en Firebase Auth PRIMERO.
                // Esto inicia sesión automáticamente y permite que las reglas de Firestore nos den acceso.
                val result = auth.createUserWithEmailAndPassword(userEmail, pass).await()
                val uid = result.user?.uid ?: ""

                // FIX: Bloque de Firestore separado para distinguir su error del de Auth.
                // Si falla aquí, sabemos que la cuenta Auth se creó bien pero el perfil no se guardó,
                // evitando así que el error de Firestore se confunda con "credenciales incorrectas".
                try {
                    // 2. Guardamos los datos en Firestore usando el email como ID del documento.
                    db.collection("perfiles_detallados")
                        .document(userEmail)
                        .set(usuario.copy(correo = userEmail, id = uid))
                        .await()

                    // 3. (Redundante tras incluir id en el set, pero se mantiene por compatibilidad)
                    db.collection("perfiles_detallados")
                        .document(userEmail)
                        .update("id", uid)
                        .await()

                } catch (firestoreEx: Exception) {
                    // La cuenta Auth se creó correctamente, pero Firestore rechazó la escritura.
                    // Causa más probable: reglas de Firestore demasiado restrictivas (allow write: if false).
                    // Solución: ajusta las reglas en la consola de Firebase para permitir
                    // escritura al usuario autenticado sobre su propio documento.
                    _authState.value = AuthState.Error(
                        "Cuenta creada pero error al guardar el perfil. " +
                        "Revisa las reglas de Firestore. (${firestoreEx.localizedMessage})"
                    )
                    return@launch
                }

                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = "${usuario.nombre} ${usuario.apellidos}".trim()
                }
                result.user?.updateProfile(profileUpdates)?.await()

                _authState.value = AuthState.Success(uid)

            } catch (e: Exception) {
                _authState.value = AuthState.Error(handleAuthError(e))
            }
        }
    }

    fun resetPassword(email: String) {
        if (email.isBlank()) {
            _authState.value = AuthState.Error("Introduce tu email")
            return
        }
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                auth.sendPasswordResetEmail(email.lowercase().trim()).await()
                _authState.value = AuthState.Idle
            } catch (e: Exception) {
                _authState.value = AuthState.Error(handleAuthError(e))
            }
        }
    }

    fun signInWithGoogle(idToken: String) {
        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                val credential = com.google.firebase.auth.GoogleAuthProvider.getCredential(idToken, null)
                val result = auth.signInWithCredential(credential).await()
                val user = result.user
                if (user != null) {
                    val email = user.email?.lowercase()?.trim() ?: ""
                    val doc = db.collection("perfiles_detallados").document(email).get().await()
                    if (!doc.exists()) {
                        val names = user.displayName?.split(" ") ?: emptyList()
                        val nuevoUsuario = Usuario(
                            id = user.uid,
                            nombre = names.getOrNull(0) ?: "Usuario",
                            apellidos = names.drop(1).joinToString(" "),
                            correo = email
                        )
                        db.collection("perfiles_detallados").document(email).set(nuevoUsuario).await()
                    }
                    _authState.value = AuthState.Success(user.uid)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(handleAuthError(e))
            }
        }
    }

    private fun handleAuthError(e: Exception): String {
        return when (e) {
            is FirebaseAuthInvalidUserException -> "No existe ninguna cuenta vinculada a este correo electrónico."
            is FirebaseAuthInvalidCredentialsException -> "Credenciales incorrectas. Revisa el correo y la contraseña."
            is FirebaseAuthUserCollisionException -> "Este correo electrónico ya está registrado. Prueba a iniciar sesión."
            is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado corta. Debe tener al menos 6 caracteres."
            is FirebaseNetworkException -> "Error de conexión. Revisa tu acceso a Internet."
            else -> e.localizedMessage ?: "Ha ocurrido un error inesperado."
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
