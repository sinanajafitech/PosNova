package com.cyebrcina.pos.printer

import com.cyebrcina.pos.core.util.asCurrency
import com.cyebrcina.pos.core.util.asDateTime
import com.cyebrcina.pos.core.util.toInstantOrNull
import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.ZReport
import com.cyebrcina.pos.printer.model.PrintAlign
import com.cyebrcina.pos.printer.model.PrintCommand
import com.cyebrcina.pos.printer.model.PrintDocument
import com.cyebrcina.pos.printer.model.PrintTextSize
import java.time.Instant

/**
 * Turns Fire Hut's `ReceiptData`/`KitchenTicketData`/`ZReport` (see openapi.yaml) into a
 * printer-agnostic [PrintDocument]. Font-size prefs from the API are numeric point sizes;
 * mapped to the nearest [PrintTextSize] bucket since the printer layer only exposes four steps.
 */
object ReceiptBuilder {

    fun buildCustomerReceipt(data: ReceiptData): PrintDocument = buildList {
        val prefs = data.prefs
        add(PrintCommand.Text(data.storeName, align = PrintAlign.CENTER, size = fontSizeOf(prefs?.headerFontSize ?: 24), bold = true))
        add(PrintCommand.FeedLines())
        add(PrintCommand.Row("Order", data.number, bold = true))
        data.createdAt.toInstantOrNull()?.let { add(PrintCommand.Row("Date", it.asDateTime())) }
        add(PrintCommand.Row("Type", data.typeLabel))
        if (prefs?.showCustomerName != false) add(PrintCommand.Row("Customer", data.customerName))
        if (prefs?.showPhone != false) data.customerPhone?.let { add(PrintCommand.Row("Phone", it)) }
        if (prefs?.showDeliveryAddress != false) {
            data.deliveryAddress?.let { address ->
                add(PrintCommand.Text("Deliver to:", size = fontSizeOf(prefs?.sectionTitleFontSize ?: 14), bold = true))
                add(PrintCommand.Text(address, size = fontSizeOf(prefs?.addressFontSize ?: 12)))
            }
        }
        add(PrintCommand.Divider)

        data.items.forEach { item ->
            add(PrintCommand.Text("${item.qty}x ${item.label}", size = fontSizeOf(prefs?.itemFontSize ?: 14), bold = true))
            add(PrintCommand.Row("", item.lineTotal.asCurrency()))
            item.addOns.forEach { add(PrintCommand.Text("  + $it", size = PrintTextSize.SMALL)) }
            item.removedIngredients.forEach { add(PrintCommand.Text("  – no $it", size = PrintTextSize.SMALL)) }
            item.notes?.let { add(PrintCommand.Text("  note: $it", size = PrintTextSize.SMALL)) }
        }
        add(PrintCommand.Divider)

        if (prefs?.showPriceBreakdown != false) {
            add(PrintCommand.Row("Subtotal", data.itemsSubtotal.asCurrency()))
            if (data.otherCharges != 0.0) add(PrintCommand.Row("Delivery/other", data.otherCharges.asCurrency()))
            if (data.discountAmount > 0) {
                add(PrintCommand.Row("Discount${data.couponCode?.let { " ($it)" } ?: ""}", "-${data.discountAmount.asCurrency()}"))
            }
        }
        add(PrintCommand.Row("TOTAL", data.total.asCurrency(), bold = true))

        if (prefs?.showPaymentLabel != false) {
            data.paymentLabel?.let { add(PrintCommand.Row("Paid via", it)) }
        }

        add(PrintCommand.FeedLines())
        if (prefs?.showThankYouFooter != false) {
            add(PrintCommand.Text("Thank you for your order!", align = PrintAlign.CENTER, bold = true, size = fontSizeOf(prefs?.footerFontSize ?: 12)))
        }
        add(PrintCommand.FeedLines(2))
        add(PrintCommand.Cut)
    }

    fun buildKitchenTicket(data: KitchenTicketData): PrintDocument = buildList {
        val prefs = data.prefs
        add(PrintCommand.Text(data.storeName, align = PrintAlign.CENTER, size = PrintTextSize.LARGE, bold = true))
        add(PrintCommand.Text("KITCHEN TICKET", align = PrintAlign.CENTER, bold = true))
        add(PrintCommand.FeedLines())
        add(PrintCommand.Row("Order", data.number, bold = true))
        data.createdAt.toInstantOrNull()?.let { add(PrintCommand.Row("Time", it.asDateTime())) }
        if (prefs?.showOrderType != false) add(PrintCommand.Row("Type", data.typeLabel))
        if (prefs?.showBranchName == true) data.branchName?.let { add(PrintCommand.Row("Branch", it)) }
        data.deliveryAddress?.let { add(PrintCommand.Text(it, size = PrintTextSize.SMALL)) }
        add(PrintCommand.Divider)

        data.items.forEach { item ->
            add(PrintCommand.Text("${item.qty}x ${item.label}", size = PrintTextSize.LARGE, bold = true))
            if (prefs?.showAddOns != false) item.addOns.forEach { add(PrintCommand.Text("  + $it")) }
            if (prefs?.showRemovedIngredients != false) item.removedIngredients.forEach { add(PrintCommand.Text("  – no $it")) }
            if (prefs?.showNotes != false) item.notes?.let { add(PrintCommand.Text("  note: $it", bold = true)) }
        }
        add(PrintCommand.FeedLines(2))
        add(PrintCommand.Cut)
    }

    fun buildZReport(report: ZReport): PrintDocument = buildList {
        add(PrintCommand.Text(report.storeName, align = PrintAlign.CENTER, size = PrintTextSize.LARGE, bold = true))
        add(PrintCommand.Text("Z-Report — ${report.date}", align = PrintAlign.CENTER, bold = true))
        add(PrintCommand.FeedLines())
        add(PrintCommand.Row("Orders", report.orderCount.toString()))
        add(PrintCommand.Row("Gross sales", report.grossSales.asCurrency()))
        add(PrintCommand.Row("Refunds", "-${report.refundsTotal.asCurrency()} (${report.refundsCount})"))
        add(PrintCommand.Row("Net sales", report.netSales.asCurrency(), bold = true))
        add(PrintCommand.Row("Avg. order", report.avgOrderValue.asCurrency()))
        add(PrintCommand.Divider)
        report.paymentBreakdown.forEach { add(PrintCommand.Row(it.provider, "${it.amount.asCurrency()} (${it.orders})")) }
        add(PrintCommand.Divider)
        add(PrintCommand.Row("VAT", report.vatAmount.asCurrency()))
        add(PrintCommand.Row("Net of VAT", report.netOfVat.asCurrency()))
        add(PrintCommand.FeedLines(2))
        add(PrintCommand.Cut)
    }

    fun buildTestPrint(storeName: String): PrintDocument = listOf(
        PrintCommand.Text(storeName, align = PrintAlign.CENTER, size = PrintTextSize.LARGE, bold = true),
        PrintCommand.Text("Printer test", align = PrintAlign.CENTER),
        PrintCommand.Row("Printed at", Instant.now().asDateTime()),
        PrintCommand.Divider,
        PrintCommand.Text("If you can read this, the printer connection is working."),
        PrintCommand.FeedLines(2),
        PrintCommand.Cut,
    )

    private fun fontSizeOf(points: Int): PrintTextSize = when {
        points <= 12 -> PrintTextSize.SMALL
        points <= 16 -> PrintTextSize.NORMAL
        points <= 22 -> PrintTextSize.LARGE
        else -> PrintTextSize.XLARGE
    }
}
