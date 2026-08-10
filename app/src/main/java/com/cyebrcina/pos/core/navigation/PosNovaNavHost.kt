package com.cyebrcina.pos.core.navigation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PosNovaNavHost(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AUTH_GRAPH_ROUTE) {
        authNavGraph(
            navController = navController,
            onAuthenticated = {
                navController.navigate(MAIN_GRAPH_ROUTE) {
                    popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
                }
            },
        )
        composable(MAIN_GRAPH_ROUTE) {
            MainGraphHost(
                windowSizeClass = windowSizeClass,
                onLoggedOut = {
                    navController.navigate(AUTH_GRAPH_ROUTE) {
                        popUpTo(MAIN_GRAPH_ROUTE) { inclusive = true }
                    }
                },
            )
        }
    }
}
