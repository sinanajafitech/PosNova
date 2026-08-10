package com.cyebrcina.pos.feature.profile.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.core.components.SecondaryButton
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.printer.model.DiscoveredPrinter
import com.cyebrcina.pos.printer.model.PrinterConnection
import com.cyebrcina.pos.printer.model.PrinterStatus

@Composable
fun SettingsScreen(
    onLoggedOut: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(topBar = { PosTopBar(title = "Settings") }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(Spacing.sm)) {
            state.session?.let { session ->
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Text(session.storeName.ifBlank { "Fire Hut Pizza & Wraps" }, style = PosTextStyles.h6, color = PosColors.Neutral12)
                    Spacer(Modifier.height(Spacing.xxxs))
                    Text("Device: ${session.deviceName.ifBlank { session.deviceId }}", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Accepting Orders", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
                        Text(
                            if (state.acceptingOrders == true) "New orders can come in" else "New orders are paused",
                            style = PosTextStyles.bodyXSmallRegular,
                            color = PosColors.Neutral7,
                        )
                    }
                    Switch(
                        checked = state.acceptingOrders == true,
                        onCheckedChange = { viewModel.toggleAcceptingOrders() },
                        enabled = !state.isTogglingStatus && state.acceptingOrders != null,
                        colors = SwitchDefaults.colors(checkedTrackColor = PosColors.Primary500),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Printer", style = PosTextStyles.h6, color = PosColors.Neutral12)
                IconButton(onClick = viewModel::startPrinterDiscovery) {
                    Icon(Icons.Default.Refresh, contentDescription = "Refresh printers")
                }
            }
            Spacer(Modifier.height(Spacing.xxs))
            AppCard(modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Connection", style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral7)
                    PrinterStatusBadge(state.printerStatus)
                }

                if (state.discoveredPrinters.isNotEmpty()) {
                    Spacer(Modifier.height(Spacing.sm))
                    Text("Discovered Devices", style = PosTextStyles.bodyXSmallMedium, color = PosColors.Neutral7)
                    Spacer(Modifier.height(Spacing.xxs))
                    Column(Modifier.fillMaxWidth()) {
                        state.discoveredPrinters.forEach { printer ->
                            PrinterItem(printer = printer, onClick = { viewModel.selectPrinter(printer) })
                            HorizontalDivider(color = PosColors.Neutral2)
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    SecondaryButton(
                        text = "Test Print",
                        onClick = viewModel::testPrint,
                        loading = state.isTestPrinting,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) },
                    )
                    SecondaryButton(text = "Open Cash Drawer", onClick = viewModel::openCashDrawer, modifier = Modifier.weight(1f))
                }
                state.testPrintError?.let { message ->
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(message, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
                }
            }

            Spacer(Modifier.weight(1f))
            SecondaryButton(
                text = "Log Out",
                onClick = { viewModel.logout(); onLoggedOut() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

@Composable
private fun PrinterItem(printer: DiscoveredPrinter, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (printer.connection) {
                is PrinterConnection.Bluetooth -> Icons.Default.Bluetooth
                is PrinterConnection.Usb -> Icons.Default.Usb
                PrinterConnection.BuiltIn -> Icons.Default.Print
            },
            contentDescription = null,
            tint = PosColors.Neutral7,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Column {
            Text(printer.name, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
            val detail = when (val conn = printer.connection) {
                is PrinterConnection.Bluetooth -> conn.address
                is PrinterConnection.Usb -> "USB Device ID: ${conn.deviceId}"
                PrinterConnection.BuiltIn -> "Internal Terminal Printer"
            }
            Text(detail, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
        }
    }
}

@Composable
private fun PrinterStatusBadge(status: PrinterStatus) {
    val (label, tone) = when (status) {
        PrinterStatus.READY -> "Connected" to BadgeTone.SUCCESS
        PrinterStatus.BUSY -> "Printing" to BadgeTone.PENDING
        PrinterStatus.NO_PAPER -> "Out of paper" to BadgeTone.WARNING
        PrinterStatus.DISCONNECTED -> "Disconnected" to BadgeTone.NEUTRAL
        PrinterStatus.ERROR -> "Error" to BadgeTone.WARNING
        PrinterStatus.UNKNOWN -> "Unknown" to BadgeTone.NEUTRAL
    }
    StatusBadge(text = label, tone = tone)
}
