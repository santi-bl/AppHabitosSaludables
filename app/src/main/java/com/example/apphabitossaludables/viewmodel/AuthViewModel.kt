package com.example.apphabitossaludables.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.apphabitossaludables.data.model.Usuario
import com.google.firebase.auth.FirebaseAuth
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
                val result = auth.signInWithEmailAndPassword(email, pass).await()
                _authState.value = AuthState.Success(result.user?.uid ?: "")
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error al iniciar sesión")
            }
        }
    }

    fun register(usuario: Usuario) {
        if (usuario.nombre.isBlank() || usuario.correo.isBlank() || usuario.contraseña.isBlank() || 
            usuario.pesoKg <= 0 || usuario.alturaCm <= 0) {
            _authState.value = AuthState.Error("Todos los campos son obligatorios")
            return
        }

        viewModelScope.launch {
            _authState.value = AuthState.Loading
            try {
                // Primero intentamos crear el usuario en Auth
                val result = auth.createUserWithEmailAndPassword(usuario.correo, usuario.contraseña).await()
                val uid = result.user?.uid ?: ""
                
                // Actualizar el perfil de Firebase Auth con el nombre
                val profileUpdates = com.google.firebase.auth.userProfileChangeRequest {
                    displayName = usuario.nombre
                }
                result.user?.updateProfile(profileUpdates)?.await()
                
                // Si Auth tiene éxito, guardamos en Firestore
                db.collection("perfiles_detallados").document(usuario.correo).set(usuario.copy(id = uid)).await()
                
                _authState.value = AuthState.Success(uid)
            } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
                // Si el usuario ya existe en Auth, intentamos loguearlo directamente
                try {
                    val loginResult = auth.signInWithEmailAndPassword(usuario.correo, usuario.contraseña).await()
                    _authState.value = AuthState.Success(loginResult.user?.uid ?: "")
                } catch (loginError: Exception) {
                    _authState.value = AuthState.Error("La cuenta ya existe y la contraseña es incorrecta.")
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error al registrar")
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
                auth.sendPasswordResetEmail(email).await()
                _authState.value = AuthState.Idle // O un estado de éxito específico
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error al enviar el email")
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
                    val email = user.email ?: ""
                    val doc = db.collection("perfiles_detallados").document(email).get().await()
                    if (!doc.exists()) {
                        val nuevoUsuario = Usuario(
                            id = user.uid,
                            nombre = user.displayName ?: "Usuario Google",
                            correo = email
                        )
                        db.collection("perfiles_detallados").document(email).set(nuevoUsuario).await()
                    }
                    _authState.value = AuthState.Success(user.uid)
                }
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.localizedMessage ?: "Error con Google")
            }
        }
    }
    
    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
