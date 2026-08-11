package com.cyebrcina.pos.core.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.QueryStats
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TableRestaurant
import androidx.compose.ui.graphics.vector.ImageVector

/** Auth graph route names. */
object AuthRoutes {
    const val SPLASH = "auth/splash"
    const val LOGIN = "auth/login"
}

/** Main (post-auth) graph route names. */
object MainRoutes {
    const val ORDER_QUEUE = "main/queue"
    const val ORDER_DETAIL = "main/queue/{orderId}"

    const val ORDER_HISTORY = "main/history"

    const val MENU = "main/menu"

    const val REPORT = "main/report"

    const val SETTINGS = "main/settings"

    const val TABLES = "main/tables"

    /** Nested graph for the till order-taking flow — see [com.cyebrcina.pos.core.navigation.NewOrderRoutes]. */
    const val NEW_ORDER_GRAPH = "main/new-order"

    fun orderDetail(orderId: String) = "main/queue/$orderId"
}

/** Routes inside the [MainRoutes.NEW_ORDER_GRAPH] nested graph — share one [com.cyebrcina.pos.feature.order.create.NewOrderViewModel]. */
object NewOrderRoutes {
    const val MENU = "main/new-order/menu"
    const val CHECKOUT = "main/new-order/checkout"
    const val SUCCESS = "main/new-order/success"
}

/** The top-level sections shown in the adaptive nav rail / bottom bar. */
enum class MainSection(val route: String, val label: String, val icon: ImageVector) {
    QUEUE(MainRoutes.ORDER_QUEUE, "Dashboard", Icons.AutoMirrored.Filled.ReceiptLong),
    NEW_ORDER(MainRoutes.NEW_ORDER_GRAPH, "New Order", Icons.Filled.AddCircle),
    TABLES(MainRoutes.TABLES, "Tables", Icons.Filled.TableRestaurant),
    HISTORY(MainRoutes.ORDER_HISTORY, "Transactions", Icons.Filled.History),
    MENU(MainRoutes.MENU, "Menu", Icons.Filled.RestaurantMenu),
    REPORT(MainRoutes.REPORT, "Report", Icons.Filled.QueryStats),
    SETTINGS(MainRoutes.SETTINGS, "Settings", Icons.Filled.Settings),
}
