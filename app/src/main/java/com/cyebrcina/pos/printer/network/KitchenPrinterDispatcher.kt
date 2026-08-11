package com.cyebrcina.pos.printer.network

import com.cyebrcina.pos.data.local.PrinterSettingsStore
import com.cyebrcina.pos.printer.escpos.EscPosEncoder
import com.cyebrcina.pos.printer.escpos.EscPosRasterEncoder
import com.cyebrcina.pos.printer.graphic.ReceiptBitmapRenderer
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrintMode
import com.cyebrcina.pos.printer.model.PrinterPaperSize
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Sends a kitchen ticket to the dedicated network kitchen printer configured in Settings, if one
 * is enabled — independent of whichever printer is selected as the main
 * [com.cyebrcina.pos.printer.PrinterService] for customer receipts, so a kitchen physically
 * apart from the till/counter can have its own printer without affecting where receipts print.
 */
@Singleton
class KitchenPrinterDispatcher @Inject constructor(
    private val settingsStore: PrinterSettingsStore,
    private val networkPrinterClient: NetworkPrinterClient,
) {
    /**
     * @return `null` if no kitchen printer is configured (caller should fall back to the main
     * printer), otherwise whether the send to the kitchen printer succeeded.
     */
    suspend fun printIfConfigured(
        document: PrintDocument,
        paperSize: PrinterPaperSize,
        printMode: PrintMode = PrintMode.ESC,
    ): Boolean? {
        val settings = settingsStore.kitchenPrinter.first()
        if (!settings.enabled || settings.host.isBlank()) return null
        val bytes = if (printMode == PrintMode.POS) {
            EscPosRasterEncoder.encode(ReceiptBitmapRenderer.render(document, paperSize))
        } else {
            EscPosEncoder.encode(document, paperSize)
        }
        return networkPrinterClient.send(settings.host, settings.port, bytes).isSuccess
    }
}
