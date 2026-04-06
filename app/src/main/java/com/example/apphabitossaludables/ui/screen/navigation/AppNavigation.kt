package com.example.apphabitossaludables.ui.screen.navigation

import androidx.compose.runtime.Composable
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.apphabitossaludables.ui.screen.auth.LoginScreen
import com.example.apphabitossaludables.ui.screen.auth.RegiesterScreen
import com.example.apphabitossaludables.ui.screen.user.UserScreen

@Composable
fun AppNavigation() {
    val nav = rememberNavController()
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
                onLogout = { ->
                    nav.navigate("login") {
                        popUpTo("user") { inclusive = true }
                    }
                },
                onFoodScreen = { ->
                    nav.navigate("food") {
                        popUpTo("user") { inclusive = true }
                    }
                }, onExerciseScreen = { ->
                    nav.navigate("exercise") {
                        popUpTo("user") { inclusive = true }
                    }
                }, onDreamScreen = { ->
                    nav.navigate("dream") {
                        popUpTo("user") { inclusive = true }
                    }
                }
            )
        }
    }
}