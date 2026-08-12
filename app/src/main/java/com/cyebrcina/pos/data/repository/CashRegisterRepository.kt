package com.cyebrcina.pos.data.repository

import com.cyebrcina.pos.data.remote.model.CashRegisterSessionDto

interface CashRegisterRepository {
    suspend fun current(): Result<CashRegisterSessionDto?>
    suspend fun open(openingFloat: Double, staffId: String?): Result<CashRegisterSessionDto?>
    suspend fun close(countedCash: Double, staffId: String?): Result<CashRegisterSessionDto?>
}
