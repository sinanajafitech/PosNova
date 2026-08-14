package com.cyebrcina.pos.feature.order.create

import com.cyebrcina.pos.data.remote.model.KitchenTicketData
import com.cyebrcina.pos.data.remote.model.KitchenTicketItem
import com.cyebrcina.pos.data.remote.model.KitchenTicketPrefs
import com.cyebrcina.pos.data.remote.model.ReceiptData
import com.cyebrcina.pos.data.remote.model.ReceiptItem
import com.cyebrcina.pos.data.remote.model.ReceiptPrefs
import com.cyebrcina.pos.printer.ReceiptBuilder
import com.cyebrcina.pos.printer.model.PrintDocument
import java.time.Instant

/**
 * Builds a customer receipt + kitchen ticket entirely from what's already on the till — no
 * `receiptData(orderId)`/`ticketData(orderId)` server round-trip — for an order that's just been
 * queued locally instead of submitted (see NewOrderViewModel.submitOrder() /
 * OrderRepository.CreateOrderResult.Queued). The kitchen can't wait for a connection to come
 * back before it knows what to cook, so this prints immediately, using the exact same
 * [ReceiptBuilder] layout logic as a normal order — only how [ReceiptData]/[KitchenTicketData]
 * get built differs (from a live [CartItem] list instead of server-computed data).
 *
 * Deliberately missing here vs. a real order: any server-side pricing rule beyond a flat
 * cart-item subtotal (there are none for a till order today — see NewOrderViewModel — so this is
 * not actually a simplification in practice), and `removedIngredients` (a website-order-only
 * concept `CartItem` has no field for).
 */
object OfflineReceiptBuilder {

    /** @return (customer receipt, kitchen ticket) */
    fun build(
        localOrderLabel: String,
        storeName: String,
        typeLabel: String,
        customerName: String,
        customerPhone: String?,
        cart: List<CartItem>,
        itemsSubtotal: Double,
        tipAmount: Double,
        paymentLabel: String,
        receiptPrefs: ReceiptPrefs?,
        kitchenTicketPrefs: KitchenTicketPrefs?,
    ): Pair<PrintDocument, PrintDocument> {
        val now = Instant.now().toString()

        fun itemLabel(item: CartItem) = item.product.name + (item.size?.let { " (${it.label})" } ?: "")

        val receiptData = ReceiptData(
            number = localOrderLabel,
            storeName = storeName,
            createdAt = now,
            typeLabel = typeLabel,
            customerName = customerName,
            customerPhone = customerPhone,
            items = cart.map { item ->
                ReceiptItem(
                    label = itemLabel(item),
                    qty = item.quantity,
                    lineTotal = item.lineTotal,
                    addOns = item.addOns.map { it.name },
                    notes = item.notes,
                )
            },
            itemsSubtotal = itemsSubtotal,
            otherCharges = 0.0,
            total = itemsSubtotal + tipAmount,
            // Flags this receipt as provisional wherever the real payment label would show —
            // staff/customer both need to know it hasn't reached Fire Hut's system yet.
            paymentLabel = "$paymentLabel — OFFLINE, pending sync",
            prefs = receiptPrefs,
        )

        val kitchenTicketData = KitchenTicketData(
            number = "$localOrderLabel (OFFLINE)",
            storeName = storeName,
            createdAt = now,
            typeLabel = typeLabel,
            items = cart.map { item ->
                KitchenTicketItem(
                    label = itemLabel(item),
                    qty = item.quantity,
                    addOns = item.addOns.map { it.name },
                    notes = item.notes,
                )
            },
            prefs = kitchenTicketPrefs,
        )

        return ReceiptBuilder.buildCustomerReceipt(receiptData) to ReceiptBuilder.buildKitchenTicket(kitchenTicketData)
    }
}
