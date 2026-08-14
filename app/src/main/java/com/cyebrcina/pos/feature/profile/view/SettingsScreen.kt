package com.cyebrcina.pos.feature.profile.view

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cyebrcina.pos.core.components.AppCard
import com.cyebrcina.pos.core.components.AppTextField
import com.cyebrcina.pos.core.components.BadgeTone
import com.cyebrcina.pos.core.components.PosTopBar
import com.cyebrcina.pos.feature.order.create.FlowSecondaryButton
import com.cyebrcina.pos.core.components.StatusBadge
import com.cyebrcina.pos.core.theme.PosColors
import com.cyebrcina.pos.core.theme.PosNovaShapes
import com.cyebrcina.pos.core.theme.PosTextStyles
import com.cyebrcina.pos.core.theme.Spacing
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.local.CurrentStaff
import com.cyebrcina.pos.data.local.KitchenPrinterConnectionType
import com.cyebrcina.pos.data.local.KitchenPrinterSettings
import com.cyebrcina.pos.data.local.OfflineSyncStatus
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.payment.model.PaymentProvider
import com.cyebrcina.pos.payment.model.TerminalStatus
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
        Column(
            Modifier.fillMaxSize().padding(padding).padding(Spacing.sm).verticalScroll(rememberScrollState()),
        ) {
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
                        colors = SwitchDefaults.colors(checkedTrackColor = PosColors.Blue500),
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            StaffClockSection(
                currentStaff = state.currentStaff,
                isClocking = state.isClockingStaff,
                error = state.staffClockError,
                onClock = viewModel::clockStaff,
            )

            Spacer(Modifier.height(Spacing.lg))
            OfflineModeSection(status = state.offlineSync, onSync = viewModel::syncForOffline)

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
                    Spacer(Modifier.height(Spacing.xs))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        state.discoveredPrinters.forEach { printer ->
                            PrinterCard(
                                printer = printer,
                                selected = printer == state.selectedMainPrinter,
                                onClick = { viewModel.selectPrinter(printer) },
                            )
                        }
                    }
                }

                Spacer(Modifier.height(Spacing.xs))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                    FlowSecondaryButton(
                        text = "Test Print",
                        onClick = viewModel::testPrint,
                        loading = state.isTestPrinting,
                        modifier = Modifier.weight(1f),
                        leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) },
                    )
                    FlowSecondaryButton(text = "Open Cash Drawer", onClick = viewModel::openCashDrawer, modifier = Modifier.weight(1f))
                }
                state.testPrintError?.let { message ->
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(message, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            KitchenPrinterSection(
                settings = state.kitchenPrinter,
                isTestPrinting = state.isTestPrintingKitchen,
                testResult = state.kitchenTestResult,
                onSave = viewModel::setKitchenPrinter,
                onTestPrint = viewModel::testKitchenPrint,
            )

            Spacer(Modifier.height(Spacing.lg))
            CardTerminalSection(
                selectedProvider = state.terminalProvider,
                status = state.terminalStatus,
                isConnecting = state.isConnectingTerminal,
                error = state.terminalError,
                onConnect = viewModel::connectTerminal,
            )

            state.receiptPrefs?.let { prefs ->
                Spacer(Modifier.height(Spacing.lg))
                ReceiptSettingsSection(prefs)
            }

            Spacer(Modifier.height(Spacing.lg))
            FlowSecondaryButton(
                text = "Log Out",
                onClick = { viewModel.logout(); onLoggedOut() },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** One discovered printer as its own card — tinted/bordered in the brand color and marked with a
 * checkmark when it's the currently-selected main printer, plain otherwise. */
@Composable
private fun PrinterCard(printer: DiscoveredPrinter, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(PosNovaShapes.medium)
            .background(if (selected) PosColors.Blue50 else PosColors.Neutral3)
            .border(
                width = if (selected) 1.5.dp else 1.dp,
                color = if (selected) PosColors.Blue500 else PosColors.Neutral4,
                shape = PosNovaShapes.medium,
            )
            .clickable(onClick = onClick)
            .padding(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = when (printer.connection) {
                is PrinterConnection.Bluetooth -> Icons.Default.Bluetooth
                is PrinterConnection.Usb -> Icons.Default.Usb
                is PrinterConnection.Network -> Icons.Default.Wifi
                PrinterConnection.BuiltIn -> Icons.Default.Print
            },
            contentDescription = null,
            tint = if (selected) PosColors.Blue500 else PosColors.Neutral7,
            modifier = Modifier.padding(end = Spacing.xs),
        )
        Column(Modifier.weight(1f)) {
            Text(printer.name, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
            val detail = when (val conn = printer.connection) {
                is PrinterConnection.Bluetooth -> conn.address
                is PrinterConnection.Usb -> "USB Device ID: ${conn.deviceId}"
                is PrinterConnection.Network -> "${conn.host}:${conn.port}"
                PrinterConnection.BuiltIn -> "Internal Terminal Printer"
            }
            Text(detail, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
        }
        if (selected) {
            Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = PosColors.Blue500)
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

/**
 * PIN clock in/out for whoever's working this till — same StaffMember/PIN Admin's own
 * clock-in kiosk uses. Toggles: entering a PIN clocks that staff member in if they weren't
 * already, or out if they were (there's no separate name/username step, just like the kiosk).
 * The "on shift" name shown here is a local convenience label for order attribution
 * (Order.staffId); the real shift record lives server-side either way.
 */
@Composable
private fun StaffClockSection(
    currentStaff: CurrentStaff?,
    isClocking: Boolean,
    error: String?,
    onClock: (pin: String) -> Unit,
) {
    var pin by remember { mutableStateOf("") }

    Text("Staff", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            currentStaff?.let { "On shift: ${it.name}" } ?: "No staff clocked in on this till",
            style = PosTextStyles.bodyMediumSemibold,
            color = PosColors.Neutral12,
        )
        Spacer(Modifier.height(Spacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            AppTextField(
                value = pin,
                onValueChange = { pin = it.filter(Char::isDigit).take(8) },
                label = "PIN",
                isPassword = true,
                keyboardType = KeyboardType.NumberPassword,
                modifier = Modifier.weight(1f),
            )
            FlowSecondaryButton(
                text = "Clock In / Out",
                onClick = { onClock(pin); pin = "" },
                loading = isClocking,
                enabled = pin.length in 4..8,
            )
        }
        error?.let { message ->
            Spacer(Modifier.height(Spacing.xxs))
            Text(message, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
        }
    }
}

/**
 * A deliberate, complete offline-readiness pass, on top of the automatic caching
 * FireHutMenuRepositoryImpl/MenuCacheStore already do every time the menu refreshes in the
 * background. Tapping "Download for Offline Use" refreshes the menu AND forces every product/
 * category photo through the image loader once, so nothing shows as a broken image if the
 * connection drops before a given item was ever actually viewed on this till — see
 * OfflineSyncManager's doc comment for the full reasoning.
 */
@Composable
private fun OfflineModeSection(status: OfflineSyncStatus, onSync: () -> Unit) {
    Text("Offline Mode", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Download for Offline Use", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
                Text(
                    status.lastSyncedAt?.toInstantOrNull()?.let { "Last synced ${it.asDateTime()}" }
                        ?: "Never synced — download the menu so this till can keep taking orders offline.",
                    style = PosTextStyles.bodyXSmallRegular,
                    color = PosColors.Neutral7,
                )
            }
            if (status.isSyncing) {
                CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), color = PosColors.Blue500, strokeWidth = 2.dp)
            }
        }

        if (status.isSyncing && status.totalImages > 0) {
            Spacer(Modifier.height(Spacing.sm))
            LinearProgressIndicator(
                progress = { status.downloadedImages / status.totalImages.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = PosColors.Blue500,
            )
            Spacer(Modifier.height(Spacing.xxxs))
            Text(
                "Downloading photos: ${status.downloadedImages}/${status.totalImages}",
                style = PosTextStyles.bodyXSmallRegular,
                color = PosColors.Neutral7,
            )
        }

        status.error?.let { message ->
            Spacer(Modifier.height(Spacing.xxs))
            Text(message, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
        }

        Spacer(Modifier.height(Spacing.sm))
        FlowSecondaryButton(
            text = if (status.isSyncing) "Downloading…" else "Download for Offline Use",
            onClick = onSync,
            enabled = !status.isSyncing,
            loading = status.isSyncing,
            leadingIcon = { Icon(Icons.Filled.CloudDownload, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/**
 * A separate printer for kitchen tickets, over the network or a paired Bluetooth device — a
 * kitchen physically apart from the till/counter (the main [PrinterService] above) can have its
 * own printer, so a ticket doesn't depend on someone carrying it over. Network host/port are
 * entered manually since there's no reliable network-printer discovery to scan for; a Bluetooth
 * printer is instead picked from the device's already-paired list, the same source
 * [com.cyebrcina.pos.printer.imin.IminPrinterService.startDiscovery] uses for the main printer.
 */
@Composable
private fun KitchenPrinterSection(
    settings: KitchenPrinterSettings,
    isTestPrinting: Boolean,
    testResult: String?,
    onSave: (KitchenPrinterSettings) -> Unit,
    onTestPrint: () -> Unit,
) {
    var enabled by remember(settings.enabled) { mutableStateOf(settings.enabled) }
    var connectionType by remember(settings.connectionType) { mutableStateOf(settings.connectionType) }
    var host by remember(settings.host) { mutableStateOf(settings.host) }
    var port by remember(settings.port) { mutableStateOf(settings.port.toString()) }
    var bluetoothAddress by remember(settings.bluetoothAddress) { mutableStateOf(settings.bluetoothAddress) }
    var bluetoothName by remember(settings.bluetoothName) { mutableStateOf(settings.bluetoothName) }
    var showBluetoothPicker by remember { mutableStateOf(false) }

    fun currentSettings(overrideEnabled: Boolean = enabled) = KitchenPrinterSettings(
        enabled = overrideEnabled,
        name = settings.name,
        connectionType = connectionType,
        host = host,
        port = port.toIntOrNull() ?: settings.port,
        bluetoothAddress = bluetoothAddress,
        bluetoothName = bluetoothName,
    )

    Text("Kitchen Printer", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Send kitchen tickets here", style = PosTextStyles.bodyMediumSemibold, color = PosColors.Neutral12)
                Text(
                    "Instead of the main printer above — for a kitchen in a different room from the till",
                    style = PosTextStyles.bodyXSmallRegular,
                    color = PosColors.Neutral7,
                )
            }
            Switch(
                checked = enabled,
                onCheckedChange = { checked ->
                    enabled = checked
                    onSave(currentSettings(overrideEnabled = checked))
                },
                colors = SwitchDefaults.colors(checkedTrackColor = PosColors.Blue500),
            )
        }

        if (enabled) {
            Spacer(Modifier.height(Spacing.sm))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                ConnectionTypeChip(
                    label = "Network",
                    selected = connectionType == KitchenPrinterConnectionType.NETWORK,
                    onClick = { connectionType = KitchenPrinterConnectionType.NETWORK },
                )
                ConnectionTypeChip(
                    label = "Bluetooth",
                    selected = connectionType == KitchenPrinterConnectionType.BLUETOOTH,
                    onClick = { connectionType = KitchenPrinterConnectionType.BLUETOOTH },
                )
            }
            Spacer(Modifier.height(Spacing.sm))

            when (connectionType) {
                KitchenPrinterConnectionType.NETWORK -> {
                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        AppTextField(
                            value = host,
                            onValueChange = { host = it },
                            label = "Printer IP",
                            placeholder = "192.168.1.50",
                            modifier = Modifier.weight(1f),
                        )
                        AppTextField(
                            value = port,
                            onValueChange = { port = it.filter(Char::isDigit) },
                            label = "Port",
                            keyboardType = KeyboardType.Number,
                            modifier = Modifier.width(96.dp),
                        )
                    }
                }
                KitchenPrinterConnectionType.BLUETOOTH -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(PosNovaShapes.medium)
                            .background(PosColors.Neutral3)
                            .clickable { showBluetoothPicker = true }
                            .padding(Spacing.sm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Bluetooth,
                            contentDescription = null,
                            tint = PosColors.Neutral7,
                            modifier = Modifier.padding(end = Spacing.xs),
                        )
                        Column(Modifier.weight(1f)) {
                            Text(
                                bluetoothName.ifBlank { "No printer chosen" },
                                style = PosTextStyles.bodySmallMedium,
                                color = PosColors.Neutral12,
                            )
                            if (bluetoothAddress.isNotBlank()) {
                                Text(bluetoothAddress, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
                            }
                        }
                        Text("Choose", style = PosTextStyles.bodyXSmallSemibold, color = PosColors.Blue500)
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xs))
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                FlowSecondaryButton(
                    text = "Save",
                    onClick = { onSave(currentSettings()) },
                    modifier = Modifier.weight(1f),
                )
                FlowSecondaryButton(
                    text = "Test Print",
                    onClick = onTestPrint,
                    loading = isTestPrinting,
                    modifier = Modifier.weight(1f),
                    leadingIcon = { Icon(Icons.Filled.Print, contentDescription = null) },
                )
            }
            testResult?.let { message ->
                Spacer(Modifier.height(Spacing.xxs))
                Text(message, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Neutral7)
            }
        }
    }

    if (showBluetoothPicker) {
        BluetoothPrinterPickerDialog(
            onDismiss = { showBluetoothPicker = false },
            onSelected = { address, name ->
                bluetoothAddress = address
                bluetoothName = name
                showBluetoothPicker = false
                onSave(currentSettings().copy(bluetoothAddress = address, bluetoothName = name))
            },
        )
    }
}

@Composable
private fun ConnectionTypeChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(if (selected) PosColors.Blue500 else PosColors.Neutral3)
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.sm, vertical = Spacing.xxs),
    ) {
        Text(label, style = PosTextStyles.bodyXSmallSemibold, color = if (selected) PosColors.White else PosColors.Neutral12)
    }
}

@SuppressLint("MissingPermission")
private fun bondedBluetoothDevices(context: Context): List<BluetoothDevice> {
    val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
    return manager?.adapter?.bondedDevices?.toList().orEmpty()
}

/** Picks from the device's already-paired Bluetooth devices — same source as the main printer's
 * discovery, but scoped to just the kitchen printer's own setting rather than sharing
 * [com.cyebrcina.pos.printer.PrinterService]'s `discoveredPrinters`/connection state, so choosing
 * a kitchen printer here can never interfere with the main printer's own selection/connection. */
@Composable
private fun BluetoothPrinterPickerDialog(onDismiss: () -> Unit, onSelected: (address: String, name: String) -> Unit) {
    val context = LocalContext.current
    val devices = remember { bondedBluetoothDevices(context) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(20.dp), color = PosColors.White) {
            Column(Modifier.width(360.dp).padding(Spacing.lg)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Choose Bluetooth Printer", style = PosTextStyles.h6, color = PosColors.Neutral12)
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
                if (devices.isEmpty()) {
                    Text(
                        "No paired Bluetooth devices. Pair the printer in this device's system Bluetooth settings first.",
                        style = PosTextStyles.bodySmallRegular,
                        color = PosColors.Neutral7,
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
                        devices.forEach { device ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(PosNovaShapes.medium)
                                    .background(PosColors.Neutral3)
                                    .clickable { onSelected(device.address, device.name ?: "Unknown Device") }
                                    .padding(Spacing.sm),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(
                                    Icons.Default.Bluetooth,
                                    contentDescription = null,
                                    tint = PosColors.Neutral7,
                                    modifier = Modifier.padding(end = Spacing.xs),
                                )
                                Column(Modifier.weight(1f)) {
                                    Text(device.name ?: "Unknown Device", style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
                                    Text(device.address, style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Read-only — these font sizes and which sections print are set from Admin
 * (Settings → Devices/Receipts on the back office), not editable per-till. Shown here so a
 * cashier troubleshooting an odd-looking receipt can see what's actually configured without
 * needing back-office access.
 */
@Composable
private fun ReceiptSettingsSection(prefs: ReceiptPrefs) {
    Text("Receipt Settings (from Admin)", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("Font sizes", style = PosTextStyles.bodyXSmallMedium, color = PosColors.Neutral7)
        Spacer(Modifier.height(Spacing.xxxs))
        PrefRow("Header", "${prefs.headerFontSize}px")
        PrefRow("Order box", "${prefs.orderBoxFontSize}px")
        PrefRow("Section title", "${prefs.sectionTitleFontSize}px")
        PrefRow("Address", "${prefs.addressFontSize}px")
        PrefRow("Item", "${prefs.itemFontSize}px")
        PrefRow("Footer", "${prefs.footerFontSize}px")

        Spacer(Modifier.height(Spacing.sm))
        Text("Sections shown", style = PosTextStyles.bodyXSmallMedium, color = PosColors.Neutral7)
        Spacer(Modifier.height(Spacing.xxxs))
        PrefRow("Customer name", if (prefs.showCustomerName) "Shown" else "Hidden")
        PrefRow("Phone", if (prefs.showPhone) "Shown" else "Hidden")
        PrefRow("Delivery address", if (prefs.showDeliveryAddress) "Shown" else "Hidden")
        PrefRow("Payment label", if (prefs.showPaymentLabel) "Shown" else "Hidden")
        PrefRow("Price breakdown", if (prefs.showPriceBreakdown) "Shown" else "Hidden")
        PrefRow("Thank-you footer", if (prefs.showThankYouFooter) "Shown" else "Hidden")
    }
}

@Composable
private fun PrefRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = PosTextStyles.bodySmallRegular, color = PosColors.Neutral12)
        Text(value, style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral7)
    }
}

private fun PaymentProvider.displayName(): String = when (this) {
    PaymentProvider.MOCK -> "Mock (testing, no hardware)"
    PaymentProvider.STRIPE_TERMINAL -> "Stripe Terminal"
    PaymentProvider.SUMUP -> "SumUp (Bluetooth)"
    PaymentProvider.FLATPAY -> "Flatpay"
    PaymentProvider.DOJO -> "Dojo"
    PaymentProvider.TEYA -> "Teya"
}

/**
 * Read-only — which provider is active is set from Admin -> Settings -> Card Terminal, not
 * chosen on the till (an earlier on-device picker meant browsing this screen could silently
 * switch it to an unconfigured provider mid-shift). "Connect" just retries connecting to
 * whatever Admin currently has selected. SumUp's own checkout SDK handles Bluetooth reader
 * pairing itself when a charge is started; the other real providers are still "not configured
 * yet" scaffolds pending real SDK/API details — see CARD_PAYMENT_SETUP.md.
 */
@Composable
private fun CardTerminalSection(
    selectedProvider: PaymentProvider,
    status: TerminalStatus,
    isConnecting: Boolean,
    error: String?,
    onConnect: () -> Unit,
) {
    Text("Card Terminal", style = PosTextStyles.h6, color = PosColors.Neutral12)
    Spacer(Modifier.height(Spacing.xxs))
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(PosNovaShapes.medium)
                .background(PosColors.Blue50)
                .padding(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.CreditCard, contentDescription = null, tint = PosColors.Blue500, modifier = Modifier.padding(end = Spacing.xs))
            Column(Modifier.weight(1f)) {
                Text(selectedProvider.displayName(), style = PosTextStyles.bodySmallMedium, color = PosColors.Neutral12)
                Text("Set from Admin → Settings → Card Terminal", style = PosTextStyles.bodyXSmallRegular, color = PosColors.Neutral7)
            }
            TerminalStatusBadge(status)
        }
        Spacer(Modifier.height(Spacing.sm))
        FlowSecondaryButton(
            text = "Connect",
            onClick = onConnect,
            loading = isConnecting,
            modifier = Modifier.fillMaxWidth(),
        )
        error?.let { message ->
            Spacer(Modifier.height(Spacing.xxs))
            Text(message, style = PosTextStyles.bodyXSmallMedium, color = PosColors.Warning500)
        }
    }
}

@Composable
private fun TerminalStatusBadge(status: TerminalStatus) {
    val (label, tone) = when (status) {
        TerminalStatus.READY -> "Connected" to BadgeTone.SUCCESS
        TerminalStatus.CONNECTING -> "Connecting…" to BadgeTone.PENDING
        TerminalStatus.AWAITING_CARD -> "Awaiting card" to BadgeTone.PENDING
        TerminalStatus.PROCESSING -> "Processing" to BadgeTone.PENDING
        TerminalStatus.DISCONNECTED -> "Disconnected" to BadgeTone.NEUTRAL
        TerminalStatus.ERROR -> "Error" to BadgeTone.WARNING
    }
    StatusBadge(text = label, tone = tone)
}
