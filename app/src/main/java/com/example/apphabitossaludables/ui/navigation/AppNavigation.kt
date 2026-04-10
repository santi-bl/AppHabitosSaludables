package com.example.apphabitossaludables.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.apphabitossaludables.data.repository.HealthRepository
import com.example.apphabitossaludables.ui.auth.LoginScreen
import com.example.apphabitossaludables.ui.auth.RegiesterScreen
import com.example.apphabitossaludables.ui.user.ActivityScreen
import com.example.apphabitossaludables.ui.user.DreamScreen
import com.example.apphabitossaludables.ui.user.NutritionScreen
import com.example.apphabitossaludables.ui.user.UserScreen
import com.example.apphabitossaludables.ui.user.VitalsScreen
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModel
import com.example.apphabitossaludables.viewmodel.AppHabitusViewModelFactory

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
    val context = LocalContext.current
    
    // Compartimos el repositorio y el viewModel entre las pantallas de salud
    val healthConnectClient = remember { HealthConnectClient.getOrCreate(context) }
    val repository = remember { HealthRepository(healthConnectClient) }
    val healthViewModel: AppHabitusViewModel = viewModel(
        factory = AppHabitusViewModelFactory(repository)
    )

    NavHost(navController = nav, startDestination = "login") {
        composable("login") {
            LoginScreen(
                onLogin = { username ->
                    nav.navigate("user") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onNavigateToRegister = { nav.navigate("register") })
        }
        composable("register") {
            RegiesterScreen(
                onRegister_Login = { username ->
                    nav.navigate("user/$username") {
                        popUpTo("register") { inclusive = true }
                    }
                }
            )
        }
        composable("user") {
            UserScreen(
                viewModel = healthViewModel,
                onLogout = {
                    nav.navigate("login") {
                        popUpTo("user") { inclusive = true }
                    }
                },
                onFoodScreen = { nav.navigate("food") },
                onExerciseScreen = { nav.navigate("exercise") },
                onDreamScreen = { nav.navigate("dream") },
                onVitalsScreen = { nav.navigate("vitals") }
            )
        }
        composable("exercise") {
            ActivityScreen(viewModel = healthViewModel, onBack = { nav.popBackStack() })
        }
        composable("dream") {
            DreamScreen(viewModel = healthViewModel, onBack = { nav.popBackStack() })
        }
        composable("food") {
            NutritionScreen(viewModel = healthViewModel, onBack = { nav.popBackStack() })
        }
        composable("vitals") {
            VitalsScreen(viewModel = healthViewModel, onBack = { nav.popBackStack() })
        }
    }
}
