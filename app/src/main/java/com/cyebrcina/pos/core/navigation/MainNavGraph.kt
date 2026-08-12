package com.cyebrcina.pos.core.navigation

import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.cyebrcina.pos.feature.menu.MenuScreen
import com.cyebrcina.pos.feature.order.create.CheckoutScreen
import com.cyebrcina.pos.feature.order.create.NewOrderScreen
import com.cyebrcina.pos.feature.order.create.NewOrderViewModel
import com.cyebrcina.pos.feature.order.create.OrderSuccessScreen
import com.cyebrcina.pos.feature.order.create.TablesScreen
import com.cyebrcina.pos.feature.order.detail.OrderDetailScreen
import com.cyebrcina.pos.feature.order.history.OrderHistoryScreen
import com.cyebrcina.pos.feature.order.list.OrderQueueScreen
import com.cyebrcina.pos.feature.profile.view.SettingsScreen
import com.cyebrcina.pos.feature.report.ReportScreen
import com.cyebrcina.pos.feature.incomingcall.IncomingCallOverlay
import com.cyebrcina.pos.feature.waitercall.WaiterCallOverlay
import androidx.hilt.navigation.compose.hiltViewModel

const val MAIN_GRAPH_ROUTE = "main_root"

/**
 * Hosts its own inner [androidx.navigation.NavController] + [MainScaffold] so that switching
 * between Orders/History/Menu/Report/Settings keeps the adaptive nav rail/bottom-bar mounted
 * while only the content area navigates.
 */
@OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun MainGraphHost(windowSizeClass: WindowSizeClass, onLoggedOut: () -> Unit) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentSection = MainSection.entries.firstOrNull { section -> currentRoute?.startsWith(section.route) == true }
        ?: MainSection.QUEUE

    // Mounted alongside MainScaffold (not inside its content slot) so it pops up over whichever
    // tab staff are on, and survives tab switches since it's scoped to this composable's own
    // lifetime rather than any one screen's.
    WaiterCallOverlay()
    IncomingCallOverlay(onTakeOrder = { navController.navigate(MainRoutes.NEW_ORDER_GRAPH) })

    MainScaffold(
        windowSizeClass = windowSizeClass,
        currentSection = currentSection,
        onSectionSelected = { section ->
            navController.navigate(section.route) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        },
    ) { contentModifier ->
        NavHost(navController = navController, startDestination = MainRoutes.ORDER_QUEUE, modifier = contentModifier) {
            composable(MainRoutes.ORDER_QUEUE) {
                OrderQueueScreen(
                    onOpenOrder = { orderId -> navController.navigate(MainRoutes.orderDetail(orderId)) },
                    onAddNewOrder = { navController.navigate(MainRoutes.NEW_ORDER_GRAPH) },
                )
            }
            composable(
                route = MainRoutes.ORDER_DETAIL,
                arguments = listOf(navArgument("orderId") { type = NavType.StringType }),
            ) {
                OrderDetailScreen(
                    onBack = { navController.popBackStack() },
                    onActioned = { navController.popBackStack() },
                )
            }
            composable(MainRoutes.ORDER_HISTORY) {
                OrderHistoryScreen(onOpenOrder = { orderId -> navController.navigate(MainRoutes.orderDetail(orderId)) })
            }
            composable(MainRoutes.MENU) { MenuScreen() }
            composable(MainRoutes.REPORT) { ReportScreen() }
            composable(MainRoutes.SETTINGS) { SettingsScreen(onLoggedOut = onLoggedOut) }
            composable(MainRoutes.TABLES) {
                TablesScreen(
                    onNavigateToNewOrder = {
                        navController.navigate(MainRoutes.NEW_ORDER_GRAPH)
                    },
                )
            }

            navigation(startDestination = NewOrderRoutes.MENU, route = MainRoutes.NEW_ORDER_GRAPH) {
                composable(NewOrderRoutes.MENU) { entry ->
                    val parentEntry = remember(entry) { navController.getBackStackEntry(MainRoutes.NEW_ORDER_GRAPH) }
                    val viewModel: NewOrderViewModel = hiltViewModel(parentEntry)
                    NewOrderScreen(
                        viewModel = viewModel,
                        onCheckout = { navController.navigate(NewOrderRoutes.CHECKOUT) },
                        onExitFlow = {
                            navController.navigate(MainRoutes.ORDER_QUEUE) {
                                popUpTo(MainRoutes.NEW_ORDER_GRAPH) { inclusive = true }
                            }
                        },
                        onHeld = {
                            navController.navigate(MainRoutes.TABLES) {
                                popUpTo(MainRoutes.NEW_ORDER_GRAPH) { inclusive = true }
                            }
                        },
                    )
                }
                composable(NewOrderRoutes.CHECKOUT) { entry ->
                    val parentEntry = remember(entry) { navController.getBackStackEntry(MainRoutes.NEW_ORDER_GRAPH) }
                    val viewModel: NewOrderViewModel = hiltViewModel(parentEntry)
                    CheckoutScreen(
                        viewModel = viewModel,
                        onBack = { navController.popBackStack() },
                        onOrderComplete = {
                            navController.navigate(NewOrderRoutes.SUCCESS) {
                                popUpTo(NewOrderRoutes.MENU) { inclusive = false }
                            }
                        },
                    )
                }
                composable(NewOrderRoutes.SUCCESS) { entry ->
                    val parentEntry = remember(entry) { navController.getBackStackEntry(MainRoutes.NEW_ORDER_GRAPH) }
                    val viewModel: NewOrderViewModel = hiltViewModel(parentEntry)
                    OrderSuccessScreen(
                        viewModel = viewModel,
                        onNewOrder = { navController.popBackStack(NewOrderRoutes.MENU, inclusive = false) },
                    )
                }
            }
        }
    }
}
