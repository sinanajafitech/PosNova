package com.cyebrcina.pos.data.remote.model

import kotlinx.serialization.Serializable

@Serializable
data class OpenRegisterRequest(val openingFloat: Double, val staffId: String? = null)

@Serializable
data class CloseRegisterRequest(val countedCash: Double, val staffId: String? = null)

/** Real, live. Mirrors Admin's CashRegisterSession — variance is
 * countedCash - expectedCash, only populated once the session is closed. */
@Serializable
data class CashRegisterSessionDto(
    val id: String,
    val openingFloat: Double,
    val openedAt: String,
    val openedByStaffName: String? = null,
    val closedAt: String? = null,
    val countedCash: Double? = null,
    val expectedCash: Double? = null,
    val variance: Double? = null,
    val closedByStaffName: String? = null,
)

@Serializable
data class CashRegisterSessionResponse(val session: CashRegisterSessionDto? = null)
