/**
 * @author Santiago Barandiarán Lasheras
 * @description Gestiona el flujo de navegación de toda la aplicación, incluyendo
 * la autenticación, el contenedor principal con navegación inferior y las pantallas de detalle.
 */
package com.example.apphabitossaludables.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.apphabitossaludables.data.repository.HealthRepository
import com.example.apphabitossaludables.data.repository.UserPreferencesRepository
import com.example.apphabitossaludables.ui.auth.ForgotPasswordScreen
import com.example.apphabitossaludables.ui.auth.LoginScreen
import com.example.apphabitossaludables.ui.auth.RegisterScreen
import com.example.apphabitossaludables.ui.settings.EditProfileScreen
import com.example.apphabitossaludables.ui.settings.PrivacyScreen
import com.example.apphabitossaludables.ui.settings.UserSettingsScreen
import com.example.apphabitossaludables.ui.user.*
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModelFactory
import com.google.firebase.auth.FirebaseAuth

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val context = LocalContext.current
    
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val startDest = if (currentUser != null) "main_content" else "login"
    
    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
    val healthRepository = remember { HealthRepository(healthConnectClient) }
    val preferencesRepository = remember { UserPreferencesRepository(context) }
    
    val healthViewModel: AppHabitusViewModel = viewModel(
        factory = AppHabitusViewModelFactory(healthRepository, preferencesRepository)
    )

    // Función auxiliar para evitar navegación accidental a pantallas vacías
    val onBackSafe = {
        if (nav.previousBackStackEntry != null) {
            nav.popBackStack()
        }
    }

    NavHost(navController = nav, startDestination = startDest) {
        composable("login") {
            LoginScreen(
                onLogin = { nav.navigate("main_content") { popUpTo("login") { inclusive = true } } },
                onNavigateToRegister = { nav.navigate("register") },
                onNavigateToForgotPassword = { nav.navigate("forgot_password") }
            )
        }
        composable("register") {
            RegisterScreen(
                onRegisterLogin = { userId, weight ->
                    if (weight > 0) healthViewModel.guardarPeso(weight)
                    nav.navigate("main_content") { popUpTo("register") { inclusive = true } }
                }
            )
        }
        composable("forgot_password") {
            ForgotPasswordScreen(onBack = onBackSafe)
        }
        
        composable("main_content") {
            MainContainer(healthViewModel, nav)
        }

        // Pantallas de detalle con navegación segura
        composable("weight") { WeightScreen(viewModel = healthViewModel, onBack = onBackSafe) }
        composable("exercise") { ActivityScreen(viewModel = healthViewModel, onBack = onBackSafe) }
        composable("dream") { DreamScreen(viewModel = healthViewModel, onBack = onBackSafe) }
        composable("food") { NutritionScreen(viewModel = healthViewModel, onBack = onBackSafe) }
        composable("vitals") { VitalsScreen(viewModel = healthViewModel, onBack = onBackSafe) }
        composable("edit_profile") { EditProfileScreen(viewModel = healthViewModel, onBack = onBackSafe) }
        composable("privacy") { PrivacyScreen(onBack = onBackSafe) }
    }
}

@Composable
fun MainContainer(viewModel: AppHabitusViewModel, rootNav: androidx.navigation.NavController) {
    val internalNav = rememberNavController()
    
    Scaffold(
        bottomBar = {
            NavigationBar {
                val navBackStackEntry by internalNav.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                
                val items = listOf(
                    NavigationItem("user", "Inicio", Icons.Default.Home),
                    NavigationItem("settings", "Cuenta", Icons.Default.Person)
                )
                
                items.forEach { item ->
                    NavigationBarItem(
                        icon = { Icon(item.icon, contentDescription = item.label) },
                        label = { Text(item.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                        onClick = {
                            internalNav.navigate(item.route) {
                                popUpTo(internalNav.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = internalNav,
            startDestination = "user",
            modifier = Modifier.padding(padding)
        ) {
            composable("user") {
                LaunchedEffect(Unit) {
                    viewModel.fetchUserProfile()
                    viewModel.cargarDatosDelDia()
                }
                UserScreen(
                    viewModel = viewModel,
                    onLogout = { rootNav.navigate("login") { popUpTo("main_content") { inclusive = true } } },
                    onFoodScreen = { rootNav.navigate("food") },
                    onExerciseScreen = { rootNav.navigate("exercise") },
                    onDreamScreen = { rootNav.navigate("dream") },
                    onVitalsScreen = { rootNav.navigate("vitals") },
                    onWeightScreen = { rootNav.navigate("weight") }
                )
            }
            composable("settings") {
                UserSettingsScreen(
                    viewModel = viewModel,
                    onLogout = { rootNav.navigate("login") { popUpTo("main_content") { inclusive = true } } },
                    onEditProfile = { rootNav.navigate("edit_profile") },
                    onPrivacy = { rootNav.navigate("privacy") }
                )
            }
        }
    }
}

data class NavigationItem(val route: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)
