package com.cyebrcina.pos.core.navigation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.cyebrcina.pos.feature.staffselect.SelectStaffScreen

const val SELECT_STAFF_ROUTE = "select_staff"

@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun PosNovaNavHost(windowSizeClass: WindowSizeClass) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = AUTH_GRAPH_ROUTE) {
        authNavGraph(
            navController = navController,
            // Both a fresh login and a silent splash re-auth land here first — staff pick who's
            // running this till before the dashboard is reachable (see SelectStaffScreen).
            onAuthenticated = {
                navController.navigate(SELECT_STAFF_ROUTE) {
                    popUpTo(AUTH_GRAPH_ROUTE) { inclusive = true }
                }
            },
        )
        composable(SELECT_STAFF_ROUTE) {
            SelectStaffScreen(
                onStaffConfirmed = {
                    navController.navigate(MAIN_GRAPH_ROUTE) {
                        popUpTo(SELECT_STAFF_ROUTE) { inclusive = true }
                    }
                },
            )
        }
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
