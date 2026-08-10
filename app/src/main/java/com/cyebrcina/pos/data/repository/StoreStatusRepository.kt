package com.cyebrcina.pos.data.repository

import kotlinx.coroutines.flow.StateFlow

/** Whether Fire Hut is currently accepting new orders — see openapi.yaml's store-status paths. */
interface StoreStatusRepository {
    val acceptingOrders: StateFlow<Boolean?>

    suspend fun refresh(): Result<Unit>
    suspend fun setAcceptingOrders(accepting: Boolean): Result<Unit>
}
