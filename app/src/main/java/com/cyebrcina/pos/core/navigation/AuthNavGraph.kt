package com.cyebrcina.pos.core.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.composable
import androidx.navigation.navigation
import com.cyebrcina.pos.feature.auth.login.LoginScreen
import com.cyebrcina.pos.feature.splash.SplashScreen

const val AUTH_GRAPH_ROUTE = "auth"

fun NavGraphBuilder.authNavGraph(navController: NavHostController, onAuthenticated: () -> Unit) {
    navigation(startDestination = AuthRoutes.SPLASH, route = AUTH_GRAPH_ROUTE) {
        composable(AuthRoutes.SPLASH) {
            SplashScreen(
                onNavigateToQueue = onAuthenticated,
                onNavigateToLogin = {
                    navController.navigate(AuthRoutes.LOGIN) { popUpTo(AuthRoutes.SPLASH) { inclusive = true } }
                },
            )
        }
        composable(AuthRoutes.LOGIN) {
            LoginScreen(onLoggedIn = onAuthenticated)
        }
    }
}
