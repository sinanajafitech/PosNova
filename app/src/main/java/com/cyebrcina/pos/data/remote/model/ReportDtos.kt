package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class PaymentMethodBreakdown(
    val provider: String,
    val orders: Int,
    val amount: Double,
)

@Serializable
data class TaxRateLine(
    val name: String,
    val region: String,
    val rate: Double,
    val appliesTo: String,
)

@Serializable
data class ZReport(
    val storeName: String,
    val date: String,
    val isToday: Boolean,
    val orderCount: Int,
    val grossSales: Double,
    val refundsTotal: Double,
    val refundsCount: Int,
    val netSales: Double,
    val avgOrderValue: Double,
    val paymentBreakdown: List<PaymentMethodBreakdown> = emptyList(),
    val vatAmount: Double,
    val netOfVat: Double,
    val primaryVatRate: TaxRateLine? = null,
)
